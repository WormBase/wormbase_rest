(ns rest-api.classes.paper.widgets.referenced
  (:require
    [pseudoace.utils :as pace-utils]
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn get-gene-name [g]
  (or (:gene/public-name g)
      (or (:gene/other-name g)
          (:gene/id g))))

(defn refers-to [p]
  {:data {
           "Species"
           (some->> (:paper/species p)
                    (map :paper.species/species)
                    (map pack-obj)
                    (sort-by :label))

           "SAGE_experiment"
           (some->> (:sage-experiment/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Gene"
           (some->> (:gene/_reference p)
                    (map (fn [g]
                           (if-let [ev (:gene/evidence g)]
                             {:text (pack-obj g)
                              :evidence (obj/get-evidence ev)}
                             (pack-obj g))))
                    (sort-by :label))

           "Interaction"
           (some->> (:interaction/_paper p)
                    (map pack-obj)
                    (sort-by :label))

           "WBProcess"
           (some->> (:wbprocess/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Expression_cluster"
           (some->> (:expression-cluster.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:expression-cluster/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Microarray_experiment"
           (some->> (:microarray-experiment/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Go_term"
           (some->> (:go-annotation/_reference p)
                    (map :go-annotation/go-term)
                    (distinct)
                    (map pack-obj)
                    (sort-by :label))

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
                               (pack-obj obj)))))
                    (sort-by :label))

           "Feature"
           (some->> (:feature.defined-by-paper/_paper p)
                    (map (fn [h]
                           (let [obj (:feature/_defined-by-paper h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Rearrangement"
           (some->> (:rearrangement.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:rearrangement/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Sequence"
           (some->> (:sequence.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:sequence/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Alleles"
           (some->> (:variation.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:variation/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "CDS"
           (some->> (:cds.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:cds/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Expr_pattern"
           (some->> (:expr-pattern.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:expr-pattern/_reference h)]
                             (let [packed-obj (assoc-in
                                                (pack-obj obj)
                                                [:label]
                                                (some->> (:expr-pattern/gene obj)
                                                         (first)
                                                         (:expr-pattern.gene/gene)
                                                         (get-gene-name)
                                                         (str "Expression pattern for ")))]
                               (if-let [ev (obj/get-evidence h)]
                                 {:text packed-obj
                                  :evidence ev}
                                 packed-obj)))))
                    (sort-by :label))

           "Picture"
           (when-let [papers (:picture/_reference p)]
             {:curated_images
              (some->> papers
                       (map (fn [pic]
                              (when-let [filename (:picture/name pic)]
                                (let [[n f] (str/split filename #"\.")]
                                  (conj
                                    {:thumbnail
                                     {:format f
                                      :name (str (:paper/id p) "/" n)
                                      :class "/img-static/pictures"}}
                                    (pack-obj pic))))))
                       (sort-by :label))})

           "Antibody"
           (some->> (:antibody.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:antibody/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Strain"
           (some->> (:strain.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:strain/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Clone"
           (some->> (:clone.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:clone/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Life_stage"
           (some->> (:life-stage.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:life-stage/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "RNAi"
           (some->> (:rnai.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:rnai/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Transript"
           (some->> (:transcript.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:transcript/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Expr_profile"
           (some->> (:expr-profile.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:expr-profile/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Operon"
           (some->> (:operon.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:operon/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Gene_cluster"
           (some->> (:gene-cluster.reference/_paper p)
                    (map (fn [h]
                           (let [obj (:gene-cluster/_reference h)]
                             (if-let [ev (obj/get-evidence h)]
                               {:text (pack-obj obj)
                                :evidence ev}
                               (pack-obj obj)))))
                    (sort-by :label))

           "Anatomy_function"
           (some->> (:anatomy-function/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Mass_spec_experiment"
           (some->> (:mass-spec-experiment/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Molecule"
           (some->> (:molecule/_reference p)
                    (map pack-obj)
                    (sort-by :label))

           "Movie"
           (some->> (:movie/_reference p)
                    (map pack-obj)
                    (sort-by :label))
   }
   :definition "Items that the publication refers to"})

(def widget
  {:name generic/name-field
   :refers_to refers-to})
