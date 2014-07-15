(ns wb.update-couch
  (:require [clojure.string :as str]
            [clojure.contrib.seq-utils :refer (indexed)]
            [com.ashafa.clutch :as c]))

(defn get-genes [db]
  (mapv :value (c/get-view db "smallace" "gene")))

(defn update-gene-descriptions [db n]
  (doseq [[i g] (->> (get-genes db)
                     (vec)
                     (shuffle)
                     (take n)
                     (indexed))]
    (c/put-document db (assoc g :desc {:concise (str "iteration " i)
                                       :evidence_automatic "wb.update-couch/update-gene-descriptions"}))))

