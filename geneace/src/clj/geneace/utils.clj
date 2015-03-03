(ns geneace.utils)

(defn vmap
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values."
  [& args] 
  (into {} (for [[k v] (partition 2 args) 
                 :when (not (nil? v))] 
             [k v])))

(defn vassoc
  "Associate `value`s with `key`s in m, ignoring any keys associated with
  nil values."
  ([m key value]
     (if value
       (assoc m key value)
       m))
  ([m k v & kvs]
     (when-not (even? (count kvs))
       (throw (IllegalArgumentException. "vassoc expects an even number of arguments after map.")))
     (reduce (fn [m [k v]] (vassoc m k v))
             (vassoc m k v)
             (partition 2 kvs))))

(defn parse-int [i]
  (when i
    (Integer/parseInt i)))

(defn parse-double [i]
  (when i
    (Double/parseDouble i)))

(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

(defn those
  [& items]
  (filter identity items))

;;
;; From Clojure 1.7
;;

(defn update
  "'Updates' a value in an associative structure, where k is a
  key and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  structure.  If the key does not exist, nil is passed as the old value."
  ([m k f]
   (assoc m k (f (get m k))))
  ([m k f x]
   (assoc m k (f (get m k) x)))
  ([m k f x y]
   (assoc m k (f (get m k) x y)))
  ([m k f x y z]
   (assoc m k (f (get m k) x y z)))
  ([m k f x y z & more]
   (assoc m k (apply f (get m k) x y z more))))
