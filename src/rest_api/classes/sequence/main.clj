(ns rest-api.classes.sequence.main
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.db.sequence :as seqdb]
    [pseudoace.utils :as pace-utils]
    [rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name gene-id]
 (let [db ((keyword db-name) wb-seq/sequence-dbs)]
   (wb-seq/get-features db gene-id)))

(defn get-segments [gene]
  (let  [species-name (->> gene :gene/species :species/id)
         g-species (generic-functions/xform-species-name species-name)
         sequence-database (seqdb/get-default-sequence-database g-species)]
    (if sequence-database
      (sequence-features sequence-database (:gene/id gene)))))

(defn longest-segment [segments]
  (first
    (sort-by #(- (:start %) (:end %)) segments)))

(defn get-longest-segment [gene]
  (let [segments (get-segments gene)]
    (if (seq segments)
      (longest-segment segments))))

(defn create-genomic-location-obj [start stop gene segment tracks gbrowse]
  (let [calc-browser-pos (fn [x-op x y mult-offset]
                            (if gbrowse
                              (->> (reduce - (sort-by - [x y]))
                                   (double)
                                   (* mult-offset)
                                   (int)
                                   (x-op x))
                              y))
        browser-start (calc-browser-pos - start stop 0.2)
        browser-stop (calc-browser-pos + stop start 0.5)
        label (str (:seqname segment) ":" start ".." stop)
        id (str (:seqname segment) ":" browser-start ".." browser-stop)]
    (pace-utils/vmap
      :class
      "genomic_location"

      :id
      id

      :label
      label

      :pos_string
      id

      :taxonomy
      (if-let [class (:gene/species gene)]
        (if-let [[_ genus species]
                  (re-matches #"^(.*)\s(.*)$"
                              (:species/id class))]
          (str/lower-case
            (str/join [(first genus) "_" species]))))

      :tracks
      tracks)))

(defn genomic-obj [object]
  (if-let [segment (get-longest-segment object)]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true))))
