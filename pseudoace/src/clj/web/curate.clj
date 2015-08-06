(ns web.curate
  (:use ring.middleware.keyword-params
        web.curate.common
        acetyl.parser
        pseudoace.utils)
  (:require [ring.util.response :refer (redirect)]
            [wb.liberal-txns :refer (resolve-liberal-tx)]
            [compojure.core :refer (routes GET POST context wrap-routes)]
            [web.curate.gene :as gene]
            [pseudoace.import :refer [importer]]
            [pseudoace.ts-import :as i]
            [datomic.api :as d]
            [clojure.edn :as edn]
            [clojure.java.io :refer [reader]]
            [web.anti-forgery :refer [anti-forgery-field]]))

(defn- entity-lur
  "Return a lookup ref for `ent`, if it has a :class/id property."
  [ent]
  (if-let [cid (first (filter #(= (name %) "id") (keys ent)))]
    [cid (get ent cid)]))

(defn- touched-entities
  "Return lookup refs for entities that were modified in the transaction
   described by `datoms`."
  [db datoms]
  (->> (map :e datoms)
       (set)
       (map (partial d/entity db))
       (map entity-lur)
       (filter identity)))

(defn- do-ace-patch [con rdr note]
  (let [imp  (importer con)
        db   (d/db con)
        objs (->> (ace-reader rdr)
                  (ace-seq))
        logs (mapcat val (i/patches->log imp db objs))
        txr  @(d/transact con (->> (i/fixup-datoms db logs)
                                   (resolve-liberal-tx db)
                                   (cons (vassoc
                                          (txn-meta)
                                          :db/doc  (if (not (empty? note))
                                                     note)))))]
    {:success true
     :entities (touched-entities (:db-after txr) (:tx-data txr))}))
       

(def ^:private reader-map
  {'db/id (fn [[part n]] (d/tempid part n))})

(def ^:private eof-marker (java.lang.Object.))  ;; a unique object which can't be
                                                ;; equal to anything produced by
                                                ;; reading EDN.

(defn- read-all
  "Return a sequence of EDN data by repeatedly reading from the reader `rdr`."
  [rdr]
  (let [r (java.io.PushbackReader. rdr)]
    (loop [data []]
      (let [datum (edn/read {:readers reader-map
                             :eof eof-marker}
                            r)]
        (if (= datum eof-marker)
          data
          (recur (conj data datum)))))))

(defn- do-edn-patch [con rdr note]
  (let [tx  (read-all patch)
        txr @(d/transact con (->> (resolve-liberal-tx (d/db con) tx)
                                  (cons (vassoc
                                         (txn-meta)
                                         :db/doc (if (not (empty? note))
                                                   note)))))]
    {:success true
     :entities (touched-entities (:db-after txr) (:tx-data txr))}))

(defn patch [{db  :db
              con :con
              {:keys [patch note format patchfile] :as params} :params}]
  (let [patch-rdr (cond
                    (and (:tempfile patchfile) (:size patchfile) (> (:size patchfile) 0))
                    (reader (:tempfile patchfile))

                    (and patch (not (empty? patch)))
                    (java.io.StringReader. patch))]
    (let [result (if patch-rdr
                   (case format
                     "ace"
                     (do-ace-patch con patch-rdr note)
                     
                     "edn"
                     (do-edn-patch con patch-rdr note)
                     
                     ;;default
                     (except "Bad format " format)))]
      (page db
            (if (:success result)
              [:div.block
               [:p "Database updated!"]
               [:p
           (interpose ", "
                      (for [[cid id] (:entities result)]
                        [:a {:href (str "/view/" (namespace cid) "/" id)
                             :target "_new"}
                id]))]])
            [:div.block
             [:form {:method "POST"
                     :enctype "multipart/form-data"}
              (anti-forgery-field)
              [:h3 "Database patch submission"]
              [:strong "Format:"]
              [:label "Ace"
               [:input {:type "radio"
                        :value "ace"
                        :name "format"
                        :checked (if (not= format "edn") "1")}]]
              [:label "Datomic"
               [:input {:type "radio"
                        :value "edn"
                        :name "format"
                        :checked (if (= format "edn") "1")}]]
              [:br]
              [:textarea {:name "patch"
                          :cols 80
                          :rows 30}
               (if (and (not (:success result)) patch)
                 patch)]
              [:br]
              [:input {:type "file"
                       :name "patchfile"
                       :accept ".ace,.edn"}]
              [:br]
              [:label "Note:"
               [:input {:type "text"
                        :name "note"
                        :value (or note "")}]]
              [:input {:type "submit"}]]]))))

(def curation-forms
 (wrap-routes
  (routes
   (GET "/gene/query"     req (gene/query-gene req))
   (GET "/gene/new"       req (gene/new-gene req))
   (POST "/gene/new"      req (gene/new-gene req))
   (GET "/gene/kill"      req (gene/kill-object "Gene" req))
   (POST "/gene/kill"     req (gene/kill-object "Gene" req))
   (GET "/gene/add-name"  req (gene/add-gene-name req))
   (POST "/gene/add-name" req (gene/add-gene-name req))
   (GET "/ace-patch"      req (redirect "patch"))
   (GET "/patch"          req (patch req))
   (POST "/patch"         req (patch req))
   )
  wrap-keyword-params))
