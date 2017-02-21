(ns rest-api.regression-testing
  (:require
   [clojure.data :as data]
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
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
    (when (= default-path "/tmp")
      (println "Warning: you probably want to supply"
               ":fixtures-path in 'opts'"))
    (get opts :fixtures-path default-path)))

(defn create-test-fixture
  "Creates a test fixture for interactions.
  `url` should resolve to a JSON document that the existing API should
  re-produce."
  [url opts]
  (let [data (slurp url)
        out-path (fixtures-path opts)
        out-file (io/file out-path (fixture-filename url))]
    (binding [*out* (io/writer out-file)
              *print-length* nil]
      (println (json/read-str data)))))

(defn read-test-fixture
  "Read an EDN test fixtures saved from an existing WB service."
  [url opts]
  (let [base-path (fixtures-path opts)
        filename (fixture-filename url)
        fixture-path (str/join "/" [base-path filename])]
    (->> (io/file fixture-path)
         (io/reader)
         (PushbackReader.)
         (edn/read))))

(defn compare-api-result
  "Comare the result of a widget function with a stored EDN fixture."
  [widget-name expected actual & opts]
  (let [debug? (get opts :debug? false)
        get-in-data (partial get-in ["fields" widget-name])
        act (get-in-data actual)
        exp (get-in-data expected)
        [left right both] (data/diff exp act)]
    (when-not (every? nil? [left right both])
      (when debug?
        (println "EXPECTED ONLY:")
        (println left)
        (println)
        (println "ACTUAL ONLY:")
        (println right))
      {:expected-only left
       :actual-only right
       :both both})))

