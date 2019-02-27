(ns rest-api.intermine
  (:require
    [clojure.string :as str]
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :as sweet]
    [datomic.api :as d]))

(defn my-handler  [request]
    (ok  {:data  ["Hello I am gene list"]}))

(def routes
  (sweet/GET "/intermine/gene"  [] :tags  ["intermine"] my-handler))
