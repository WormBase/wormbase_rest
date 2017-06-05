(ns rest-api.classes.construct.widgets.overview
  (:require
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

(defn type-of-construct [construct] ; not finished - need JSON to be fixed for this field
  {:data (:construct/type-of-construct construct)
   :description "type of construct"})

(defn used-for [construct] ; not finished
  {:data (when-let [tgs (:construct/transgene-construct construct)]
           (for [tg tgs]
             {:use_lab (for [lab (:transgene/laboratory tg)]
                         (pack-obj (:transgene.laboratory/laboratory lab)))
              :use_summary (:transgene.summary/text (:transgene/summary tg))
              :used_in (pack-obj tg)
              :used_in_type (cond
                             (contains? construct :construct/transgene-construct)
                             "Transgene construct"

                             (contains? construct :construct/transgene-coinjection)
                             "Transgene coinjection"

                             (contains? construct :variation/_derived-from-construct)
                             "Engineered variation"

                             (contains? construct :wbprocess/_marker-construct) ;not sure
                             "Topic output indicator"

                             (contains? construct :expr-pattern/_construct)
                             "Expression-pattern"

                             (contains? construct :interaction/_unaffiliated-construct) ; not sure if this is the right way
                             "Interactor")}))
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
