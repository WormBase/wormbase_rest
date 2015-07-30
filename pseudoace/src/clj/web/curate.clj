(ns web.curate
  (:use ring.middleware.keyword-params
        web.curate.common
        acetyl.parser)
  (:require [compojure.core :refer (routes GET POST context wrap-routes)]
            [web.curate.gene :as gene]
            [pseudoace.import :refer [importer]]
            [pseudoace.ts-import :as i]
            [datomic.api :as d]
            [web.anti-forgery :refer [anti-forgery-field]]))

(defn- do-ace-patch [con patch]
  (let [imp  (importer con)
        objs (->> (java.io.StringReader. patch)
                  (ace-reader)
                  (ace-seq))
        logs (mapcat val (i/objs->log imp objs))]
    @(d/transact con (conj (i/fixup-datoms (d/db con) logs) (txn-meta)))
    {:success true}))
       

(defn ace-patch [{db  :db
                  con :con
                  {:keys [patch] :as params} :params}]
  (let [result (if patch
                 (do-ace-patch con patch))]
    (page db
      (if (:success result)
        [:div.block
         [:p "Database updated!"]])
      [:div.block
       [:form {:method "POST"}
        (anti-forgery-field)
        [:h3 "Ace format submission"]
        [:textarea {:name "patch"
                    :cols 80
                    :rows 30}
         (if (and (not (:success result)) patch)
           patch)]
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
