(ns rest-api.classes.rearrangement.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mapping-data [r]
  {:data (when-let [pnds (:rearrangement/pos-neg-data r)]
           (for [pnd pnds]
             {:type (if (= "positive"(name (:pos-neg-data/calculation pnd)))
                      "+" "-")
              :author (when-let [authors (:pos-neg-data/mapper pnd)]
                        (for [author authors]
                          (pack-obj author)))
              :genotype (:pos-neg-data/genotype pnd)
              :remark (when-let [rhs (:pos-neg-data/remark pnd)]
                        (for [rh rhs]
                          {:text (:pos-neg-data.remark/text rh)
                           :evidence (obj/get-evidence rh)}))
              :results (:pos-neg-data/results pnd)}

              ; need to check all  Gene_1 Locus_1 Rearrangement_1 Allele_1 Clone_1  and then same for 2
             )
           )
   :description "the mapping data of the rearrangement"})

(defn display [r]
  {:data (not-empty
           (pace-utils/vmap
             "Hidden Under"
             (when-let [hu (:rearrangement/hide-under r)]
               (pack-obj hu))

             "Hides"
             (when-let [hus (:rearrangement/_hide-under r)]
               (for [hu hus]
                 (pack-obj hu)))))
   :description "Rearrangements Hiding/Hidden by this rearrangement"})

(defn chromosome [r]
  {:data (when-let [mp (:rearrangement/map r)]
           (let [chr (:map/id (:rearrangement.map/map (first mp)))
                 left (when-let [mh (:map-position/left (first mp))]
                        (format "%.2f" (:map-position.left/float mh)))
                 right (when-let [mh (:map-position/right (first mp))]
                         (format "%.3f" (:map-position.right/float mh)))]
          (if (or (nil? left) (nil? right))
             ""
             (str chr " " left " to " right))))
   :description "Reference strains for the Rearrangement"})

(defn reference-strain [r]
  {:data (when-let [strains (:rearrangement/reference-strain r)]
           (for [strain strains]
             (pack-obj strain)))
   :description "Reference strains for the Rearrangement"})

(defn strains [r]
  {:data (when-let [strains (:rearrangement/strain r)]
           (for [strain strains]
             {:info (let [genotype (:strain/genotype strain)
                          elements nil] ; need to check Gene, variation, rearrangement, clone, transgene
                          {:str genotype
                           :data elements})
              :keys (keys strain)
              :deb (:db/id strain)
              :strain (pack-obj strain)}))
   :description "Strains associated with the Rearrangement"})

(defn type-field [r]
  {:data (when-let [ts (:rearrangement/type r)]
           (for [t ts]
             (str/capitalize (name t))))
   :description "the type of rearrangement"})

(defn positive [r]
  {:data (when-let [gihs (:rearrangement/gene-inside r)]
           {"Gene inside"
            (for [gih gihs]
              (pack-obj (:rearrangement.gene-inside/gene gih)))})
   :description "Covered by rearrangement"})

(defn negative [r]
  {:data (when-let [gihs (:rearrangement/gene-outside r)]
           {"Gene outside"
            (for [gih gihs]
              (pack-obj (:rearrangement.gene-outside/gene gih)))})
   :description "Not covered by rearrangement"})

(def widget
  {:name generic/name-field
   :mapping_data mapping-data
   :display display
   :choromosome chromosome
   :reference_strain reference-strain
;   :strains strains
   :remarks generic/remarks
   :type type-field
   :positive positive
   :negative negative})
