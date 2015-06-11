(ns pseudoace.utils
  (:require [clojure.java.io :refer (writer)]))

(defn vmap
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values."
  [& args] 
  (into {} (for [[k v] (partition 2 args) 
                 :when (not (nil? v))] 
             [k v])))

(defn vmap-if
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values.  Return nil if all values are empty"
  [& args]
  (reduce
   (fn [m [k v]]
     (if (nil? v)
       m
       (assoc m k v)))
   nil (partition 2 args)))

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

(defn conj-if
  ([col x]
     (cond
      (nil? x)
      col

      (nil? col)
      [x]

      :default
      (conj col x)))
  ([col x & xs]
     (reduce conj-if (conj-if col x) xs)))

(defn conjv
  "Like `conj` but creates a single-element vector if `coll` is nil."
  ([coll x]
     (if (nil? coll)
       [x]
       (conj coll x)))
  ([coll x & xs]
     (reduce conj (conjv coll x) xs)))

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

(defmacro with-outfile [f & body]
  "Execute `body` with *out* bound to `(writer f)`."
  (let [fh (gensym)]
    `(with-open [~fh (writer ~f)]
       (binding [*out* ~fh]
         ~@body))))

(defn those
  "Return a seq consisting (only) of the true arguments,
   or `nil` if no arguments are true"
  [& args]
  (seq (filter identity args)))

(deftype Pair [k v])

(defn- pair-k [^Pair p]
  (.-k p))

(defn- pair-v [^Pair p]
  (.-v p))

(defn sort-by-cached
  "Similar to `sort-by` but caches the results of `keyfn`."
  [keyfn coll]
  (->> (map #(Pair. (keyfn %) %) coll)
       (into-array Pair)
       (sort-by pair-k)
       (mapv pair-v)))
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

;;
;; From old clojure-contrib
;;

(defmacro cond-let
  "Takes a binding-form and a set of test/expr pairs. Evaluates each test
  one at a time. If a test returns logical true, cond-let evaluates and
  returns expr with binding-form bound to the value of test and doesn't
  evaluate any of the other tests or exprs. To provide a default value
  either provide a literal that evaluates to logical true and is
  binding-compatible with binding-form, or use :else as the test and don't
  refer to any parts of binding-form in the expr. (cond-let binding-form)
  returns nil."
  [bindings & clauses]
  (let [binding (first bindings)]
    (when-let [[test expr & more] clauses]
      (if (= test :else)
        expr
        `(if-let [~binding ~test]
           ~expr
           (cond-let ~bindings ~@more))))))
