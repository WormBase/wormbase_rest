(ns rest-api.classes.construct.widgets.isolation
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn clone [c]
  {:data (some->> (:construct/clone c)
                  (map pack-obj)
                  (first))
   :description "the clone of this construct"})

(defn construction-summary [c]
  {:data (first (:construct/construction-summary c))
   :d (:db/id c)
   :description "Construction details for the transgene"})

(defn historical-gene [c]
  {:data (some->> (:construct/historical-gene c)
                  (map (fn [h]
                         (let [gene (:construct.historical-gene/gene h)]
                           {:text (pack-obj gene)
                            :label (:id (pack-obj gene))
                            :evidence (conj
                                        {(:construct.historical-gene/text h) ""}
                                        (obj/get-evidence h))})))
                  (sort-by :label))
   :description "Historical record of the dead genes originally associated with this transgene"})

(defn dna-text [c]
  {:data (:construct/dna-text c)
   :descriptoiin "DNA sequence of the construct"})

(def widget
  {:laboratory generic/laboratory
   :clone clone
   :construction_summary construction-summary
   :historical_gene historical-gene
   :dna_text dna-text
   :name generic/name-field})
