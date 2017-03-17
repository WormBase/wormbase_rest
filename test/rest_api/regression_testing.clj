(ns rest-api.regression-testing
  (:require
   [cheshire.core :as json]
   [clojure.data :as data]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   (java.io PushbackReader)
   (java.net URI)))

(defn fixture-filename
  "Given a WormBase service `url` return a filename to use for writing
  or reading an EDN fixture."
  [url]
  (let [uri (URI. url)
        path (.getPath uri)
        path-len (.length path)
        filename (-> path
                     (str/replace-first "/" "")
                     (str/replace "/" "_"))]
    (str filename ".edn")))

(defn fixtures-path
  "Return the path for saved EDN fixtures from `opts`."
  [opts]
  (let [default-path "/tmp"]
    (when (= (:fixtures-path opts) default-path)
      (println "Warning: you probably want to supply"
               ":fixtures-path in 'opts'"))
    (get opts :fixtures-path default-path)))

(defn create-test-fixture
  "Creates a test fixture for interactions.
  `url` should resolve to a JSON document that the existing API should
  re-produce."
  [url opts & [munge-data]]
  (let [data (slurp url)
        out-path (fixtures-path opts)
        out-file (io/file out-path (fixture-filename url))]
    (when-not (.exists out-file)
      (let [pdata (json/parse-string data)
            mdata (if munge-data
                    (munge-data pdata)
                    pdata)]
        (binding [*out* (io/writer out-file)
                  *print-length* false]
          (prn mdata))))))

(defn read-test-fixture
  "Read an EDN test fixtures saved from an existing WB service."
  [url opts]
  (let [base-path (fixtures-path opts)
        filename (fixture-filename url)
        fixture-path (str/join "/" [base-path filename])]
    (->> (io/file fixture-path)
         (io/reader)
         (PushbackReader.)
         (edn/read)
         (walk/keywordize-keys))))

(defn sort-maps-by-id [maps]
  (->> maps
       (sort-by #(get % "id"))
       (vec)
       (filter identity)))

(defn update-to-comparable [data]
  (walk/postwalk (fn [xform]
                   (if (map? xform)
                     (reduce-kv
                      (fn [x k v]
                        (assoc x k (cond
                                     (= k "citations")
                                     (sort-maps-by-id (set v))
                                     
                                     (and (or (vector? v) (list? v))
                                          (every? map? v))
                                     (sort-maps-by-id v)
                                     :default v)))
                      (empty xform)
                      xform)
                     xform))
                 data))

(defn compare-api-result
  "Compare the result of a widget function with a stored EDN fixture.
  Returns `nil` if `actual` is equal to `expected`.
  Returns a mapping describing the differences otherwise."
  [widget-name ref-result result & [opts]]
  (let [debug? (get opts :debug? false)
        actual (-> result
                   (walk/stringify-keys)
                   (update-to-comparable))
        expected (-> ref-result
                     (get-in ["fields" widget-name])
                     (update-to-comparable))
        [left right both] (data/diff expected actual)]
    (when-not (every? nil? [left right])
      (when debug?
        (when left
          (println)
          (println "Differences in expected but not in actual:")
          (pprint left)
          (println)
          (println "--- Actual:")
          (pprint actual))
        (when right
          (println)
          (println "Differences in actual but not in expected:")
          (pprint right)
          (println)
          (println "+++ Expected:")
          (pprint expected)))
      {:expected-only left
       :actual-only right
       :both both})))
