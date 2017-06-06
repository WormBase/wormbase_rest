(ns rest-api.classes.wbprocess.widgets.overview
  (:require
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn other-name [w]
  {:data (first (:wbprocess/other-name w))
   :description "Term alias"})

(defn life-stage [w]
  {:data (when-let [ls (:wbprocess/life-stage w)]
           (for [l ls]
             {:text (pack-obj (:wbprocess.life-stage/life-stage l))
              :evidence (obj/get-evidence l)}))
   :description "Life stages associated with this topic"})

(defn related-process [w]
  {:data (pace-utils/vmap
           "Specialization of"
           (when-let [ds (:wbprocess/specialisation-of w)]
             (for [d ds] (pack-obj d)))

           "Generalization of"
           (when-let [ds (:wbprocess/_specialisation-of w)]
             (for [d ds] (pack-obj d))))
   :description "Topics related to this record"})

(def widget
  {:name generic/name-field
   :summary generic/summary
   :other_name other-name
   :life_stage life-stage
   :historical_gene generic/historical-gene
   :related_process related-process})
