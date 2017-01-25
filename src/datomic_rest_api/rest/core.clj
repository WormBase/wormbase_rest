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



;; helpers for managing whitelisted endpoints

(defonce ^{:private true} whitelist (atom {}))

(defn- endpoint-key [scope schema-name endpoint-name]
  (-> (str/join "." [scope schema-name endpoint-name])
      (str/replace #"-" "_")))

(defn resolve-endpoint [scope schema-name endpoint-name]
  (-> (endpoint-key scope schema-name endpoint-name)
      (@whitelist)))

(defn register-endpoint [scope endpoint-fn endpoint-name]
  (let [schema-name (-> (str *ns*)
                        (str/split #"\.")
                        (last))]
    (swap! whitelist
           assoc
           (endpoint-key scope schema-name endpoint-name)
           endpoint-fn)))

(defn- rest-widget-fn [field-map]
  (fn [binding]
    (reduce (fn [result-map [key field-fn]]
              (assoc result-map key (field-fn binding)))
            {}
            field-map)))

;; registered widgets and fields will be whitelisted

(defn register-widget [widget-name field-map]
  (do (register-endpoint "widget" (rest-widget-fn field-map) widget-name)
      (doseq [[key field-fn] field-map]
        (register-endpoint "field" field-fn (name key)))))

(defn register-independent-field [field-name field-fn]
  (register-endpoint "field" field-fn field-name))

(defmacro def-rest-widget
  [name body]
  `(register-widget (str (quote ~name)) ~body))


(defn endpoint-urls [scope]
  (->> (keys @whitelist)
       (map #(str/split % #"\."))
       (map #(zipmap [:scope :schema :name] %))
       (filter #(= scope (:scope %)))
       (map #(str/join "/" ["rest"
                            (:scope %)
                            (:schema %)
                            :id
                            (:name %)]))))

;; helpers for managing whitelisted endpoints
