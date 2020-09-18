(ns rest-api.classes.graphview.widget
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.db.main :refer [datomic-conn]]
   [pseudoace.schema-datomic :as schema-datomic]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :refer [pack-obj humanize-ident]]))


(defn get-data-forward [entity max-depth depth]
 (cond 
  (> depth max-depth)
  nil

  (instance? datomic.query.EntityMap entity)
  (let [data (some->> (keys entity)
                      (map (fn [k]
                       (when (not (= k :importer/temp))
                        (let [value  (get-data-forward (k entity) max-depth (+ depth 1))]
                         (when (and (not (nil? value))
                                    (not (= "..." value)))
                          {k value})))))
                      (remove nil?)
                      (into {}))]
    (if (not-empty data)
      data
      "..."))

  (instance? clojure.lang.PersistentHashSet entity)
  (let [data (if (< 100 (count entity))
              (str "Count: " (str (count entity)))
              (some->> entity
                       (map (fn [e]
                       (get-data-forward e max-depth (+ depth 1))))
                       (remove (fn [x] (= x "...")))
                       (remove nil?)))]
   (if (not-empty data)
    data
    "..."))

  (or
   (instance? java.lang.String entity)
   (instance? clojure.lang.Keyword entity))
  entity

  (or
   (instance? java.util.Date entity)
   (instance? java.lang.Double entity)
   (instance? java.lang.Long entity)
   (instance? java.lang.Boolean entity))
  (str entity)

  :else
  (str (type entity))))

(defn dataview [entity]
 {:data (let [max-depth 5
              depth 0]
          (get-data-forward entity max-depth depth))
  :description "Data directly related to the endity"})

 (defn schemaview [object]
  {:data (let [db (d/db datomic-conn)
               id-kw (first (filter #(= (name %) "id") (keys object)))
               role (namespace id-kw)
               
               schema (schema-datomic/raw-schema-from-db db)
               clj-schema (into {} 
                           (for [[k v] schema
                                 :let [name-parts (str/split (str k) #"\.")]
                                 :when (some #(= role %) name-parts)]
                             [(keyword k) v]))]
            clj-schema)
   :description "Schema related to the entity"})


(def widget
  {:name generic/name-field
   :dataview dataview
   :schemaview schemaview})
