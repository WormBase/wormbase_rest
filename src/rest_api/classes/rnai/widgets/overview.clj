(ns rest-api.classes.rnai.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn historical-name [r]
  {:data (:rnai/history-name r)
   :keys (keys r )
   :desciprition "historical name of the rnai"})

(defn laboratory [r]
  {:data (when-let [labs (:rnai/laboratory r)]
           (for [lab labs]
             {:laboratory (pack-obj lab)
              :representative (when-let [reps (:laboratory/representative lab)]
                                (for [rep reps] (pack-obj rep)))}))
   :description "the laboratory where the RNAi was isolated, created, or named"})

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
   :laboratory laboratory
   :targets targets
   :taxonomy generic/taxonomy
   :remarks generic/remarks})
