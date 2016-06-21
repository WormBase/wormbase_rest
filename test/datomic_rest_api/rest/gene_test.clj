(ns datomic_rest_api.rest.gene-test
  (:use midje.sweet)
 ;; (:use [datomic-rest-api.rest.gene])
  (:require [clojure.test :refer :all]
            [environ.core :refer (env)]
            [clojure.data.json :as json]
            [clojure.string :as string]))

;;(def uri (env :trace-db))
;;(def con (datomic.api/connect uri))

;;(def base_url "localhost:8008")

(defn first-element [sequence default]
  (if (nil? sequence)
    default
    (first sequence)))


(facts "about `first-element`"
  (fact "it normally returns the first element"
    (first-element [1 2 3] :default) => 1
    (first-element '(1 2 3) :default) => 1))



;;(facts "The base url is simply for testing purposes"
;;  (fact "Url test"
;;  (let [base_url "localhost:8080"
;;        url base_url]
;;      (fact "Check base url"
;;           (expect uri => "http://localhost:8130")))))
