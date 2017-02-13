(ns rest-api.classes.gene.widgets.location
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.gene.sequence :as sequence-fns]
    [rest-api.classes.gene.generic :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))


(defn genetic-position [gene]
  {:data nil
   :description (str "Genetic position of Gene:" (:gene/id gene))})

(defn tracks [gene]
  {:data (if (:gene/corresponding-transposon)
           ["TRANSPOSONS"
            "TRANSPOSON_GENES"]
           ["GENES"
            "VARIATIONS_CLASSICAL_ALLELES"
            "CLONES"])
   :description "tracks displayed in GBrowse"})

(defn- genomic-obj [gene]
 (if-let [segment (segment-fns/get-longest-segment gene)]
           (let [[start, stop] (->> segment
                                     ((juxt :start :end))
                                     (sort-by +))
                 id (str  (:seqname segment) ":" start ".." stop)]
             (create-genomic-location id gene nil))))

(defn genomic-position [gene]
  {:data (genomic-obj gene)
   :description "The genomic location of the sequence"})

(defn genomic-image [gene]
  {:data (genomic-obj gene)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic-position genetic-position
     :tracks tracks
     :genomic-position genetic-position
     :genomic-image genomic-image})
