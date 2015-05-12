(ns wb.sequence
  (:use wb.locatables
        pseudoace.utils)
  (:require [datomic.api :as d :refer (q entity)]
            [clojure.string :as str]))

  
(defn seq-length [seq]
  (or
   (:sequence.dna/length (:sequence/dna seq))

   (count (:dna/sequence (:sequence.dna/dna (:sequence/dna seq))))

   (q '[:find (max ?ss-end) .
        :in $ ?seq
        :where [?seq :sequence/subsequence ?ss]
               [?ss  :sequence.subsequence/end ?ss-end]]
      (d/entity-db seq) (:db/id seq))))

(defn- run-of-ns [n]
  (str/join (repeat n \n)))

(defn- region-sequence* [sequence seq-min seq-max]
  (if-let [dna (->> (:sequence/dna sequence)
                    (:sequence.dna/dna)
                    (:dna/sequence))]
    (.substring dna (dec seq-min) seq-max)
    (str/join
      (loop [seq-min         seq-min
             segs            []
             [subseq & rest] (->> (:sequence/subsequence sequence)
                                  (sort-by :sequence.subsequence/start))]
        (cond
          (and subseq
               (<= seq-min seq-max)
               (>= seq-max (:sequence.subsequence/start subseq)))
          (let [{ss      :sequence.subsequence/sequence
                 start   :sequence.subsequence/start
                 end     :sequence.subsequence/end}
                subseq]
            (recur
             (max seq-min (inc end))
             (conj-if
              segs
              (if (< seq-min start)
                (run-of-ns (- start seq-min)))
              (if (and (<= seq-min end) (>= seq-max start))
                (region-sequence* ss (- seq-min start -1) (- (min seq-max end) start -1))))
             rest))

          :default
          (conj-if segs (if (< seq-min seq-max)
                          (run-of-ns (- seq-max seq-min -1)))))))))

(defn region-sequence [db seq-name min max]
  (apply region-sequence* (root-segment (entity db [:sequence/id seq-name]) min max)))

