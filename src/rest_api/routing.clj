(ns rest-api.routing
  (:require
   [clojure.string :as str]
   [compojure.api.sweet :as sweet]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]
   [ring.util.request :as req]
   [ring.util.http-response :as res]
   [schema.core :as schema]))

(defn- entity-not-found [schema-name id]
  (res/not-found
   {:message (format "Entity %s %s does not exist" schema-name id)}))

(defn- conform-uri [request]
  (str/replace-first (:uri request) "/" ""))

(defmulti conform-to-scheme
  (fn [scheme entity-handler entity request]
    (keyword scheme)))

(defmethod conform-to-scheme :field
  [scheme entity-handler entity request]
  (let [result (entity-handler entity)
        endpoint-kw (-> (req/path-info request)
                        (str/split #"/")
                        (last)
                        (keyword))]
    {endpoint-kw result}))

(defmethod conform-to-scheme :widget
  [scheme entity-handlers entity request]
  (let [result (reduce-kv (fn [m k handler]
                            (assoc m k (handler entity)))
                          (empty entity-handlers)
                          entity-handlers)]
    {:fields result}))

(defn make-request-handler [scheme entity-handler entity-class]
  (fn [request]
    (let [db (d/db datomic-conn)
          id (get-in request [:params :id])
          attr (keyword entity-class "id")
          lookup-ref [attr id]]
      (if-let [entity (d/entity db lookup-ref)]
        (->> (conform-to-scheme scheme entity-handler entity request)
             (merge {:class entity-class
                     :name id
                     :uri (conform-uri request)})
             (res/ok))
        (entity-not-found entity-class id)))))

(defprotocol RouteSpecification
  (-create-routes
    [route-spec]
    [route-spec opts]))

(defrecord RouteSpec [datatype widget field]
  RouteSpecification
  (-create-routes [this {:keys [publish-widget-fields?] :as opts}]
    (let [fields (:field this)
          field-defs (if publish-widget-fields?
                       (apply merge (cons fields (->> this :widget vals)))
                       fields)
          route-data (assoc this :field field-defs)
          entity-class (:entity-class route-data)
          entity-segment (str/replace entity-class #"-" "_")]
      (flatten
        (for [kw [:widget :field]
              :let [scheme (name kw)
                    ep-defs (kw route-data)]]
          (for [[ep-kw entity-handler] (sort-by key ep-defs)
                :let [ep-name (name ep-kw)]]
            (sweet/context (str "/" scheme "/" entity-segment) []
              :tags [(str entity-segment " " scheme "s")]
              (sweet/GET (str "/:id/" ep-name) []
                :path-params [id :- schema/Str]
                (make-request-handler kw entity-handler entity-class))))))))

  (-create-routes [this]
    (-create-routes this {:publish-widget-fields? true})))

(defmacro defroutes
  "Convenience constructor for defining `RouteSpec`s.
   Helps ensure this is defined with a constant name when used."
  [& rs]
  `(def ~(symbol "routes")
     (if-let [rs# (cond
                   (map? ~@rs) (map->RouteSpec ~@rs)
                   (vector? ~@rs) (apply ->RouteSpec ~@rs))]
       (-create-routes rs#)
       (throw (IllegalArgumentException.
               (format "Invalid route spec %s" ~@rs))))))
