(ns rest-api.classes.gene.sequence
  (:require
    [rest-api.classes.gene.sequence :as seqfeat]
    [rest-api.db.sequence :as seqdb]
    [pseudoace.utils :as pace-utils]
    [rest-api.db.sequence :as wb-seq]))

(defn xform-species-name
  "Transforms a `species-name` from the WB database into
  a name used to look up connection configuration to a sequence db."
  [species]
  (let  [species-name-parts  (str/split species #" ")
         g  (str/lower-case  (ffirst species-name-parts))
         species  (second species-name-parts)]
    (str/join "_"  [g species])))

(defn sequence-features [db-name gene-id]
 (let [db ((keyword db-name) wb-seq/sequence-dbs)]
   (wb-seq/gene-features db gene-id)))

(defn get-segments  [gene]
  (let  [species-name  (->> gene :gene/species :species/id)
         g-species  (xform-species-name species-name)
         sequence-database  (seqdb/get-default-sequence-database g-species)]
    (if sequence-database
      (seqfeat/sequence-features sequence-database  (:gene/id gene)))))

(defn longest-segment  [segments]
  (first
    (sort-by #(-  (:start %)  (:end %)) segments)))

(defn get-longest-segment  [gene]
  (let  [segments  (get-segments gene)]
    (if  (seq segments)
      (longest-segment segments))))

(defn create-genomic-location-obj [id gene tracks]
  (pace-utils/vmap
    :class
    "genomic_location"

    :id
    id

    :label
    id

    :pos_string
    id

    :taxonomy
    (if-let  [class  (:gene/species gene)]
      (if-let  [[_ genus species]
                (re-matches #"^(.*)\s(.*)$"
                            (:species/id class))]
        (str/lower-case
          (str/join  [(first genus) "_" species]))))

    :tracks
    tracks))

