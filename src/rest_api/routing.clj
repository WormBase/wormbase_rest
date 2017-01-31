(ns rest-api.routing
  (:require 
   [cheshire.core :as json]
   [clojure.string :as str]
   [compojure.api.sweet :as sweet]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]
   [ring.util.response :as ring]
   [schema.core :as schema]))

(defn- json-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring/response)
      (ring/content-type "application/json")))

(defn- entity-not-found [schema-name id]
  (-> {:message (format "Entity %s %s does not exist" schema-name id)}
      (json-response)
      (ring/status 404)))

(defn- wrap-entity-handler [entity-handler]
  (fn [schema-name id]
    (let [db (d/db datomic-conn)
          attr (keyword schema-name "id")
          lookup-ref [attr id]]
      (if-let [wb-entity (d/entity db lookup-ref)]
        (entity-handler wb-entity)))))

(defn- wrap-entity-handlers [route-spec]
  (fn [binding]
    (reduce-kv (fn [m k handler]
                 (assoc m k (handler binding)))
               (empty route-spec)
               route-spec)))

(defmulti create-routes
  (fn [a _ _]
    (map? a)))

(defmethod create-routes
  true
  [route-spec endpoint-name datatype]
  (let [handler (wrap-entity-handlers route-spec)]
    (create-routes handler endpoint-name datatype)))

(defmethod create-routes
  false
  [entity-handler endpoint-name datatype]
  (let [ep-name (if (keyword? endpoint-name)
                  (name endpoint-name)
                  endpoint-name)
        uri-parts [nil datatype ":id" ep-name]
        uri (str/join "/" uri-parts)
        handler (wrap-entity-handler entity-handler)]
    (sweet/GET uri [id]
      :path-params [id :- schema/Str]
      (if-let [result (handler datatype id)]
        (-> {:name id
             :class datatype
             :url (str/replace uri #":id" id)}
            (assoc (keyword ep-name) result)
            (json-response))
        (entity-not-found datatype id)))))

(defprotocol RouteSpecification
  (create
    [route-spec]
    [route-spec opts]))

(defrecord RouteSpec [datatype widget field]
  RouteSpecification
  (create [this {:keys [publish-widget-fields?] :as opts}]
    (let [fields (:field this)
          field-defs (if publish-widget-fields?
                       (apply merge (cons fields (->> this :widget vals)))
                       fields)
          route-data (assoc this :field field-defs)
          wb-dt (:datatype route-data)]
      (flatten
       (for [kw [:widget :field]
             :let [scheme (name kw)
                   ep-defs (kw route-data)]]
         (for [[ep-name ep-map] (sort-by key ep-defs)]
           (sweet/context (str "/" scheme) []
             :tags [(str datatype " " scheme "s")]
             (create-routes ep-map ep-name wb-dt)))))))
  (create [this]
    (create this {:publish-widget-fields? true})))

(defn routes-from-spec
  "Creates compojure-api routes from the given route spec `rs`."
  [xs]
  (let [rs (if (map? xs)
             (map->RouteSpec xs)
             xs)]
    (create rs)))

(defmacro defroutes
  "Convenience constructor for defining `RouteSpec`s.
   Helps ensure this is defined with a constant name when used."
  [& rs]
  `(def ~(symbol "routes")
     (routes-from-spec (map->RouteSpec ~@rs))))
