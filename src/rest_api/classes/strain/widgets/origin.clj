(ns rest-api.classes.strain.widgets.origin
  (:require
    [clojure.string :as str]
    [rest-api.formatters.date :as dates]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn substrate [s]
  {:data (when-let [s (:strain/substrate s)]
         (str/capitalize (str/replace s #"_" " ")))
   :description "the substrate the strain was isolated on"})

(defn place [s]
  {:data (:strain/place s)
   :description "the place where the strain was isolated"})

(defn contact [s] ; this is not found in the database
  {:data (when-let [contacts (:strain/contact s)]
          (pack-obj (first contacts)))
   :description "the person who built the strain, or who to contact about it"})

(defn sampled-by [s]
  {:data (first (:strain/sampled-by s))
   :description "the person who sampled the strain"})

(defn date-received [s]
  {:data (when-let [d (first (:strain/cgc-received s))]
           (dates/format-date4 d))
   :description "date the strain was received at the CGC"})

(defn gps-coordinates [s]
  {:data (when-let [c (:strain/geolocation s)]
           {:longitude (:strain.geolocation/longitude c)
            :latitude (:strain.geolocation/latitude c)})
   :description "GPS coordinates of where the strain was isolated"})

(defn associated-organisms [s]
  {:data (when-let [as (:strain/associated-organisms s)]
           (map :species/id as))
   :description "the place where the strain was isolated"})

(defn made-by [s]
  {:data (when-let [ps (:strain/made-by s)]
           (pack-obj (first ps)))
   :description "the person who built the strain"})

(defn isolated-by [s]
  {:data (when-let [ps (:strain/isolated-by s)]
             (pack-obj (first ps)))
   :description "the person who isolated the strain"})

(defn life-stage [s]
  {:data (when-let [ls (:strain/life-stage s)]
           (pack-obj (first ls)))
   :description "the life stage the strain was in when isolated"})

(defn date-isolated [s]
  {:data (when-let [d (:strain/date s)]
          (dates/format-date4 d))
   :description "the date the strain was isolated"})

(defn landscape [s]
  {:data (when-let [ls (:strain/landscape s)]
         (str/capitalize (str/replace  (name ls) #"-" " ")))
   :description "the general landscape where the strain was isolated"})

(defn log-size-of-population [s]
  {:data (:strain/log-size-of-population s)
   :description "the log size of the population when isolated"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :substrate substrate
   :place place
   :contact contact
   :sampled_by sampled-by
   :date_received date-received
   :gps_coordinates gps-coordinates
   :associated_organisms associated-organisms
   :made_by made-by
   :isolated_by isolated-by
   :life_stage life-stage
   :date_isolated date-isolated
   :landscape landscape
   :log_size_of_population log-size-of-population})
