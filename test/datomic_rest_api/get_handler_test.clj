(ns datomic_rest_api.get-handler-test
;;  (:use midje.sweet)
  (:use [datomic-rest-api.get-handler])
  (:require [clojure.test :refer :all]
            [environ.core :refer (env)]
            [clojure.data.json :as json]
            [clojure.string :as string]))

;;(def port (env :trace-port))
;;(def base_url (reduce str ["http://localhost:" port "/"]))

;; general test
