(ns rest-api.classes.sequence.core
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.db.sequence :as seqdb]
    [pseudoace.utils :as pace-utils]
    [rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name id role]
  (let [db ((keyword db-name) wb-seq/sequence-dbs)]
    (case role
      "variation" (wb-seq/get-features-by-attribute db id)
      "cds" (wb-seq/sequence-features-where-type db id "CDS%")
      "pcr-product" (wb-seq/sequence-features-where-type db id "PCR_product%")
      (wb-seq/get-features db id))))

(defn get-g-species [object role]
  (when-let [species-name (:species/id
                             (or
                               ((keyword role "species") object)
                               (or
                                 (:clone/species
                                   (first
                                     (:pcr-product/clone object)))
                                 (:transcript/species
                                   (first
                                     (:transcript/_corresponding-pcr-product object))))))]
    (generic-functions/xform-species-name species-name)))

(defn get-segments [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
	role (namespace id-kw)]
    (let [g-species (get-g-species object role)
          sequence-database (seqdb/get-default-sequence-database g-species)]
     (when sequence-database
	(sequence-features sequence-database (id-kw object) role)))))

(defn longest-segment [segments]
  (first
    (sort-by #(- (:start %) (:end %)) segments)))

(defn get-longest-segment [object]
  (let [segments (get-segments object)]
    (if (seq segments)
      (longest-segment segments))))

(defn create-genomic-location-obj [start stop object segment tracks gbrowse img]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        calc-browser-pos (fn [x-op x y mult-offset]
                            (if gbrowse
                              (->> (reduce - (sort-by - [x y]))
                                   (double)
                                   (* mult-offset)
                                   (int)
                                   (x-op x))
                              y))
        browser-start (calc-browser-pos - start stop 0.2)
        browser-stop (calc-browser-pos + stop start 0.5)
        id (str (:seqname segment) ":" browser-start ".." browser-stop)
        label (if (= img true)
                   id
                 (str (:seqname segment) ":" start ".." stop))]
    (pace-utils/vmap
      :class "genomic_location"
      :id id
      :label label
      :pos_string id
      :seqment (:seqname segment)
      :start start
      :stop stop
      :taxonomy (get-g-species object role)
      :tracks tracks)))

(defn genomic-obj [object]
  (when-let [segment (get-longest-segment object)]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true true))))

(defn genomic-obj-position [object]
  (when-let [segment (get-longest-segment object)]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true false))))
