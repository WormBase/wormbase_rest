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

(defn- id-attribs [db]
  (->> (d/q '[:find ?eid ?ident
              :where [?eid :db/ident ?ident]
                     [(name ?ident) ?ident-name]
                     [(ground "id") ?ident-name]]
          db)
       (into {})))

(defn- entity-lur-datoms [db id-attrs eid]
  "Return a lookup ref for `ent`, if it has a :class/id property.  Doesn't call `entity`, so can be used on history dbs."
  (last
   (for [[e a v t added?] (d/datoms db :eavt eid)
         :let [a (id-attrs a)]
         :when (and a added?)]
     [a v])))

(defn- touched-entities
  "Return lookup refs for entities that were modified in the transaction
   described by `datoms`."
  [db datoms]
  (->> (map :e datoms)
       (set)
       (sort)
       (map (partial entity-lur-datoms (d/history db) (id-attribs db)))
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
     :db-after (:db-after txr)
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
  (let [tx  (read-all rdr)
        txr @(d/transact con (->> (resolve-liberal-tx (d/db con) tx)
                                  (cons (vassoc
                                         (txn-meta)
                                         :db/doc (if (not (empty? note))
                                                   note)))))]
    {:success true
     :db-after (:db-after txr)
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
               [:p "Database updated.  Your patch has transaction ID "
                (let [t (d/basis-t (:db-after result))]
                  [:a {:href (str "txns?id=" t)} t])]
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

(defn txn-report [{db  :db
                   con :con
                   {:keys [from to count id t] :as params} :params}]
  (let [basis (d/basis-t db)
        txs   (cond
                (string? (or t id))
                (let [t (Long/parseLong (or t id))]
                  (d/tx-range (d/log con) t (inc t)))

                :default
                (d/tx-range (d/log con) (- basis 10) nil))]
    (page db
      [:h2 "Transaction log"]
      (for [{:keys [t data]} (reverse (seq txs))
            :let [txn (d/entity db (d/t->tx t))]]
        [:div.block
         [:h3 t " " (:db/txInstant txn) " " (:person/standard-name (:wormbase/curator txn)) " "
          [:a {:href (str "undo-txn?t=" t)} "[undo]"]]
         (if-let [doc (:db/doc txn)]
           [:p doc])
         [:p  (interpose ", "
                      (for [[cid id] (touched-entities db data)]
                        [:a {:href (str "/view/" (namespace cid) "/" id)
                             :target "_new"}
                         id]))]]))))


(defn make-reverse-txn [db datoms]
  (vec
   (for [[e a v _ add?] datoms
         :let  [a (d/entity db a)]
         :when (and (not= (d/part e) 3)
                    (not (:db/noHistory a)))]      ;; :db.part/tx is always partition 3???
     [(if add?
        :db/retract
        :db/add)
      e (:db/ident a) v])))

(defn- find-retracted-objs
  "Search a transaction for retractions of the form [:db/retract <end> :<class>/id _].
   Return a map of entity id -> lookup ref."
  [txn]
  (->> (for [[op e a v] txn
             :when (and (= op :db/retract)
                        (= (name a) "id"))]
         [e [a v]])
       (into {})))

(defn- find-entangled-txns [db entity gt-t]
  (->> (concat
        (d/datoms db :eavt entity)
        (d/datoms db :vaet entity))
       (map :tx)
       (filter #(> % gt-t))
       (into #{})))

(defn undo-txn [{{:keys [t]}  :params
                 db           :db
                 con          :con
                 method       :request-method}]
  (let [t      (Integer/parseInt t)
        datoms (-> (d/log con)
                   (d/tx-range t (inc t))
                   (first)
                   (:data))]
    (if datoms
      (let [reverse        (make-reverse-txn db datoms)
            retracted-objs (find-retracted-objs reverse)]
        (if (= method :post)
          (let [txr @(d/transact con (conj reverse
                                           (txn-meta)
                                           {:db/id (d/tempid :db.part/tx)
                                            :db/doc (str "Revert transaction " t)}))]
            (page db
              [:h3 "Transaction " t " was undone!"]))
          (let [test (d/with db reverse)]                
            (page db
              [:form {:method "POST"}
               (anti-forgery-field)
               [:input {:type "hidden"
                        :name "t"
                        :value t}]
               [:h3 "Transaction " t " can be undone."]
               (if-let [tangle (->> (keys retracted-objs)
                                    (mapcat #(find-entangled-txns db % (d/t->tx t)))
                                    (set)
                                    (sort)
                                    (seq))]
                 [:p "Warning: potentially-entangled transactions "
                  (interpose ", "
                   (for [tx tangle
                         :let [t (d/tx->t tx)]]
                      [:a {:href (str "txns?t=" t)} t]))])
               [:input {:type "submit"}]]
              (str reverse)))))
      (page db "No such txn"))))

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
   (GET "/gene/merge"     req (gene/merge-gene req))
   (POST "/gene/merge"    req (gene/merge-gene req))
   (GET "/ace-patch"      req (redirect "patch"))
   (GET "/patch"          req (patch req))
   (POST "/patch"         req (patch req))
   (GET "/txns"           req (txn-report req))
   (GET "/undo-txn"       req (undo-txn req))
   (POST "/undo-txn"      req (undo-txn req))
   )
  wrap-keyword-params))
