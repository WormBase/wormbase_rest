(ns rest-api.classes.feature.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-ontology-terms [f]
  {:data (when-let [sos (:feature/so-term f)]
           (for [so sos] (pack-obj so)))
   :description "sequence ontology terms describing the feature"})

(defn binds-gene-product [f]
  {:data (when-let [ghs (:feature/bound-by-product-of f)]
           (for [gh ghs
                 :let [gene (:feature.bound-by-product-of/gene gh)]]
             (pack-obj gene)))
   :description "gene products that bind to the feature"})

(defn defined-by [f]
  {:data (let [person (when-let [ps (:feature/defined-by-person f)]
                        (for [p ps]
                          {:object (pack-obj p)
                           :label "Person"}))
               paper (when-let [phs (:feature/defined-by-paper f)]
                       (for [ph phs]
                         {:object (pack-obj (:feature.defined-by-paper/paper ph))
                          :label "Paper"}))
               analysis (when-let [phs (:feature/defined-by-analysis f)]
                          (for [ph phs]
                            {:object (pack-obj (:feature.defined-by-analysis/analysis ph))
                             :label "Analysis"}))
               s (when-let [phs (:feature/defined-by-sequence f)]
                   (for [ph phs]
                     {:object (pack-obj (:feature.defined-by-sequence/sequence ph))
                      :label "Sequence"}))]
           (remove nil? [person paper analysis s]))
   :description "how the sequence feature was defined"})

(defn description [f]
  {:data (first (:feature/description f))
   :desciption (str "description of the Feature " (:feature/id f))})

(defn transcription-factor [f]
  {:data (when-let [tfh (first (:feature/associated-with-transcription-factor f))]
           (pack-obj (:feature.associated-with-transcription-factor/transcription-factor tfh)))
   :description "Transcription factor of the feature"})

(def widget
  {:name generic/name-field
   :sequence_ontology_terms sequence-ontology-terms
   :binds_gene_product binds-gene-product
   :taxonomgy generic/taxonomy
   :defined_by defined-by
   :description description
   :transcription_factor transcription-factor
   :remarks generic/remarks
   :method generic/method
   :other_names generic/other-names})
