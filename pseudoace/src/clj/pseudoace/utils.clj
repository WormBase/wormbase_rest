(ns pseudoace.utils)

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

(defn conj-in [m k v]
  "If `k` is already present in map `m`, conj `v` onto the existing
   seq of values, otherwise assoc a new vector of `v`."
  (assoc m k (if-let [o (m k)]
               (conj o v)
               [v])))

(defmacro except
  "Concatenate args as if with `str` then throw an exception"
  [& args]
  `(throw (Exception. (str ~@args))))

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

(defn find-index [pred col]
  (loop [[i & is] (range (count col))]
    (if (not (nil? i))
      (if (pred (nth col i))
        i
        (recur is)))))

