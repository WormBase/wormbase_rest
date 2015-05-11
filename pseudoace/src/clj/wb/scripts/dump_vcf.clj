(ns wb.scripts.dump-vcf
  (:require [web.locatable-api :as loc]
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
    (when-let [sub (:variation/substitution v)]
      (println
       (str/join "\t"
        [chr
         (inc min)
         (:variation/id v)
         (:variation.substitution/ref sub)
         (:variation.substitution/alt sub)
         "."
         "."
         "."])))))
       
       
    
