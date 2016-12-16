(ns datomic-rest-api.rest.helpers.references
  (:use datomic-rest-api.rest.object)
  (:require [cheshire.core :as json]
            [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap vassoc]]))

(defn- format-paper [paper]
  {:ptype     (if-let [t (:paper.type/type (:paper/type paper))]
                (name t)
                "unknown")
   :name      (pack-obj "paper" paper)
   :title     [(:paper/title paper)]
   :abstract  (map :longtext/text (:paper/abstract paper)) ;; Does anything actually have multiple abstracts?
   :author    (for [a (sort-by :ordered/index (:paper/author paper))]
                (if-let [p (:affiliation/person a)]
                  (pack-obj (first p))
                  {:class "person"
                   :label (:author/id (:paper.author/author a))
                   :id    (:author/id (:paper.author/author a))}))
   :year      (:paper/publication-date paper)
   :journal   [(:paper/journal paper)]})

(defn get-references [class db id]
  (let [obj  (obj-get class db id)
        refs (get obj (keyword class "reference"))]
    (if refs
      {:status 200
       :content-type "application/json"
       :body (json/generate-string
              {:count (count refs)
               :page  1
               :query id
               :type "paper"
               :results (map format-paper (take 1000 refs))})})))   ;; Should do some pagination?
