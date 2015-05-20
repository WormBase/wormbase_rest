(ns wb.scripts.dump-gtf
  (:use pseudoace.utils)
  (:require [wb.locatables :as loc]
            [wb.sequence :as seq]
            [clojure.string :as str]
            [datomic.api :as d :refer (q entity)]))

;;
;; This namespace is intended mainly for producing GTF files for use with Ensembl VEP
;; See http://www.ensembl.org/info/docs/tools/vep/script/vep_cache.html#gtf for details.
;; A GFF3 dumper may be more appropriate for general use.
;;

(defn write-gff2-record
  [{:keys [chr source type start end strand frame score attrs]}]
  (println
   (str/join "\t"
     (conj-if
      [chr
       source
       type
       start
       end
       (or score  ".")
       (or strand ".")
       (or frame  ".")]
      (if attrs
        (str/join " "
          (for [[k v] attrs]
            (format "%s \"%s\";" (name k) v))))))))
                         

(defn dump-gtf [db chr]
  (let [sequence (entity db [:sequence/id chr])
        seq-name (str/replace (:sequence/id sequence) #"CHROMOSOME_" "")
        chr-len  (seq/seq-length sequence)
        features     (->>
                      (loc/features db "transcript" (:db/id sequence) 0 chr-len)
                      (sort-by second))]
    (doseq [[tid tmin tmax] features]
      (let [transcript (entity db tid)
            gene       (->> (:gene.corresponding-transcript/_transcript transcript)
                            (first)
                            (:gene/_corresponding-transcript))
            
            strand     (case (:locatable/strand transcript)
                         :locatable.strand/positive "+"
                         :locatable.strand/negative "-")
            cds        (->> (:transcript/corresponding-cds transcript)
                            (:transcript.corresponding-cds/cds))
            [_ cds-min cds-max]  (if cds
                                   (loc/root-segment cds))
            method     (if cds
                         "protein_coding"
                         (:method/id (:transcript/method transcript)))]
                              
        ;;
        ;; This would look a bit nicer as another mapcat, but need
        ;; to use reduce so that phase information can be threaded
        ;; from exon to exon.
        ;;
        
        (->>
         (reduce
          (fn [{:keys [features last-phase]}
               [exon-number
                {emin :transcript.source-exons/min
                 emax :transcript.source-exons/max}]]
            (let [[emin emax] (case (:locatable/strand transcript)
                                :locatable.strand/positive
                                [(+ tmin emin -1)
                                 (+ tmin emax)]
                                
                                :locatable.strand/negative
                                [(- tmax emax)
                                 (- tmax emin -1)])
                  coding-min (if cds-min
                               (max emin cds-min)
                               (inc emax))
                  coding-max (if cds-max
                               (min emax cds-max)
                               (dec emin))]
              ;; biotype (e.g. "protein_coding") must go in the source field if we
              ;; want VEP to do useful stuff with protein-coding genes.
              {:features
               (conj-if features
                        {:chr    seq-name
                         :source method
                         :type   "exon"
                         :start  (inc emin)
                         :end    emax
                         :strand strand
                         :attrs  {:transcript_biotype method
                                  :transcript_id      (:transcript/id transcript)
                                  :gene_id            (:gene/id gene)
                                  :exon_number        exon-number}}
                    
                        (if (< coding-min coding-max)
                          {:chr    seq-name
                           :source method
                           :type   "CDS"
                           :start  (inc coding-min)
                           :end    coding-max
                           :strand strand
                           :frame  (case last-phase
                                     1 2
                                     2 1
                                     0) ;; default.
                           :attrs  {:transcript_biotype method
                                    :gene_id            (:gene/id gene)
                                    :transcript_id      (:transcript/id transcript)
                                    :exon_number        exon-number}}))
               
               :last-phase (if (< coding-min coding-max)
                             (rem (+ (or last-phase 0)
                                     (- coding-max coding-min))
                                  3))}))
          {:features []}
          (map vector
               (iterate inc 1)
               (->> (:transcript/source-exons transcript)
                    (sort-by :transcript.source-exons/min))))
         (:features)
         (map write-gff2-record)
         (doall))))))
                             

             
                                            
            
        
    
    
