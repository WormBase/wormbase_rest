(ns rest-api.classes.rearrangement.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mapping-data [r]
  {:data (when-let [pnds (:rearrangement/pos-neg-data r)]
           (remove
             nil?
             (flatten
               (for [pnd pnds]
                 (for [reference [:pos-neg-data/gene-1
                                  :pos-neg-data/gene-2
                                  :pos-neg-data/locus-1
                                  :pos-neg-data/locus-2
                                  :pos-neg-data/rearrangement-1
                                  :pos-neg-data/rearrangement-2
                                  :pos-neg-data/allele-1
                                  :pos-neg-data/allele-2
                                  :pos-neg-data/clone-1
                                  :pos-neg-data/clone-2
                                  ]]
                   (when-let [entity (reference pnd)]
		     {:type (when-let [calc (:pos-neg-data/calculation pnd)]
			      (if (= "positive"(name calc))
				"+" "-"))
		      :class (cond
                               (or (= reference :pos-neg-data/locus-1)
                                   (= reference :pos-neg-data/locus-2))
                               "Locus"

                                (or (= reference :pos-neg-data/gene-1)
                                   (= reference :pos-neg-data/gene-2))
                               "Gene"

                               (or (= reference :pos-neg-data/rearrangement-1)
                                   (= reference :pos-neg-data/rearrangement-2))
                               "Rearrangement"

                               (or (= reference :pos-neg-data/allele-1)
                                   (= reference :pos-neg-data/allele-2))
                               "Allele"

                               (or (= reference :pos-neg-data/clone-1)
                                   (= reference :pos-neg-data/clone-2))
                               "Clone")
                      :name (cond
                              (= reference :pos-neg-data/locus-1)
                              (pack-obj (:pos-neg-data.locus-1/locus entity))

                              (= reference :pos-neg-data/locus-2)
                              (pack-obj (:pos-neg-data.locus-2/locus entity))

                              (= reference :pos-neg-data/gene-1)
                              (pack-obj (:pos-neg-data.gene-1/gene entity))

                              (= reference :pos-neg-data/gene-2)
                              (pack-obj (:pos-neg-data.gene-2/gene entity))

                              :else
                              (pack-obj entity))
                      :author (when-let [authors (:pos-neg-data/mapper pnd)]
                                (for [author authors]
                                  (pack-obj author)))
                      :genotype (:pos-neg-data/genotype pnd)
                      :position (if-let [gene (or (when (= reference :pos-neg-data/gene-1)
                                                    (:pos-neg-data.gene-1/gene
                                                      (:pos-neg-data/gene-1 pnd)))
                                                  (when (= reference :pos-neg-data/gene-2)
                                                    (:pos-neg-data.gene-2/gene
                                                      (:pos-neg-data/gene-2 pnd))))]
                                  (let [position (:map-position/position (:gene/map gene))
                                        position-float (:map-position.position/float position)
                                        map-error (:map-error/error position)
                                        map_id (:map/id (:gene.map/map (:gene/map gene)))
                                        result (format "%s: %.2f" map_id position-float)]
                                    (if (some? map-error)
                                      (format "%s +/- %.3f" result map-error)
                                      result))
                                  "-")
                      :remark (when-let [rhs (:pos-neg-data/remark pnd)]
                                (for [rh rhs]
                                  (:pos-neg-data.remark/text rh)))
                      :results (:pos-neg-data/results pnd)}))))))
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
             {:info
              {:genotype
               {:str (:strain/genotype strain)

                :data (flatten
                        (remove
                          nil?
                          (conj
                            (when-let [genes (:gene/_strain strain)]
                              (for [gene genes]
                                {(:label (pack-obj gene))
                                 (pack-obj gene)}))
                            (when-let [rearrangements (:rearrangement/_strain strain)]
                              (for [rearrangement rearrangements]
                                {(:label (pack-obj rearrangement))
                                 (pack-obj rearrangement)}))
                            (when-let [vhs (:variation.strain/_strain strain)] ;arDf1
                              (for [vh vhs
                                    :let [v (:variation/_strain vh)]]
                                {(:label (pack-obj v))
                                 (pack-obj v)}))
                            (when-let [clones (:clone/_in-strain strain)]
                              (for [clone clones]
                                {(:label (pack-obj clone))
                                 (pack-obj clone)}))
                            (when-let [tgs (:transgene/_strain strain)]
                              (for [tg tgs]
                                {(:label (pack-obj tg))
                                 (pack-obj tg)})))))}}
              :strain (pack-obj strain)}))
   :description "Strains associated with the Rearrangement"})

(defn type-field [r]
  {:data (when-let [ts (:rearrangement/type r)]
           (for [t ts]
             (str/capitalize (name t))))
   :description "the type of rearrangement"})

(defn positive [r]
  {:data (when-let [gihs (:rearrangement/gene-inside r)]
           {"Genes inside"
            (for [gih gihs]
              (pack-obj (:rearrangement.gene-inside/gene gih)))})
   :description "Covered by rearrangement"})

(defn negative [r]
  {:data (when-let [gihs (:rearrangement/gene-outside r)]
           {"Genes outside"
            (for [gih gihs]
              (pack-obj (:rearrangement.gene-outside/gene gih)))})
   :description "Not covered by rearrangement"})

(def widget
  {:name generic/name-field
   :mapping_data mapping-data
   :display display
   :chromosome chromosome
   :reference_strain reference-strain
   :strains strains
   :remarks generic/remarks
   :type type-field
   :positive positive
   :negative negative})
