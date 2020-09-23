(ns rest-api.classes.graphview.widget
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.db.main :refer [datomic-conn]]
   [pseudoace.schema-datomic :as schema-datomic]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :refer [pack-obj humanize-ident]]))


(defn has-page? [role]
 (some #(= role %)
            ["analysis"
             "anatomy-term"
             "antibody"
             "cds"
             "clone"
             "construct"
             "do-term"
             "expression-cluster"
             "expr-pattern"
             "expr-profile"
             "feature"
             "gene-class"
             "gene"
             "gene-cluster"
             "go-term"
             "homology-group"
             "interaction"
             "laboratory"
             "life-stage"
             "microarray-results"
             "molecule"
             "motif"
             "operon"
             "paper"
             "pcr-product" "oligo"
             "person"
             "phenotype"
             "picture"
             "position-matrix"
             "protein"
             "pseudogene"
             "rearrangement"
             "rnai"
             "sequence"
             "strain"
             "structure-data"
             "transcript"
             "transgene"
             "transposon"
             "transposon-family"
             "variation"
             "wbprocess"]))


(defn get-data-forward [entity max-depth depth]
 (cond 
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

  (> depth max-depth)
  nil

  (instance? datomic.query.EntityMap entity)
  (let [data (let [id-kw (first (filter #(= (name %) "id") (keys entity)))
                   role (when (not (nil? id-kw)) (namespace id-kw))]
              (if (and (not (= depth 0))
                       (has-page? role))
               (pack-obj entity)
               (some->> (keys entity)
                        (map (fn [k]
                              (when (not (= k :importer/temp))
                               (let [value  (get-data-forward (k entity) max-depth (+ depth 1))]
                                (when (and (not (nil? value))
                                           (not (= "..." value)))
                                 {k value})))))
                        (remove nil?)
                        (into {}))))]
    (if (not-empty data)
      data
      "..."))

  (instance? clojure.lang.PersistentHashSet entity)
  (let [data (if (< 30 (count entity))
              (str "Count: " (str (count entity)))
              (some->> entity
                       (map (fn [e]
                       (get-data-forward e max-depth (+ depth 1))))
                       (remove (fn [x] (= x "...")))
                       (remove nil?)))]
   (if (not-empty data)
    data
    "..."))

  :else
  (str (type entity))))

(defn dataview [entity]
 {:data (let [max-depth 5
              depth 0
              db (d/db datomic-conn)
              id-kw (first (filter #(= (name %) "id") (keys entity)))
              role (namespace id-kw)
              schema (schema-datomic/raw-schema-from-db db)
              backwards-pointers (remove nil?
                                  (flatten
                                   (for [[k v] schema
                                   :when (not (= role k))]
                                    (some->> v
                                             (map :db/ident)
                                             (map (fn [id]
                                                 (when (= (name id) role)
                                                   (keyword (namespace id) (str "_" (name id))))))))))]
         {:forwards
          (get-data-forward entity max-depth depth)

          :backwards
          (some->> backwards-pointers
                   (map (fn [p]
                         (when-let [entities (p entity)]
                          {p
                           (some->> entities
                                    (map (fn [d]
                                          (let [id-kw (first (filter #(= (name %) "id") (keys d)))
                                                role (when (not (nil? id-kw)) (namespace id-kw))]
                                           (if (has-page? role)
                                            (pack-obj d)
                                            (let [name-parts (str/split (namespace p) #"\.")
                                                  fields (if (= 2 (count name-parts))
                                                          (conj (keys d)
                                                                (keyword (str (first name-parts) "/_" (second name-parts))))
                                                          (keys d))]
                                             (some->> fields
                                                      (filter (fn [k]
                                                               (and (not (= k (keyword (namespace p) (str/replace (str (name p)) #"_" ""))))
                                                                    (not (= k :importer/temp)))))
                                                      (map (fn [k]
                                                            {k (get-data-forward (k d) 1 0)}))
                                                      (into {}))))))))})))
                   (remove nil?))})
  :description "Data directly related to the entity"})

 (defn schemaview [entity]
  {:data (let [db (d/db datomic-conn)
               id-kw (first (filter #(= (name %) "id") (keys entity)))
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
