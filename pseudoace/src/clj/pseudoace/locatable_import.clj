(ns pseudoace.locatable-import
  (:use pseudoace.utils
        pseudoace.binning)
  (:require [datomic.api :as d :refer (q entity touch tempid)]
            [pseudoace.ts-import :refer [merge-logs
                                         select-ts
                                         take-ts
                                         drop-ts
                                         logs-to-dir]]))

(defn- log-features [fd feature-lines]
  (when (= (:locatable/strand fd) :locatable.strand/negative)
    (except "Don't support negative-strand ?Feature_data"))
  (let [parent (:locatable/parent fd)
        offset (or (:locatable/min fd) 0)]
    (reduce
     (fn [log [[method start-s end-s score note :as core] [lines]]]
       (let [tid   [:importer/temp (d/squuid)]
             start (parse-int start-s)
             end   (parse-int end-s)
             min   (+ offset -1 (min start end))
             max   (+ offset (max start end))
             bin   (reg2bin min max)]
         (update log (second (:timestamps (meta core)))
            conj-if
            [:db/add tid :locatable/parent (:db/id parent)]
            [:db/add tid :locatable/min min]
            [:db/add tid :locatable/max max]
            [:db/add tid :locatable/strand (if (> start end)
                                             :locatable.strand/negative
                                             :locatable.strand/positive)]
            [:db/add tid :locatable/method [:method/id method]]
            (if score
              [:db/add tid :locatable/score (parse-double score)])
            (if note
              [:db/add tid :locatable/note note])
            [:db/add tid :locatable/bin bin]
            [:db/add tid :locatable/xbin (xbin (:db/id parent) bin)])))
     {}
     (group-by (partial take-ts 5) feature-lines))))

(defn- log-splice-confirmations [fd splice-lines]
  (when (= (:locatable/strand fd) :locatable.strand/negative)
    (except "Don't support negative-strand ?Feature_data"))
  (let [parent (:db/id (:locatable/parent fd))
        offset (or (:locatable/min fd) 0)]
    (reduce
     (fn [log [start-s end-s confirm-type confirm confirm-x :as line]]
       (let [tid   [:importer/temp (d/squuid)]
             start (parse-int start-s)
             end   (parse-int end-s)
             min   (+ offset -1 (min start end))
             max   (+ offset (max start end))
             bin   (reg2bin min max)]
         (update log (first (drop 2 (:timestamps (meta line))))
           (fn [old]
             (into (or old [])
               (concat
                 [[:db/add tid :locatable/parent parent]
                  [:db/add tid :locatable/min min]
                  [:db/add tid :locatable/max max]
                  [:db/add tid :locatable/strand (if (> start end)
                                                   :locatable.strand/negative
                                                   :locatable.strand/positive)]
                  [:db/add tid :locatable/bin bin]
                  [:db/add tid :locatable/xbin (xbin parent bin)]]
                 
                 (case confirm-type
                   "cDNA"
                   [[:db/add tid :splice-confirm/cdna [:sequence/id confirm]]]

                   "EST"
                   [[:db/add tid :splice-confirm/est [:sequence/id confirm]]]

                   "OST"
                   [[:db/add tid :splice-confirm/ost [:sequence/id confirm]]]

                   "RST"
                   [[:db/add tid :splice-confirm/rst [:sequence/id confirm]]]

                   "RNASeq"
                   (let [rtid [:importer/temp (d/squuid)]]
                     [[:db/add tid :splice-confirm/rnaseq rtid]
                      [:db/add rtid :splice-confirm.rnaseq/analysis [:analysis/id confirm]]
                      [:db/add rtid :splice-confirm.rnaseq/count (parse-int confirm-x)]])

                   "Mass_spec"
                   [[:db/add tid :splice-confirm/mass-spec [:mass-spec-peptide/id confirm]]]

                   "Homology"
                   [[:db/add tid :splice-confirm/homology confirm]]

                   "UTR"
                   [[:db/add tid :splice-confirm/utr [:sequence/id confirm]]]

                   "False"
                   [[:db/add tid :splice-confirm/false-splice [:sequence/id confirm]]]

                   "Inconsistent"
                   [[:db/add tid :splice-confirm/inconsistent [:sequence/id confirm]]])))))))
     {}
     splice-lines)))

(defmulti log-locatables (fn [_ obj] (:class obj)))

(defmethod log-locatables "Feature_data" [db obj]
  (if-let [fd (entity db [:feature-data/id (:id obj)])]
    (if (= (:locatable/strand fd)
           :locatable.strand/negative)
      (println "Skipping" (:id fd) "because negative strand")
      (merge-logs
       (log-features fd (select-ts obj ["Feature"]))
       (log-splice-confirmations fd (select-ts obj ["Splices" "Confirmed_intron"]))
       ;; Predicted_5 and Predicted_3 don't seem to be used
       ))
    (println "Couldn't find ?Feature_data " (:id obj))))

(defmethod log-locatables :default [_ _]
  nil)

(defn lobjs->log [db objs]
  (reduce merge-logs (map (partial log-locatables db) objs)))

(defn split-locatables-to-dir
  [db objs dir]
  (logs-to-dir (lobjs->log db objs) dir))
  
  
