(ns rest-api.classes.rearrangement.widgets.isolation
  (:require
    [rest-api.formatters.date :as date]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn source [r]
  {:data (when-let [s (:rearrangement/source-rearrangment r)]
           (pack-obj s))
   :description "Source rearrangement for this rearrangement"})

(defn mutagen [r]
  {:data (:rearrangement.mutagen/name
           (:rearrangement/mutagen r))
   :description "Mutagen associated with the Rearrangement"})

(defn date [r]
  {:data (when-let [d (:rearrangement/date r)]
           (date/format-date6 d))
   :description "Date associated with the Rearrangement"})

(defn author [r]
  {:data (some->> (:rearrangement/author r)
                  (map pack-obj))
   :description "Author associated with the Rearrangement"})

(defn derived [r]
  {:data (some->> (:rearrangement/_source-rearrangement r)
                  (map pack-obj))
   :description "Rearrangements derived from this rearrangement"})

(def widget
  {:name generic/name-field
   :source source
   :laboratory generic/laboratory
   :mutagen mutagen
   :date date
   :author author
   :derived derived})
