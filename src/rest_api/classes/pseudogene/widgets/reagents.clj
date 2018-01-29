(ns rest-api.classes.pseudogene.widgets.reagents
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn sage-tags [p]; this can not be found in database
  {:data (some->> (:sage-tag.pseudogene/_pseudogene p)
                  (map :sage-tag/_pseudogene)
                  (map pack-obj)
                  (sort-by :label))
   :description "SAGE tags identified"})

(defn matching-cdnas [p]
  {:data (some->> (:pseudogene/matching-cdna p)
                  (map :pseudogene.matching-cdna/sequence)
                  (map pack-obj)
                  (sort-by :label))
   :description "cDNAs matching this pseudogene"})

(def widget
  {:name generic/name-field
   :sage_tags sage-tags
   :matching_cdnas matching-cdnas})
