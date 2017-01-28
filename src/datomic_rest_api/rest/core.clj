(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [cheshire.core :as json]
            [compojure.api.sweet :as sweet :refer (GET)]
            [ring.util.response :as ring]))

(defn endpoint-adaptor [endpoint-fn]
  (fn [db schema-name id]
    (let [attr (keyword schema-name "id")
          lookup-ref [attr id]]
      (if-let [wb-entity (d/entity db lookup-ref)]
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
      (ring/response)
      (ring/content-type "application/json")))

(defn- entity-not-found [schema-name id]
  (-> {:message (format "Entity %s %s does not exist" schema-name id)}
      (json-response)
      (ring/status 404)))

(defn widget-setting
  [widget-name field-map]
  {:is-widget true
   :name widget-name
   :fields field-map})

(defn field-setting [field-name field-fn]
  {:is-widget false
   :name field-name
   :field field-fn})

(defn- field-route [db schema-name field-name field-fn]
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

(defn- widget-route [db schema-name widget-name fields-map]
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

;; Create a route factory based on the supplied route settings.
;; The new factory takes a db connection and returns a route
;; consistent with the route settings.
;; (Provider is a factory of factories.)
(defn route-provider [schema-name & routes-settings]
  (fn [db]
    (->> (map (fn [route-setting]
                (if (:is-widget route-setting)
                  (widget-route db schema-name
                                (:name route-setting)
                                (:fields route-setting))
                  (field-route db schema-name
                                (:name route-setting)
                                (:field route-setting))))
              routes-settings)
         (apply sweet/routes))))

;; syntactic sugar for creating routes
(defmacro def-rest-routes [route-symbol schema-name & route-settings]
  `(defn ~route-symbol [db#]
     ;; create routes by suppling db to a factory
     ((route-provider ~schema-name ~@route-settings) db#)))
