(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d]
            [clojure.string :as str]))


(defn field-adaptor [field-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (field-fn (d/entity db [(keyword wbid-field) id])))))

(defn widget-adaptor [widget-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (widget-fn (d/entity db [(keyword wbid-field) id])))))

(def whitelist (atom {}))

(defn register-endpoint [scope schema endpoint-fn endpoint-label]
  (let [endpoint-key (-> (str/join "." [scope schema endpoint-label])
                         (str/replace #"-" "_"))]
    (swap! whitelist assoc endpoint-key endpoint-fn)))

(defn rest-widget-fn [field-map]
  (fn [binding]
    (reduce (fn [result-map [key field-fn]]
              (assoc result-map key (field-fn binding)))
            {}
            field-map)))

(defn register-widget [widget-name field-map]
  (let [schema-name (-> (str *ns*)
                        (str/split #"\.")
                        (last))]
    (do (register-endpoint "widget" schema-name (rest-widget-fn field-map) widget-name)
        (doseq [[key field-fn] field-map]
          (register-endpoint "field" schema-name field-fn (name key)))
        (println (keys @whitelist)))))

(defmacro def-rest-widget
  [name body]
  `(register-widget (str (quote ~name)) ~body))


;; (defmacro def-rest-widget
;;   "def-rest-widget is synonymous to defn"
;;   [name [binding] & body]
;;   `(defn ~name [~binding]
;;      (do ~@body)))
