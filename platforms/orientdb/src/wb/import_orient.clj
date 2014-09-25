(ns wb.import-orient
  (:use acetyl.parser)
  (:require [clojure.core.match :refer (match)]))

(defn vmap
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values."
  [& args] 
  (into {} (for [[k v] (partition 2 args) 
                 :when (not (nil? v))] 
             [k v])))

(defn conj-into 
  "Update a map with key-value pairs, conj-ing new values onto sequences"
  [m kvs]
  (reduce (fn [m [k v]]
            (if k
              (if-let [old (m k)]
                (assoc m k (conj old v))
                (assoc m k [v]))
              m))
          m kvs))

(defmulti ace-to-orient :class)

(defmethod ace-to-orient "Paper"
  [obj]
  (let [authors         (select obj ["Author"])
        [[ref_title]]   (select obj ["Reference" "Title"])
        [[ref_journal]] (select obj ["Reference" "Journal"])
        [[ref_volume]]  (select obj ["Reference" "Volume"])
        [[ref_page]]    (select obj ["Reference" "Page"])
        [[brief_cite]]  (select obj ["Brief_citation"])
        [[abstract]]    (select obj ["Abstract"])]
    (vmap :id         (:id obj)
          :authors     (map first authors)
          :ref_title   ref_title
          :ref_journal ref_journal
          :ref_volume  ref_volume
          :ref_page    ref_page
          :brief_citation  (unescape brief_cite))))
          ; :abstract    (@longtext-cache abstract)
