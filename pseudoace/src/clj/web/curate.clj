(ns web.curate
  (:use ring.middleware.keyword-params
        web.curate.common
        acetyl.parser
        pseudoace.utils)
  (:require [compojure.core :refer (routes GET POST context wrap-routes)]
            [web.curate.gene :as gene]
            [pseudoace.import :refer [importer]]
            [pseudoace.ts-import :as i]
            [datomic.api :as d]
            [web.anti-forgery :refer [anti-forgery-field]]))

(defn- do-ace-patch [con patch note]
  (let [imp  (importer con)
        db   (d/db con)
        objs (->> (java.io.StringReader. patch)
                  (ace-reader)
                  (ace-seq))
        logs (mapcat val (i/patches->log imp db objs))]
    @(d/transact con (-> (i/fixup-datoms db logs)
                          (conj (vassoc
                                 (txn-meta)
                                 :db/doc  (if (not (empty? note))
                                            note)))))
    {:success true}))
       

(defn ace-patch [{db  :db
                  con :con
                  {:keys [patch note] :as params} :params}]
  (let [result (if (and patch (not (empty? patch)))
                 (do-ace-patch con patch note))]
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
