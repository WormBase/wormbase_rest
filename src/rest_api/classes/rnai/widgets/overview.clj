(ns rest-api.classes.rnai.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn historical-name [r]
  {:data (:rnai/history-name r)
   :desciprition "historical name of the rnai"})

(defn targets [r]
  {:data (not-empty
           (vals
             (into
               {}
               ((comp vec flatten vector)
                (when-let [gene (:rnai.gene/gene (first (:rnai/gene r)))]
                  {(:gene/id gene) (pack-obj gene)})
                (when-let [ghs (:rnai/predicted-gene r)]
                  (for [gh ghs :let [cds (:rnai.predicted-gene/cds gh)]]
                    {(:cds/id cds) (pack-obj cds)}))))))
   :description "gene targets of the RNAi experiment"})

(def widget
  {:name generic/name-field
   :historical_name historical-name
   :laboratory generic/laboratory
   :targets targets
   :taxonomy generic/taxonomy
   :remarks generic/remarks})
