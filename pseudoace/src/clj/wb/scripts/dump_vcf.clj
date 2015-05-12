(ns wb.scripts.dump-vcf
  (:require [wb.locatables :as loc]
            [wb.sequence :as seq]
            [clojure.string :as str]
            [datomic.api :as d :refer (q entity)]))

;;
;; Not for "serious" use at the moment -- only dumps minimal data about substitutions.
;; intended for VEP testing.
;;

(defn dump-variations [db chr min max]
  (println "##fileformat=VCFv4.2")
  (println (str/join "\t" ["#CHROM" "POS" "ID" "REF" "ALT"
                           "QUAL" "FILTER" "INFO"]))
  (doseq [[v min max] (->>
                       (loc/features db
                                     "variation"
                                     (:db/id (entity db [:sequence/id chr]))
                                     min
                                     max)
                       (sort-by second))
          :let [v (entity db v)]]
    (cond 
      (:variation/substitution v)
      (let [sub (:variation/substitution v)]
        (println
         (str/join "\t"
          [(str/replace chr #"CHROMOSOME_" "")
           (inc min)
           (or (:variation/public-name v)
               (:variation/id v))
           (:variation.substitution/ref sub)
           (:variation.substitution/alt sub)
           "."
           "."
           "."])))

      (and (or (:variation/insertion v)
               (:variation/deletion v))
           (< (- max min) 2000))     ;; ignore big insertions.
      (let [ref (seq/region-sequence db chr (inc min) max)
            alt (if-let [ins (:variation/insertion v)]
                  (:variation.insertion/text ins)
                  (.substring ref (dec (count ref)) (count ref)))]
        (println
         (str/join "\t"
           [(str/replace chr #"CHROMOSOME_" "")
            (inc min)
            (or (:variation/public-name v)
                (:variation/id v))
            ref
            alt
            "."
            "."
            "."]))))))
        
       
       
    
