(ns rest-api.classes.expr-pattern.widgets.details
  (:require
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn experimental-details [e]
  {:data {:types (pace-utils/vmap
                   "Antibody"
                   (:expr-pattern/antibody e)

                   "CIS Regulatory Element"
                   (when (contains? e :expr-pattern/cis-regulatory-element) "")

                   "EPIC"
                   (when (contains? e :expr-pattern/epic) "")

                   "Genome Editing"
                   (when (contains? e :expr-pattern/genome-editing) "")

                   "In-Situ"
                   (:expr-pattern/in-situ e)

                   "Localizome"
                   (when (contains? e :expr-pattern/localizome) "")

                   "Microarray"
                   (when-let [analyses (:expr-pattern/microarray e)]
                     (sort-by :label (map pack-obj analyses)))

                   "Northern"
                   (:expr-pattern/northern e)

                   "Reporter Gene"
                   (:expr-pattern/reporter-gene e)

                   "RNAseq"
                   (when-let [analyses (:expr-pattern/rnaseq e)]
                     (sort-by :label (map pack-obj analyses)))

                   "RT-PCR"
                   (when (contains? e :expr-pattern/rt-pcr) "")

                   "Western"
                   (:expr-pattern/western e)

                   "Tiling Array"
                   (when-let [analyses (:expr-pattern/tiling-array e)]
                     (sort-by :label (map pack-obj analyses))))
          :antibody_info (when-let [a (:expr-pattern/antibody-info e)]
                           (map pack-obj a))
          :transgene (when-let [t (:expr-pattern/transgene e)]
                       (map pack-obj t))
          :construct (when-let [c (:expr-pattern/construct e)]
                       (map pack-obj c))
          :strain (when-let [s (:expr-pattern/strain e)]
                    (pack-obj s))}
   :description "Experimental details of the expression pattern"})

(defn anatomy-ontology [e]
  {:data (some->> (:expr-pattern/anatomy-term e)
                  (map :expr-pattern.anatomy-term/anatomy-term)
                  (sort-by :anatomy-term/id)
                  (map (fn [term]
                         {:anatomy_term (pack-obj term)
                          :definition (:anatomy-term.definition/text
                                        (:anatomy-term/definition term))})))
   :description "anatomy ontology terms associated with this expression pattern"})

(defn gene-ontology [e]
  {:data (some->> (:expr-pattern/go-term e)
                  (map :expr-pattern.go-term/go-term)
                  (sort-by :go-term/id)
                  (map (fn [term]
                         {:go_term (pack-obj term)
                          :definition (first (:go-term/definition term))})))
   :description "gene ontology terms associated with this expression pattern"})

(defn expressed-by [e]
  {:data (some->> (:expr-pattern/gene e)
                  (map :expr-pattern.gene/gene)
                  (map pack-obj)
                  (sort-by :label)
                  (group-by :id))
   :description "Items that exhibit this expression pattern"})

(defn expressed-in [e]
  {:data (not-empty
           (pace-utils/vmap
           "life stage"
           (some->> (:expr-pattern/life-stage e)
                    (map :expr-pattern.life-stage/life-stage)
                    (map pack-obj)
                    (sort-by :id))))
   :description "where the expression has been noted"})

(def widget
  {:name generic/name-field
   :experimental_details experimental-details
   :anatomy_ontology anatomy-ontology
   :gene_ontology gene-ontology
   :expressed_by expressed-by
   :expressed_in expressed-in})
