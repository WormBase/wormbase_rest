(ns rest-api.classes.transgene.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn other-reporter [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map :construct/other-reporter)
                  (remove nil?)
                  (first))
   :description "other reporters of this construct"})

(defn utr [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map (fn [c]
                         (some->> (:construct/three-prime-utr c)
                                  (map :construct.three-prime-utr/gene)
                                  (map pack-obj))))
                  (flatten)
                  (remove nil?)
                  (not-empty))
   :description "3' UTR for this transgene"})

(defn used-for [t]
  {:data (not-empty
           (remove
             nil?
             (flatten
               (conj
                 (some->> (:transgene/_integrated-from t)
                          (map (fn [tg]
                                 {:use_lab (some->> (:transgene/laboratory tg)
                                                    (map :transgene.laboratory/laboratory)
                                                    (map pack-obj))
                                  :use_summary (:transgene.summary/text (:transgene/summary tg))
                                  :used_in (pack-obj tg)
                                  :evidence (obj/get-evidence tg)
                                  :used_in_type "Transgene derivative"})))
                 (some->> (:transgene/marker-for t)
                          (map (fn [h]
                                 {:use_lab []
                                  :use_summary nil
                                  :used_in {:id (:transgene.marker-for/text h)
                                            :label (:transgene.marker-for/text h)
                                            :class "text"
                                            :taxonomy "all"}
                                  :used_in_type "Marker for"})))
                 (some->> (:expr-pattern/_transgene t)
                          (map (fn [ep]
                                 {:use_lab []
                                  :use_summary nil
                                  :used_in {:taxonomy "all"
                                            :class "expr_pattern"
                                            :label (let [gene (:expr-pattern.gene/gene (first (:expr-pattern/gene ep)))]
                                                     (str "Expression pattern for " (or (:gene/public-name gene)
                                                                                          (get :gene/id gene ""))))
                                            :id (:expr-pattern/id ep)}
                                  :evidence (obj/get-evidence ep)
                                  :used_in_type "Expr pattern"})))
                 (some->> (:interactor-info/_transgene t)
                          (map (fn [ih]
                                 {:use_lab []
                                  :use_summary nil
                                  :used_in (let [i (:interaction/_interactor-overlapping-gene ih)
                                                 ghs (:interaction/interactor-overlapping-gene i)]
                                             {:taxonomy "all"
                                              :class "interaction"
                                              :label (str/join " : "
                                                               (some->> (:interaction/interactor-overlapping-gene i)
                                                                        (map :interaction.interactor-overlapping-gene/gene)
                                                                        (map (fn [gene]
                                                                               (or (:gene/public-name gene)
                                                                                   (get :gene/id gene ""))))))
                                              :id (:interaction/id i)})
                                  :evidence (obj/get-evidence ih)
                                  :used_in_type "Interactor"})))))))
   :description "The Transgene is used for"})

(defn marked-rearrangement [t]
  {:data (some->> (:rearrangement/_marked-by-transgene t)
		  (map pack-obj))
   :description "rearrangements that the transgene can be used as a marker for"})

(defn fusion-reporter [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map :construct/fusion-reporter)
                  (flatten)
                  (remove nil?)
                  (first))
   :description "reporter construct for this construct"})

(defn synonym [t]
  {:data (sort (:transgene/synonym t))
   :description "a synonym for the transgene"})

(defn driven-by-gene [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map (fn [h]
                         (some->> (:construct/driven-by-gene h)
                                  (map :construct.driven-by-gene/gene)
                                  (map pack-obj))))
                  (flatten)
                  (remove nil?)
                  (not-empty))
   :description "gene that drives the transgene"})

(defn purification-tag [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map (fn [c]
                         (some->> (:construct/purification-tag c))))
                  (flatten)
                  (remove nil?)
                  (first))
   :description "the purification tag for the construct"})

(defn strains [t]
  {:data (some->> (:transgene/strain t)
                  (map pack-obj)
                  (sort-by :label))
   :description "Strains associated with this transgene"})

(defn recombination-site [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map (fn [c]
                        (:construct/recombinant-site c)))
                  (flatten)
                  (remove nil?)
                  (not-empty))
   :description "map position of the integrated transgene"})

(defn gene-product [t]
  {:data (some->> (:construct/_transgene-construct t)
                  (map (fn [construct]
                         (some->> (:construct/gene construct)
                                  (map :construct.gene/gene)
                                  (map pack-obj))))
                  (flatten)
                  (remove nil?)
                  (sort-by :label)
                  (not-empty))
   :description "gene products for this transgene"})

(defn integrated-from [t]
  {:data (some->> (:transgene/integrated-from t)
                  (map pack-obj))
   :description "integrated from"})

(defn transgene-derivation [t]
  {:data (some->> (:transgene/_integrated-from t)
                  (map pack-obj))
   :d (:db/id t)
   :description "derived from"})

(def widget
  {:name generic/name-field
   :other_reporter other-reporter
   :utr utr
   :integrated_from integrated-from
   :transgene_derivation transgene-derivation
   :used_for used-for
   :marked_rearrangement marked-rearrangement
   :taxonomy generic/taxonomy
   :fusion_reporter fusion-reporter
   :summary generic/summary
   :synonym synonym
   :driven_by_gene driven-by-gene
   :purification_tag purification-tag
   :strains strains
   :remarks generic/remarks
   :recombination_site recombination-site
   :gene_product gene-product})
