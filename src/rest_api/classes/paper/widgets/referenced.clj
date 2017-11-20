(ns rest-api.classes.paper.widgets.referenced
  (:require
    [pseudoace.utils :as pace-utils]
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn refer-to [p]
  {:data (pace-utils/vmap
           "Species"
           (some->> (:paper/species p)
                    (map :paper.species/species)
                    (map pack-obj))

           "SAGE_experiment"
           (some->> (:sage-experiment/_reference p)
                    (map pack-obj))

           "Gene" ; need to figure out for published as for WBPaper00002036
           (some->> (:gene/_reference p)
                    (map (fn [g]
                           (if-let [ev (keys (:gene/evidence g))]
                             {:text (pack-obj g)
                              :evidence ev}
                             (pack-obj g)))))

           "Interaction"
           (some->> (:interaction/_paper p)
                    (map pack-obj))

           "WBProcess"
           (some->> (:wbprocess/_reference p)
                    (map pack-obj))

           "Expression_cluster"
           (some->> (:expression-cluster.reference/_paper p)
                    (map :expression-cluster/_reference)
                    (map pack-obj))

           "Microarray_experiment"
           (some->> (:microarray-experiment/_reference p)
                    (map pack-obj))

           "Go_term"
           (some->> (:go-annotation/_reference p)
                    (map :go-annotation/go-term)
                    (map (fn [term]
                           {(:go-term/id term)
                            (pack-obj term)})))

           "Cell"  ; WBPaper00022662662
           nil

           "Cell_group"
           nil ; cell group not in database. can't find where info comes from example WBPaper00005376

           "Transgene"
           (some->> (:transgene.reference/_paper p)
                    (map :transgene/_reference)
                    (map pack-obj)
                    )

           "Feature"
           (some->> (:feature.defined-by-paper/_paper p)
                    (map :feature/_defined-by-paper)
                    (map pack-obj))

           "Rearrangement"
           (some->> (:rearrangement.reference/_paper p)
                    (map :rearrangement/_reference)
                    (map pack-obj))

           "Sequence"
           (some->> (:sequence.reference/_paper p)
                    (map :sequence/_reference)
                    (map pack-obj))

           "Alleles"
           (some->> (:variation.reference/_paper p)
                    (map :variation/_reference)
                    (map pack-obj))

           "CDS"
           (some->> (:cds.reference/_paper p)
                    (map :cds/_reference)
                    (map pack-obj))

           "Expr_pattern"
           (some->> (:expr-pattern.reference/_paper p)
                    (map :expr-pattern/_reference)
                    (map pack-obj))

           "Picture"
           (when-let [papers (:picture/_reference p)]
             {:Curated_images
              (some->> papers
                       (map (fn [pic]
                              (when-let [filename (:picture/name pic)]
                                (let [[n f] (str/split filename #"\.")]
                                  (conj
                                    {:thumbnail
                                     {:format f
                                      :name (str (:picture/id pic) "/" n)
                                      :class "img-static/pictures"}}
                                    (pack-obj pic)))))))})

           "Antibody"
           (some->> (:antibody.reference/_paper p)
                    (map :antibody/_reference)
                    (map pack-obj))

           "Strain"
           (some->> (:strain.reference/_paper p)
                    (map :strain/_reference)
                    (map pack-obj))

           "Clone"
           (some->> (:clone.reference/_paper p)
                    (map :clone/_reference)
                    (map pack-obj))

           "Life_stage"
           (some->> (:life-stage.reference/_paper p)
                    (map :life-stage/_reference)
                    (map pack-obj))

           "RNAi"
           (some->> (:rnai.reference/_paper p)
                    (map :rnai/_reference)
                    (map pack-obj))

           "Transript"
           (some->> (:rnai.reference/_paper p)
                    (map :rnai/_reference)
                    (map pack-obj))

           "Transript"
           (some->> (:expr-profile.reference/_paper p)
                    (map :expr-profile/_reference)
                    (map pack-obj))

           "Operon"
           (some->> (:operon.reference/_paper p)
                    (map :operon/_reference)
                    (map pack-obj))

           "Gene_cluster"
           (some->> (:gene-cluster.reference/_paper p)
                    (map :gene-cluster/_reference)
                    (map pack-obj))

           "Anatomy_function"
           (some->> (:anatomy-function/_reference p)
                    (map pack-obj))

           "Mass_spec_experiment"
           (some->> (:mass-spec-experiment/_reference p)
                    (map pack-obj))

           "Molecule"
           (some->> (:molecule/_reference p)
                    (map pack-obj))

           "Movie"
           (some->> (:movie/_reference p)
                    (map pack-obj)))
:definition "Items that the publication refers to"})

(def widget
  {:name generic/name-field
   :refer_to refer-to})
