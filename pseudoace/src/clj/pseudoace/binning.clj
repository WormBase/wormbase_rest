(ns pseudoace.binning
  (:use datomic-schema.schema)
  (:require [datomic.api :as d]))

;; 
;; This is mostly obsolete, for wb248-imp2 onwards use wb.binning instead.
;;

(def locatable-bin-schema
  (concat
   (generate-schema [
     (schema locatable
       (fields
        [bin :long :indexed "UCSC/BAM-style bin number"]
        [xbin :long :indexed "Bottom 16 bits contains bin number.  Top bits contains parent entity ID"]))])

   [{:db/id          #db/id[:db.part/tx]
     :db/txInstant   #inst "1970-01-01T00:00:01"}]))

(defn reg2bin [beg end]
  (let [end (dec end)]
    (cond
     (= (bit-shift-right beg 14) (bit-shift-right end 14))
     (+ (/ (dec (bit-shift-left 1 15)) 7) (bit-shift-right beg 14))

     (= (bit-shift-right beg 17) (bit-shift-right end 17))
     (+ (/ (dec (bit-shift-left 1 12)) 7) (bit-shift-right beg 17))

     (= (bit-shift-right beg 20) (bit-shift-right end 20))
     (+ (/ (dec (bit-shift-left 1 9)) 7) (bit-shift-right beg 20))

     (= (bit-shift-right beg 23) (bit-shift-right end 23))
     (+ (/ (dec (bit-shift-left 1 6)) 7) (bit-shift-right beg 23))

     (= (bit-shift-right beg 26) (bit-shift-right end 26))
     (+ (/ (dec (bit-shift-left 1 3)) 7) (bit-shift-right beg 26))

     :default
     0)))

(defn reg2bins [beg end]
  (concat
   [0]
   (range (+ 1 (bit-shift-right beg 26)) (+ 1 1 (bit-shift-right end 26)))
   (range (+ 9 (bit-shift-right beg 23)) (+ 9 1 (bit-shift-right end 23)))
   (range (+ 73 (bit-shift-right beg 20)) (+ 73 1 (bit-shift-right end 20)))
   (range (+ 585 (bit-shift-right beg 17)) (+ 585 1 (bit-shift-right end 17)))
   (range (+ 4681 (bit-shift-right beg 14)) (+ 4681 1 (bit-shift-right end 14)))))

(defn xbin [id x]
  (bit-or x (bit-shift-left (bit-and id 0x3ffffffffff) 16)))

(defn add-bins-to-locatables [con]
  (loop [db
            (d/db con)
         [targets & more]
            (partition-all 1000 (d/q '[:find [?l ...]
                                       :where [?l :locatable/parent _]]
                                     db))]
    (if (seq targets)
      (recur
       (:db-after
        @(d/transact-async
          con
          (->> (d/q '[:find ?l ?parent ?min ?max
                      :in $ [?l ...]
                      :where [?l :locatable/parent ?parent]
                             [?l :locatable/min ?min]
                             [?l :locatable/max ?max]]
                    db targets)
               (map
                (fn [[l parent min max]]
                  (let [bin (reg2bin min max)]
                    {:db/id l
                     :locatable/bin bin
                     :locatable/xbin (xbin parent bin)}))))))
       more))))
