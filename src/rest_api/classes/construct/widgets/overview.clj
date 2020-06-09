(ns rest-api.classes.construct.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn construction-summary [construct]
  {:data (first (:construct/construction-summary construct))
   :description "Construction details for the transgene"})

(defn other-reporter [construct]
  {:data (when-let [chs (:construct/other-reporter construct)] (first chs))
   :description (str "The name and WormBase internal ID of " (:construct/id construct))})

(defn selection-marker [construct]
  {:data (:construct/selection-marker construct)
   :description "Coinjection marker for this transgene"})

(defn type-of-construct [construct] ; This does not show up on the page
  {:data (:construct/type-of-construct construct)
   :description "type of construct"})

(defn used-for [construct]
  {:data (not-empty
           (remove
             nil?
             (flatten
               (conj
                 (some->> (:construct/transgene-construct construct)
                          (map (fn [tg]
                                 {:use_lab (some->> (:transgene/laboratory tg)
                                                    (map :transgene.laboratory/laboratory)
                                                    (map pack-obj))
                                  :use_summary (:transgene.summary/text (:transgene/summary tg))
                                  :used_in (pack-obj tg)
                                  :evidence (obj/get-evidence tg)
                                  :used_in_type "Transgene construct"})))
                 (some->> (:construct/transgene-coinjection construct)
                          (map (fn [tg]
                                 {:use_lab (some->> (:transgene/laboratory tg)
                                                    (map :transgene.laboratory/laboratory)
                                                    (map pack-obj))
                                  :use_summary (:transgene.summary/text (:transgene/summary tg))
                                  :used_in (pack-obj tg)
                                  :evidence (obj/get-evidence tg)
                                  :used_in_type "Transgene coinjection"})))
                 (some->> (:expr-pattern/_construct construct)
                          (map (fn [ep]
                                 {:use_lab []
                                  :use_summary nil
                                  :used_in {:taxonomy "all"
                                            :class "expr_pattern"
                                            :label (let [gene (:expr-pattern.gene/gene (first (:expr-pattern/gene ep)))]
                                                     (str "Expression pattern for " (or
                                                                                      (or (:gene/public-name gene )
                                                                                          (:gene/id gene))
                                                                                      "")))
                                            :id (:expr-pattern/id ep)}
                                  :evidence (obj/get-evidence ep)
                                  :used_in_type "Expression pattern"})))
                 (some->> (:variation/_derived-from-construct construct)
                          (map (fn [v]
                                 {:use_lab (some->> (:variation/laboratory v)
                                                    (map pack-obj))
                                  :use_summary nil
                                  :used_in (pack-obj v)
                                  :evidence (obj/get-evidence v)
                                  :used_in_type "Engineered variation"})))
                 (some->> (:interactor-info/_construct construct)
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
                                                                                   (:gene/id gene))))))
                                              :id (:interaction/id i)})
                                  :evidence (obj/get-evidence ih)
                                  :used_in_type "Interaction"})))
                 (some->> (:wbprocess/_marker-construct construct)
                          (map (fn [wbp]
                                 {:use_lab []
                                  :use_summary (:wbprocess.summary/text (:wbprocess/summary wbp))
                                  :used_in (pack-obj wbp)
                                  :evidence (obj/get-evidence wbp)
                                  :used_in_type "Topic output_indica"})))))))
   :description "The Construct is used for"})

(defn utr [construct]
  {:data (some->> (:construct/three-prime-utr construct)
                  (map :construct.three-prime-utr/gene)
                  (map pack-obj))
   :description "3' UTR for this transgene"})

(defn driven-by-gene [c]
    {:data (some->> (:construct/driven-by-gene c)
                    (map :construct.driven-by-gene/gene)
                    (map pack-obj))
     :description "gene that drives the construct"});)

(defn fusion-reporter [c]
    {:data (some->> (:construct/fusion-reporter c))
     :description "Reporter for this construct"})

(def widget
  {:construction_summary construction-summary
   :driven_by_gene driven-by-gene
   :fusion_reporter fusion-reporter
   :gene_product generic/gene-product
   :other_reporter other-reporter
   :remarks generic/remarks
   :selection_marker selection-marker
   :summary generic/summary
   :type_of_construct type-of-construct
   :used_for used-for
   :utr utr
   :name generic/name-field})
