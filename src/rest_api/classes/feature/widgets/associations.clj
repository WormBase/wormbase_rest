(ns rest-api.classes.feature.widgets.associations
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn associations [f]
  {:data (not-empty
           (sort-by :label
                    (flatten
                      (vals
                        (pace-utils/vmap
                          :cds
                          (some->> (:feature/associated-with-cds f)
                                   (map :feature.associated-with-cds/cds)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "CDS")))

                          :feature
                          (some->> (:feature/associated-with-feature f)
                                   (map :feature.associated-with-feature/feature)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Feature")))

                          :gene
                          (some->> (:feature/associated-with-gene f)
                                   (map :feature.associated-with-gene/gene)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Gene")))

                          :operon
                          (some->> (:feature/associated-with-operon f)
                                   (map :feature.associated-with-operon/operon)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Operon")))

                          :position-matrix
                          (some->> (:feature/associated-with-position-matrix f)
                                   (map :feature.associated-with-position-matrix/position-matrix)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Position Matrix")))

                          :pseudogene
                          (some->> (:feature/associated-with-pseudogene f)
                                   (map :feature.associated-with-pseudogene/pseudogene)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Pseudogene")))

                          :transcript
                          (some->> (:feature/associated-with-transcript f)
                                   (map :feature.associated-with-transcript/transcript)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Transcript")))

                          :transcription-factor
                          (some->> (:feature/associated-with-transcription-factor f)
                                   (map :feature.associated-with-transcription-factor/transcription-factor)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Transcription Factor")))

                          :transposon
                          (some->> (:feature/associated-with-transposon f)
                                   (map :feature.associated-with-transposon/transposon)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Transposon")))

                          :variation
                          (some->> (:feature/associated-with-variation f)
                                   (map :feature.associated-with-variation/variation)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Variation")))
                          :expr-patern
                          (some->> (:expr-pattern.associated-feature/_feature f)
                                   (map :expr-pattern/_associated-feature)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Expression Pattern")))
                          :interaction
                          (some->> (:interaction.feature-interactor/_feature f)
                                   (map :interaction/_feature-interactor)
                                   (map #(pace-utils/vmap
                                           :association (pack-obj %)
                                           :label "Variation"))))))))
   :description "objects that define this feature"})

(defn binds-gene-product [f]
  {:data (some->> (:feature/bound-by-product-of f)
                  (map :feature.bound-by-product-of/gene)
                  (map pack-obj))
   :description "gene products that bind to the feature"})

(defn transcription-factor [f]
  {:data (when-let [tf (:feature.associated-with-transcription-factor/transcription-factor
                         (first
                           (:feature/associated-with-transcription-factor f)))]
           (pack-obj tf))
   :description "Transcription factor of the feature"})

(def widget
  {:name generic/name-field
   :associations associations
   :binds_gene_product binds-gene-product
   :transcription_factor transcription-factor})
