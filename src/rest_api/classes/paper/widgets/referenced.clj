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
                   (map (fn [h]
                          (let [obj (:expression-cluster/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

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
                   (map (fn [h]
                          (let [obj (:transgene/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "Feature"
          (some->> (:feature.defined-by-paper/_paper p)
                   (map (fn [h]
                          (let [obj (:feature/_defined-by-paper h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "Rearrangement"
          (some->> (:rearrangement.reference/_paper p)
                   (map (fn [h]
                          (let [obj (:rearrangement/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "Sequence"
          (some->> (:sequence.reference/_paper p)
                   (map (fn [h]
                          (let [obj (:sequence/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "Alleles"
          (some->> (:variation.reference/_paper p)
                    (map (fn [h]
                          (let [obj (:variation/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "CDS"
          (some->> (:cds.reference/_paper p)
                   (map (fn [h]
                          (let [obj (:cds/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

          "Expr_pattern"
          (some->> (:expr-pattern.reference/_paper p)
                   (map (fn [h]
                          (let [obj (:expr-pattern/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

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
                    (map (fn [h]
                          (let [obj (:antibody/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

             "Strain"
             (some->> (:strain.reference/_paper p)
                     (map (fn [h]
                          (let [obj (:strain/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

             "Clone"
             (some->> (:clone.reference/_paper p)
                      (map (fn [h]
                          (let [obj (:clone/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "Life_stage"
              (some->> (:life-stage.reference/_paper p)
                       (map (fn [h]
                          (let [obj (:life-stage/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "RNAi"
              (some->> (:rnai.reference/_paper p)
                        (map (fn [h]
                          (let [obj (:rnai/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "Transript"
              (some->> (:transcript.reference/_paper p)
                         (map (fn [h]
                          (let [obj (:transcript/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "Expr_profile"
              (some->> (:expr-profile.reference/_paper p)
                          (map (fn [h]
                          (let [obj (:expr-profile/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "Operon"
              (some->> (:operon.reference/_paper p)
                           (map (fn [h]
                          (let [obj (:operon/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

              "Gene_cluster"
              (some->> (:gene-cluster.reference/_paper p)
                            (map (fn [h]
                          (let [obj (:gene-cluster/_reference h)]
                            (if-let [ev (obj/get-evidence h)]
                              {:text (pack-obj obj)
                               :evidence ev}
                              (pack-obj obj))))))

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
