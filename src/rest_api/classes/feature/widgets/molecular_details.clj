(ns rest-api.classes.feature.widgets.molecular-details
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn flanking-sequences [f]
  {:data {:flanks [(:feature.flanking-sequences/five-prime (:feature/flanking-sequences f))
                   (:feature.flanking-sequences/three-prime (:feature/flanking-sequences f))]
          :feture_seq nil
          :seq nil 
          :sequences  [{:sequence (:feature.flanking-sequences/five-prime (:feature/flanking-sequences f))
                        :comment "flanking sequnece (upstream)"}
                       {:sequence (:feature.flanking-sequences/three-prime (:feature/flanking-sequences f))
                        :comment "flanking sequence (downstream)"}]}
   :description "sequences flanking the feature"})

(defn dna-text [f]
  {:data nil
   :description "DNA text of the sequence feature"})

(def widget
  {:name generic/name-field
   :flanking_sequences flanking-sequences
   :dna_text dna-text})
