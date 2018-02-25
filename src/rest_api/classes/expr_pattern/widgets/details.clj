(ns rest-api.classes.expr-pattern.widgets.details
  (:require
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.date :as dates]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn experimental-details [e]
  {:data {:types (into
                   []
                   (pace-utils/vmap
                     "Antibody"
                     (when (contains? e :expr-pattern/antibody) "")

                     "Cis regulatory element"
                     (when (contains? e :expr-pattern/cis-regulatory-element) "")

                     "EPIC"
                     (when (contains? e :expr-pattern/epic) "")

                     "Genome editing"
                     (when (contains? e :expr-pattern/genome-editing) "")

                     "In-situ"
                     (when (contains? e :expr-pattern/in-situ) "")

                     "Localizome"
                     (when (contains? e :expr-pattern/localizome) "")

                     "Microarray"
                     (when-let [analyses (:expr-pattern/microarray e)]
                       (sort-by :label (map pack-obj analyses)))

                     "Northern"
                     (when (contains? e :expr-pattern/northern) "")

                     "Reporter gene"
                     (when (contains? e :expr-pattern/reporter-gene) "")

                     "RNAseq"
                     (when-let [analyses (:expr-pattern/rnaseq e)]
                       (sort-by :label (map pack-obj analyses)))

                     "RT-PCR"
                     (when (contains? e :expr-pattern/rt-pcr) "")

                     "Western"
                     (when (contains? e :expr-pattern/western) "")

                     "Tiling array"
                     (when-let [analyses (:expr-pattern/tiling-array e)]
                       (sort-by :label (map pack-obj analyses)))))
          :variation (->> (:variation.expr-pattern/_expr-pattern e)
                          (map :variation/_expr-pattern)
                          (map pack-obj)
                          (seq))
          :antibody_info (when-let [a (:expr-pattern/antibody-info e)]
                           (map pack-obj a))
          :author (when-let [a (:expr-pattern/author e)]
                    (map pack-obj a))
          :date (when-let [d (:expr-pattern/date e)]
                  (dates/format-date4 d))
          :transgene (some->> (:expr-pattern/transgene e)
                       (map (fn [t] [(pack-obj t)
                                     (:transgene.summary/text (:transgene/summary t))]))
                       (into []))
          :construct (some->> (:expr-pattern/construct e)
                       (map (fn [c] [(pack-obj c)
                                     (:construct.summary/text (:construct/summary c))]))
                       (into []))

          :strain (when-let [s (:expr-pattern/strain e)]
                    [(pack-obj s)])}
   :description "Experimental details of the expression pattern"})

(defn anatomy-ontology [e]
  {:data (some->> (:expr-pattern/anatomy-term e)
                  (map :expr-pattern.anatomy-term/anatomy-term)
                  (sort-by :anatomy-term/id)
                  (map (fn [term]
                         {:anatomy_term (pack-obj term)
                          :definition (:anatomy-term.definition/text
                                        (:anatomy-term/definition term))})))
   :description "Anatomy ontology terms associated with this expression pattern"})

(defn gene-ontology [e]
  {:data (some->> (:expr-pattern/go-term e)
                  (map :expr-pattern.go-term/go-term)
                  (sort-by :go-term/id)
                  (map (fn [term]
                         {:go_term (pack-obj term)
                          :definition (first (:go-term/definition term))})))
   :description "gene ontology terms associated with this expression pattern"})

(defn expressed-by [e]
  {:data (pace-utils/vmap
           :gene
           (some->> (:expr-pattern/gene e)
                  (map :expr-pattern.gene/gene)
                  (map pack-obj)
                  (sort-by :label)
                  (map (fn [o]
                         {(:id o) o}))
                  (into {}))

           :sequence
           (some->> (:expr-pattern/sequence e)
                    (map pack-obj)
                    (sort-by :label)
                    (map pack-obj)
                    (map (fn [o]
                           {(:id o) o}))
                    (into {}))

           :protein
           (some->> (:expr-pattern/protein e)
                    (map pack-obj)
                    (sort-by :label)
                    (map (fn [o]
                           {(:id o) o}))
                    (into {}))

           :clone
           (some->> (:expr-pattern/clone e)
                    (map pack-obj)
                    (sort-by :label)
                    (map (fn [o]
                           {(:id o) o}))
                    (into {})))
   :description "Items that exhibit this expression pattern"})

(defn expressed-in [e]
  {:data (not-empty
           (pace-utils/vmap
             "life stage"
             (some->> (:expr-pattern/life-stage e)
                      (map :expr-pattern.life-stage/life-stage)
                      (map pack-obj)
                      (sort-by :label))))
   :description "Where the expression has been noted"})

(defn sequence-feature [e]
  {:data (some->> (:expr-pattern/associated-feature e)
                  (map :expr-pattern.associated-feature/feature)
                  (map (fn [f]
                         (let [packed-obj (pack-obj f)]
                           (if-let [term (:so-term/name
                                           (first
                                             (:feature/so-term f)))]
                             (conj
                               packed-obj
                               {:label (str (:label packed-obj) " - " term)})
                             packed-obj)))))
   :description (str "The sequence feature associated with the expression profile " (:expr-pattern/id e))})

(def widget
  {:name generic/name-field
   :experimental_details experimental-details
   :anatomy_ontology anatomy-ontology
   :gene_ontology gene-ontology
   :expressed_by expressed-by
   :sequence_feature sequence-feature
   :expressed_in expressed-in})
