(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [cheshire.core :as json]
            [compojure.api.sweet :as sweet :refer (GET)]
            [ring.util.response]))


(defn endpoint-adaptor [endpoint-fn]
  (fn [db schema-name id]
    (let [wbid (str schema-name "/id")]
      (if-let [wb-entity (d/entity db [(keyword wbid) id])]
        (endpoint-fn wb-entity)))))

(defn rest-widget-fn [fields-map]
  (fn [binding]
    (reduce (fn [result-map [key field-fn]]
              (assoc result-map key (field-fn binding)))
            {}
            fields-map)))

(defn- json-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring.util.response/response)
      (ring.util.response/content-type "application/json")))

(defn- entity-not-exist [schema-name id]
  (-> {:message (format "Entity %s %s does not exist" schema-name id)}
      (json-response)
      (ring.util.response/status 404)))

(defn field-route [db schema-name field-name field-fn]
  (let [field-url (str/join "/" ["/rest" "field" schema-name ":id" field-name])
        adapted-field-fn (endpoint-adaptor field-fn)]
    (GET field-url [id]
         :tags [(str schema-name " fields")]
         (if-let [result (adapted-field-fn db schema-name id)]
           (-> {:name id
                :class schema-name
                :url (str/replace field-url #":id" id)}
               (assoc (keyword field-name) result)
               (json-response))
           (entity-not-exist schema-name id)))))

(defn widget-route [db schema-name widget-name fields-map]
  (let [widget-url (str/join "/" ["/rest" "widget" schema-name ":id" widget-name])
        adapted-widget-fn (endpoint-adaptor (rest-widget-fn fields-map))]
    (->> (cons (GET widget-url [id]
                    :tags [(str schema-name " widgets")]
                    (if-let [result (adapted-widget-fn db schema-name id)]
                      (-> {:name id
                           :class schema-name
                           :url (str/replace widget-url #":id" id)}
                          (assoc (keyword widget-name) result)
                          (json-response))
                      (entity-not-exist schema-name id)))
               (map (fn [[field-name field-fn]]
                      (field-route db schema-name (name field-name) field-fn))
                    fields-map))
         (apply sweet/routes))))
