(ns wb.update-couch
  (:require [clojure.string :as str]
            [clojure.contrib.seq-utils :refer (indexed)]
            [com.ashafa.clutch :as c]))

(defn get-genes [db]
  (mapv :value (c/get-view db "smallace" "gene")))

(defn get-phenos [db]
  (mapv :value (c/get-view db "smallace" "phenotype")))

(defn update-gene-descriptions [db n]
  (doseq [[i g] (->> (get-genes db)
                     (vec)
                     (shuffle)
                     (take n)
                     (indexed))]
    (c/put-document db (assoc g :desc {:concise (str "iteration " i)
                                       :evidence_automatic "wb.update-couch/update-gene-descriptions"}))))

;
; The following could be a little faster if we skipped the
; get-document on genes.  However, we'd have to either ensure that
; each gene was only updated once, or store the updated gene in
; memory.  Moreover, the script could then easily fail if anyone else
; was writing genes concurrently.
;

(defn make-random-rnais [db prefix n]
  (let [genes  (mapv :_id (get-genes db))
        phenos (mapv :_id (get-phenos db))]
    (doseq [i (range n)]
      (let [gid (rand-nth genes)
            pid (rand-nth phenos)
            rid (str prefix i)
            gene (c/get-document db gid)]   ; re-fetch gene in case it's already been updated
        (c/put-document db
          {:_id           rid
           :expt_delivery "feeding"
           :phenotype     [pid]})
        (c/put-document db
          (assoc gene 
            :rnai
            (conj (or (:rnai gene) [])
              {:rnai rid
               :evidence_automatic "wb.update_couch/make-random-rnais"})))))))
            
