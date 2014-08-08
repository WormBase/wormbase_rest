(ns wb.utils)

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
