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
                 (when-let [tgs (:construct/transgene-construct construct)]
                   (for [tg tgs]
                     {:use_lab (for [lab (:transgene/laboratory tg)]
                                 (pack-obj (:transgene.laboratory/laboratory lab)))
                      :use_summary (:transgene.summary/text (:transgene/summary tg))
                      :used_in (pack-obj tg)
                      :used_in_type "Transgene construct"}))
                 (when-let [tgs (:construct/transgene-coinjection construct)]
                   (for [tg tgs]
                     {:use_lab (for [lab (:transgene/laboratory tg)]
                                 (pack-obj (:transgene.laboratory/laboratory lab)))
                      :use_summary (:transgene.summary/text (:transgene/summary tg))
                      :used_in (pack-obj tg)
                      :used_in_type "Transgene coinjection"}))
                 (when-let [eps (:expr-pattern/_construct construct)]
                   (for [ep eps]
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
                      :used_in_type "Expression pattern"}))
                 (when-let [vs (:variation/_derived-from-construct construct)]
                   (for [v vs]
                     {:use_lab (for [lab (:variation/laboratory v)] (pack-obj(lab)))
                      :use_summary nil
                      :used_in (pack-obj v)
                      :used_in_type "Engineered variation"}))
                 (when-let [ihs (:interactor-info/_construct construct)] 
                   (for [ih ihs]
                     {:use_lab []
                      :use_summary nil
                      :used_in (let [i (:interaction/_interactor-overlapping-gene ih)
                                     ghs (:interaction/interactor-overlapping-gene i)]
                                   {:taxonomy "all"
                                    :class "interaction"
                                    :label (str/join " : "
                                                     (for [gh ghs
                                                           :let [gene (:interaction.interactor-overlapping-gene/gene gh)]]
                                                       (or (:gene/public-name gene)
                                                           (:gene/id gene))))
                                    :id (:interaction/id i)})
                      :used_in_type "interaction"}))
                 (when-let [wbps (:wbprocess/_marker-construct construct)] ;; no examples in database
                   (for [wbp wbps]
                     {:use_lab []
                      :use_summary (:wbprocess.summary/text (:wbprocess/summary wbp))
                      :used_in (pack-obj wbp)
                      :used_in_type "Topic output_indica"}))))))
   :description "The Construct is used for"})

(defn utr [construct]
  {:data (when-let [ths (:construct/three-prime-utr construct)]
           (for [th ths :let [gene (:construct.three-prime-utr/gene th)]]
             (pack-obj gene)))
   :description "3' UTR for this transgene"})

(def widget
  {:construction_summary construction-summary
   :driven_by_gene generic/driven-by-gene
   :fusion_reporter generic/fusion-reporter
   :gene_product generic/gene-product
   :other_reporter other-reporter
   :remarks generic/remarks
   :selection_marker selection-marker
   :summary generic/summary
   :type_of_construct type-of-construct
   :used_for used-for
   :utr utr
   :name generic/name-field})
