(ns wb.utils)

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

(defn parse-int [^String s]
  (Integer/parseInt s))

(defn parse-float [^String s]
  (Float/parseFloat s))

(defn parse-double [^String s]
  (Double/parseDouble s))

(defn group-by-prefix
  "Group the n-suffixes of coll by their n-prefixes"
  [n coll]
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (take n x)]
        (assoc! ret k (conj (get ret k []) (drop n x)))))
    (transient {}) coll)))

(defn except
  "Concatenate args as if with `str` then throw an exception"
  [& args]
  (throw (Exception. (apply str args))))
