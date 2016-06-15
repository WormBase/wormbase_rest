(ns datomic_rest_api.rest.gene-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [environ.core :refer (env)]
            [clojure.data.json :as json]
            [clojure.string :as string]))


;; general test

;;(facts "The base url is simply for testing purposes"
;;  (let [response (http/get base_url)]
;;    (fact "Check status"
;;           (:status response) => 200)
;;    (fact "check response body"
;;           (:body response) => "hello")))
;;
;;(facts "GENE: Checking human_diseases" 
;;   (let [gene_id "WBGene00000900"
;;         url  (reduce str [ base_url "rest/widget/gene/" gene_id "/human_diseases"])
;;         response (http/get url {:accept :json})]
;;     (print response)
;;     (fact "check response status"
;;           (:status response) => 200)))
;;     (fact "check body - expermental_model - nill returned when there is no data"
;;           (let [body  (json/read-str (:body response) :key-fn keyword)]
;;             (print body)))))
;;             (:experimenta_model (:data (:human_diseases (:fields body)))) => nil))))
