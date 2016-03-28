(ns wb.binning
  (:import clojure.lang.Murmur3))

;;
;; Raw binning functions based on BAM spec.
;;

(defn- reg2bin [beg end]
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

(defn- reg2bins [beg end]
  (concat
   [0]
   (range (+ 1 (bit-shift-right beg 26)) (+ 1 1 (bit-shift-right end 26)))
   (range (+ 9 (bit-shift-right beg 23)) (+ 9 1 (bit-shift-right end 23)))
   (range (+ 73 (bit-shift-right beg 20)) (+ 73 1 (bit-shift-right end 20)))
   (range (+ 585 (bit-shift-right beg 17)) (+ 585 1 (bit-shift-right end 17)))
   (range (+ 4681 (bit-shift-right beg 14)) (+ 4681 1 (bit-shift-right end 14)))))

;;
;; Public API.
;;

(defn bin
  "Return a WB bin number for a feature attached to `[:sequence/id seq]`
   with coordinates `min` and `max`."
  [^String seq min max]
  (bit-or
   (bit-shift-left (Murmur3/hashUnencodedChars seq) 20)
   (reg2bin min max)))

(defn bins
  "Return WB bin numbers for features overlapping the region from `min` to `max`
   attached to `[:sequence/id seq]`."
  [^String seq min max]
  (mapv (partial bit-or (bit-shift-left (Murmur3/hashUnencodedChars seq) 20))
        (reg2bins min max)))

