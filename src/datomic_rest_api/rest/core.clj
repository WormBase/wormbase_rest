(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d :refer (db q touch entity)]
            [cheshire.core :as c :refer (generate-string)]
            [clojure.string :as str]))


(defn wrap-field [field-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (field-fn (d/entity db [(keyword wbid-field) id])))))

(defn wrap-widget [widget-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (widget-fn (d/entity db [(keyword wbid-field) id])))))

(defmacro def-rest-widget
  "def-rest-widget is synonymous to defn"
  [name [binding] & body]
  `(defn ~name [~binding]
     (do ~@body)))
