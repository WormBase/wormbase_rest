(ns web.query
  (:use pseudoace.utils)
  (:require
   [clojure.edn :as edn]
   [datomic.api :as d :refer (q db)]))

(def ^:private reader-map
  {'db/id (fn [[part n]] (d/tempid part n))})

(defn- parse-edn [string-or-data]
  (if (string? string-or-data)
    (edn/read-string {:readers reader-map} string-or-data)
    string-or-data))

(defn post-query-restful [con {:keys [query args limit offset] :as params}]
  (let [query  (or (params :query) (params "query"))
        args   (or (params :args) (params "args"))
        limit  (parse-int (or (params :limit) (params "limit")))
        offset (parse-int (or (params :offset) (params "offset")))
        args (->> (parse-edn args)
                  (mapv
                   (fn [arg]
                     (if-let [a (:db/alias arg)]
                       (d/db con)
                       arg))))
        query (parse-edn query)
        results (apply d/q query args)
        results (if offset
                  (drop offset results)
                  results)
        results (if limit
                  (take limit results)
                  results)]
    {:status 200
     :body (pr-str results)}))
     
    
    
               
