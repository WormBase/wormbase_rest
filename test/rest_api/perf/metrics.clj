(ns rest-api.perf.metrics
  "Response size analysis and metrics collection."
  (:require
   [cheshire.core :as json]
   [clojure.walk :as walk]))

(defn response-size-bytes
  "Calculate JSON-serialized size of response in bytes."
  [response]
  (-> response
      (json/generate-string)
      (.getBytes "UTF-8")
      count))

(defn response-size-kb
  "Calculate response size in KB."
  [response]
  (/ (response-size-bytes response) 1024.0))

(defn nesting-depth
  "Calculate maximum nesting depth of data structure."
  [data]
  (cond
    (map? data)
    (if (empty? data)
      1
      (inc (apply max (map nesting-depth (vals data)))))

    (sequential? data)
    (if (empty? data)
      1
      (inc (apply max (map nesting-depth data))))

    :else 0))

(defn count-packed-objects
  "Count entity references (maps with :class and :id keys) in response.
   These are created by pack-obj and indicate linked entities."
  [data]
  (let [counter (atom 0)]
    (walk/postwalk
     (fn [x]
       (when (and (map? x)
                  (contains? x :class)
                  (contains? x :id))
         (swap! counter inc))
       x)
     data)
    @counter))

(defn count-by-class
  "Count packed objects grouped by their :class value."
  [data]
  (let [classes (atom [])]
    (walk/postwalk
     (fn [x]
       (when (and (map? x)
                  (contains? x :class)
                  (contains? x :id))
         (swap! classes conj (:class x)))
       x)
     data)
    (frequencies @classes)))

(defn analyze-response
  "Analyze response data for size optimization opportunities."
  [data]
  (let [size-bytes (response-size-bytes data)
        size-kb (/ size-bytes 1024.0)
        depth (nesting-depth data)
        entity-count (count-packed-objects data)
        class-counts (count-by-class data)]
    {:size-bytes size-bytes
     :size-kb size-kb
     :nesting-depth depth
     :entity-count entity-count
     :entities-by-class class-counts
     :top-classes (->> class-counts
                       (sort-by val >)
                       (take 5)
                       (into {}))}))

(defn analyze-data-field
  "Analyze the :data field specifically (where most content is)."
  [response]
  (if-let [data (:data response)]
    (analyze-response data)
    {:size-bytes 0 :size-kb 0 :nesting-depth 0
     :entity-count 0 :entities-by-class {} :top-classes {}}))

(defn find-large-collections
  "Find collections in the response that contain many items.
   Returns paths to collections with their sizes."
  [data & {:keys [min-size] :or {min-size 50}}]
  (let [results (atom [])]
    (letfn [(walk-path [path d]
              (cond
                (map? d)
                (doseq [[k v] d]
                  (walk-path (conj path k) v))

                (sequential? d)
                (do
                  (when (>= (count d) min-size)
                    (swap! results conj {:path path :count (count d)}))
                  (doseq [[idx item] (map-indexed vector d)]
                    (walk-path (conj path idx) item)))))]
      (walk-path [] data))
    (->> @results
         (sort-by :count >)
         (take 10))))
