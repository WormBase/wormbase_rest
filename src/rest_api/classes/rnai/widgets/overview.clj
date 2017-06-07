(ns rest-api.classes.rnai.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn historical-name [r]
  {:data (:rnai/history-name r)
   :desciprition "historical name of the rnai"})

(defn- get-target-type [object]
  (when-let [ia (first (:evidence/inferred-automatically object))]
    (if (re-find #"primary" ia)
      "Primary target"
      "Secondary target")))

(defn targets [r]
  {:data (not-empty
           (remove
             nil?
             (flatten
             (conj
               (for [gh (:rnai/predicted-gene r)
                     :let [cds (:rnai.predicted-gene/cds gh)]]
                 {:target_type (get-target-type gh)
                  :gene (pack-obj cds)})
               (for [h (:rnai/gene r)
                     :let [gene (:rnai.gene/gene h) ]]
                 {:target_type (get-target-type h)
                  :gene (pack-obj gene)})))))
   :description "gene targets of the RNAi experiment"})

(def widget
  {:name generic/name-field
   :historical_name historical-name
   :laboratory generic/laboratory
   :targets targets
   :taxonomy generic/taxonomy
   :remarks generic/remarks})
