(ns pseudoace.locatable-import
  (:use pseudoace.utils
        wb.binning)
  (:require [datomic.api :as d :refer (q entity touch tempid)]
            [pseudoace.ts-import :refer [merge-logs
                                         select-ts
                                         take-ts
                                         drop-ts
                                         logs-to-dir]]))

;;
;; TODO:
;;   - Reinstate strand and missing-parent checks once acedb is fixed.
;;   - More alignment reconstruction?
;;

(defn- bin-me-maybe [this [attr id] min max]
  (if (= attr :sequence/id)
    [:db/add this :locatable/murmur-bin (bin id min max)]))

(defn- log-features [feature-lines parent offset]
  (let [offset (or offset 0)]
    (reduce
     (fn [log [[method start-s end-s score note :as core] [lines]]]
       (let [tid   [:importer/temp (str (d/squuid))]
             start (parse-int start-s)
             end   (parse-int end-s)
             min   (+ offset -1 (min start end))
             max   (+ offset (max start end))]
         (update log (second (:timestamps (meta core)))
            conj-if
            [:db/add tid :locatable/parent parent]
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
            (bin-me-maybe tid parent min max))))
     {}
     (group-by (partial take-ts 5) feature-lines))))

(defn- log-splice-confirmations [splice-lines parent offset]
  (let [offset (or offset 0)]
    (reduce
     (fn [log [start-s end-s confirm-type confirm confirm-x :as line]]
       (if confirm
         (let [tid   [:importer/temp (str (d/squuid))]
               start (parse-int start-s)
               end   (parse-int end-s)
               min   (+ offset -1 (min start end))
               max   (+ offset (max start end))]
           (update log (first (drop 2 (:timestamps (meta line))))
             (fn [old]
               (into (or old [])
                     (concat
                      (those
                       [:db/add tid :locatable/parent parent]
                       [:db/add tid :locatable/min min]
                       [:db/add tid :locatable/max max]
                       [:db/add tid :locatable/strand (if (> start end)
                                                        :locatable.strand/negative
                                                   :locatable.strand/positive)]
                       (bin-me-maybe tid parent min max))
                      
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
                        (let [rtid [:importer/temp (str (d/squuid))]]
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
                        [[:db/add tid :splice-confirm/inconsistent [:sequence/id confirm]]]))))))
         log))   ;; There are a certain number of these with missing data, which we'll skip.
     {}
     splice-lines)))

;;
;; For version 1, we're *not* going to try collapsing Homol lines into single gapped alignments,
;; since it seems that the blastp importer is only recording one line per sub-hit...
;;
;; this probably needs revisiting if we use this code for things other than ?Protein homols
;;

(defn- log-homols [homol-lines parent offset protein?]
 (let [offset (or offset 0)]
  (reduce
   (fn [log [[type target method score-s parent-start-s parent-end-s target-start-s target-end-s :as line] lines ]] 
     (let [tid   [:importer/temp (str (d/squuid))]
           start (parse-int parent-start-s)
           end   (parse-int parent-end-s)
           min   (if start (+ offset -1 start))
           max   (if end (+ offset end))]
           (update log (first (drop 3 (:timestamps (meta line))))
             (fn [old]
               (into (or old [])
                (let [base (those
                             [:db/add tid :locatable/parent parent]
                             (and (some? method) [:db/add tid :locatable/method [:method/id method]])
                             (and (some? min) [:db/add tid :locatable/min min])
                             (and (some? max) [:db/add tid :locatable/max max])
                             (and (not-any? nil? [start end]) (if-not protein?
                               [:db/add tid :locatable/strand (if (> start end)
                                                                :locatable.strand/negative
                                                                :locatable.strand/positive)]))
                             (and (not-any? nil? [min max]) (bin-me-maybe tid parent min max))
                             (and (some? target-start-s) [:db/add tid :homology/min (dec (parse-int target-start-s))])
                             (and (some? target-end-s) [:db/add tid :homology/max      (parse-int target-end-s)   ])
                             (and (some? score-s) [:db/add tid :locatable/score (parse-double score-s)]))]
                   (case type
                     "DNA_homol"
                     (if protein?
                       (except "Don't support protein->DNA homols")
                       (conj base [:db/add tid :homology/dna [:sequence/id target]]))
    
                     "Pep_homol"
                     (conj base [:db/add tid :homology/protein [:protein/id target]])
                     
                     "Structure_homol"
                     (conj base [:db/add tid :homology/structure [:structure-data/id target]])
    
                     "Motif_homol"
                     (conj base [:db/add tid :homology/motif [:motif/id target]])
    
                     "RNAi_homol"
                     (conj base [:db/add tid :homology/rnai [:rnai/id target]])
    
                     "Oligo_set_homol"
                     (conj base [:db/add tid :homology/oligo-set [:oligo-set/id target]])
    
                     "Expr_homol"
                     (conj base [:db/add tid :homology/expr [:expr-pattern/id target]])
    
                     "MSPeptide_homol"
                     (conj base [:db/add tid :homology/ms-peptide [:mass-spec-peptide/id target]])
    
                     "SAGE_homol"
                     (conj base [:db/add tid :homology/sage [:sage-tag/id target]])
    
                     "Homol_homol"   ;; silently ignore
                     nil
    
                     ;; default
                     (except "Don't understand homology type " type))))))))
       {}
   (group-by (partial take-ts 8) homol-lines))))
              

(defmulti log-locatables (fn [_ obj] (:class obj)))

(defmethod log-locatables "Feature_data" [db obj]
  (if-let [fd (entity db [:feature-data/id (:id obj)])]
   (let [parent [:sequence/id (:sequence/id (:locatable/parent fd))]]
    (if (= (:locatable/strand fd)
           :locatable.strand/negative)
      (println "Skipping" (:id obj) "because negative strand")
      (merge-logs
       (log-features
        (select-ts obj ["Feature"])
        parent
        (:locatable/min fd))
       (log-splice-confirmations
        (select-ts obj ["Splices" "Confirmed_intron"])
        parent
        (:locatable/min fd))
       ;; Predicted_5 and Predicted_3 don't seem to be used
       )))
    (println "Couldn't find ?Feature_data " (:id obj))))

(defmethod log-locatables "Protein" [db obj]
  (let [parent [:protein/id (:id obj)]]
    (merge-logs
     (log-features (select-ts obj ["Feature"]) parent nil)
     (log-homols   (select-ts obj ["Homol"])   parent nil true))))

(defmethod log-locatables "Homol_data" [db obj]
  (if-let [homol (entity db [:homol-data/id (:id obj)])]
   (let [parent [:sequence/id (:sequence/id (:locatable/parent homol))]]
    (if (= (:locatable/strand homol)
           :locatable.strand/negative)
      (println "Skipping" (:id obj) "because negative strand")      
      (log-homols
       (select-ts obj ["Homol"])
       parent
       (:locatable/min homol)
       false)))))

(defmethod log-locatables "Sequence" [db obj]
  (let [parent [:sequence/id (:id obj)]]
    (log-splice-confirmations
     (select-ts obj ["Splices" "Confirmed_intron"])
     parent
     nil)))

(defmethod log-locatables :default [_ _]
  nil)

(defn lobjs->log [db objs]
  (reduce merge-logs (map (partial log-locatables db) objs)))

(defn split-locatables-to-dir
  [db objs dir]
  (logs-to-dir (lobjs->log db objs) dir))
