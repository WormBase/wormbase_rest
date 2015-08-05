(ns web.curate
  (:use ring.middleware.keyword-params
        web.curate.common
        acetyl.parser
        pseudoace.utils)
  (:require [wb.liberal-txns :refer (resolve-liberal-tx)]
            [compojure.core :refer (routes GET POST context wrap-routes)]
            [web.curate.gene :as gene]
            [pseudoace.import :refer [importer]]
            [pseudoace.ts-import :as i]
            [datomic.api :as d]
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

(defn- do-ace-patch [con patch note]
  (let [imp  (importer con)
        db   (d/db con)
        objs (->> (java.io.StringReader. patch)
                  (ace-reader)
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
       

(defn ace-patch [{db  :db
                  con :con
                  {:keys [patch note] :as params} :params}]
  (let [result (if (and patch (not (empty? patch)))
                 (do-ace-patch con patch note))]
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
       [:form {:method "POST"}
        (anti-forgery-field)
        [:h3 "Ace format submission"]
        [:textarea {:name "patch"
                    :cols 80
                    :rows 30}
         (if (and (not (:success result)) patch)
           patch)]
        [:br]
        [:label "Note:"
         [:input {:type "text"
                  :name "note"
                  :value (or note "")}]]
        [:input {:type "submit"}]]])))

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
   (GET "/ace-patch"      req (ace-patch req))
   (POST "/ace-patch"     req (ace-patch req))
   )
  wrap-keyword-params))
