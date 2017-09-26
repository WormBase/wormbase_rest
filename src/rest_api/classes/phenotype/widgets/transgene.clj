(ns rest-api.classes.phenotype.widgets.transgene
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn transgene-info [t obs]
  (when-let [holders (if obs
                       (:transgene.phenotype/_phenotype t)
                       (:transgene.phenotype-not-observed/_phenotype t))]
    (for [holder holders
          :let [transgene (if obs
                       (:transgene/_phenotype holder)
                       (:transgene/_phenotype-not-observed holder))]]
      {:transgene (pack-obj transgene)
       :remark nil
       :caused_by nil
       })))

(defn transgene [t]
  {:data (transgene-info t true)
   :description (str "The name and WormBase internal ID of " (:db/id t))})

(defn transgene-not [t]
  {:data (transgene-info t false)
   :description (str "The name and WormBase internal ID of " (:db/id t))})

(def widget
  {:transgene transgene
   :transgene_not transgene-not
   :name generic/name-field})
