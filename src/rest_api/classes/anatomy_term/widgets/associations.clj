(ns rest-api.classes.anatomy-term.widgets.associations
  (:require
    [clojure.string :as str]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn transgenes [a]
  {:data nil ; can't find any
   :description "transgenes annotated with this anatomy_term"})

(defn gene-ontology [a]
  {:data (when-let [ghs (:anatomy-term/go-term a)]
           (for [gh ghs]
             (pace-utils/vmap
               :term
               (when-let [go-term (:anatomy-term.go-term/go-term gh)]
                 (pack-obj go-term))

               :ao_code
               (when-let [ao-code (:anatomy-term.go-term/ao-code gh)]
                 (pack-obj ao-code)))))
   :description "go_terms associated with this anatomy_term"})

(defn- anatomy-function [a bool]
  (let [afhs  (if (= bool true)
                (:anatomy-function.involved/_anatomy-term a)
                (:anatomy-function.not-involved/_anatomy-term a))]
    (for [afh afhs
          :let [af (if (= bool true)
                     (:anatomy-function/_involved afh)
                     (:anatomy-function/_not-involved afh))]]
      {:gene (when-let [gh (:anatomy-function/gene af)]
               (pack-obj (:anatomy-function.gene/gene gh)))
       :reference (when-let [reference (:anatomy-function/reference af)]
                    (pack-obj reference))
       :af_data (:anatomy-function/id af)
       :bp_inv (when-let [hs (:anatomy-function/involved af)]
                 (for [h hs]
                   {:text (:anatomy-term.term/text
                            (:anatomy-term/term
                              (:anatomy-function.involved/anatomy-term h)))
                    :evidence (obj/get-evidence h)}))
       :bp_not_inv (when-let [hs (:anatomy-function/not-involved af)]
                     (for [h hs]
                       {:text (:anatomy-term.term/text
                                (:anatomy-term/term
                                  (:anatomy-function.not-involved/anatomy-term h)))
                        :evidence (obj/get-evidence h)}))

       :phenotype (when-let [ph (:anatomy-function/phenotype af)]
                    (let [phenotype (:anatomy-function.phenotype/phenotype ph)]
                      (if-let [evidence (obj/get-evidence ph)]
                        {:text (pack-obj phenotype)
                         :evidence evidence}
                        (pack-obj phenotype))))
       :assay (when-let [hs (:anatomy-function/assay af)]
                (for [h hs]
                  {:text (:ao-code/id (:anatomy-function.assay/ao-code h))
                   :evidence (when-let [genotypes (:condition/genotype
                                                    (:anatomy-function.assay/condition h))]
                               {:genotype (str/join "<br /> " genotypes)})}))})))

(defn anatomy-functions [a]
  {:data (anatomy-function a true)
   :description "anatomy_functions associatated with this anatomy_term"})

(defn anatomy-function-nots [a]
  {:data (anatomy-function a false)
   :description "anatomy_functions associatated with this anatomy_term"})

(defn expression-clusters [a]
  {:data (when-let [hs (:expression-cluster.anatomy-term/_anatomy-term a)]
           (for [h hs
                 :let [ec (:expression-cluster/_anatomy-term h)]]
             {:description (first (:expression-cluster/description ec))
              :expression_cluster (pack-obj ec)}))
   :description "expression cluster data"})

(defn expression-patterns [a]
  {:data (when-let [hs (:expr-pattern.anatomy-term/_anatomy-term a)]
           (for [h hs
                 :let [ep (:expr-pattern/_anatomy-term h)]]
             {:description (when-let [patterns (:expr-pattern/pattern ep)]
                              (str/join "<br /> " patterns))
              :expression_pattern (pack-obj ep)
              :certainty nil
              :reference (when-let [hs (:expr-pattern/reference ep)]
                           (let [paper (:expr-pattern.reference/paper (first hs))]
                             (:paper/id paper)))
              :gene (when-let [g (:expr-pattern/gene ep)]
                      (pack-obj (:expr-pattern.gene/gene (first g))))
              :author (when-let [a (:expr-pattern/author ep)]
                        (map pack-obj a))}))
   :description (str "expression patterns associated with the Anatomy_term: " (:anatomy-term/id a))})

(def widget
  {:name generic/name-field
   :transgenes transgenes
   :gene_ontology gene-ontology
   :anatomy_function_nots anatomy-function-nots
   :expression_clusters expression-clusters
   :expression_patterns expression-patterns
   :anatomy_functions anatomy-functions})
