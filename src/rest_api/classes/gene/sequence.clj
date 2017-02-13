(ns rest-api.classes.gene.sequence
  (:require
    [clojure.string :as str]
    [rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name id]
  (let [db ((keyword db-name) wb-seq/sequence-dbs)]
    (wb-seq/gene-features db id)))

(defn sequence-features-where-type [db-name id method]
  (let [db ((keyword db-name) wb-seq/sequence-dbs)]
    (wb-seq/sequence-features-where-type db id method)))

(defn xform-species-name
  "Transforms a `species-name` from the WB database into
  a name used to look up connection configuration to a sequence db."
  [species]
  (let  [species-name-parts  (str/split species #" ")
         g  (str/lower-case  (ffirst species-name-parts))
         species  (second species-name-parts)]
    (str/join "_"  [g species])))
