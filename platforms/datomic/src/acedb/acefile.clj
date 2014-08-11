(ns acedb.acefile
  (:use clojure.java.io)
  (:require [clojure.string :as str]))

(defrecord AceReader [reader]
  java.io.Closeable
  (close [_]
    (.close reader)))

(defn ace-reader
  "Open a .ace file for reading"
  [ace]
    (AceReader. (reader ace)))

(defn- null-line? [^String l]
  (or (empty? l)
      (.startsWith (.trim l) "//")))

(defn- long-text-end? [l]
  (= l "***LongTextEnd***"))

(def ^:private header-re #"^(\w+) : \"([^\"]+)\"")
(def ^:private line-re #"[A-Za-z_0-9.-]+|\".*?[^\\]\"")

(defn- unquote-str [^String s]
  (if (.startsWith s "\"")
    (if (.endsWith s "\"")
      (.substring s 1 (dec (count s)))
      (throw (Exception. (str "malformed string " s))))
    s))
   
      

(defn- parse-aceobj [[header-line & lines]]
  (if-let [[_ clazz id] (re-find header-re header-line)]
    {:class clazz 
     :id id 
     :lines (vec (for [l lines]
              (mapv #(unquote-str (first %)) (partition 3 (re-seq line-re l)))))}
    (throw (Exception. (str "Bad header line: " header-line)))))
  

(defn- aceobj-seq [lines]
  (lazy-seq
   (let [lines       (drop-while null-line? lines)
         obj-lines   (take-while (complement null-line?) lines)
         rest-lines  (drop-while (complement null-line?) lines)]
     (when (not (empty? obj-lines))
       (let [obj (parse-aceobj obj-lines)]
         (if (= (:class obj) "LongText")
           (cons (assoc obj :text (.trim (str/join "\n" 
                                           (take-while (complement long-text-end?) rest-lines))))
                 (aceobj-seq (rest (drop-while (complement long-text-end?) rest-lines))))
           (cons obj
                 (aceobj-seq rest-lines))))))))

(defn ace-seq
  "Return a sequence of objects from a .ace file"
  [ace]
  (aceobj-seq (line-seq (:reader ace))))

(defn- pmatch 
  "Test whether `path` is a prefix of `l`"
  [path l]
  (and (< (count path) (count l))
       (= path (take (count path) l))))

(defn select
  "Return any lines in acedb object `obj` with leading tags matching `path`" 
  [obj path]
  (for [l (:lines obj)
        :when (pmatch path l)]
    (nthrest l (count path))))

(defn unescape [s]
  "Unescape \\-escaped characters in a string."
  (when s
    (str/replace s #"\\(.)" "$1")))

(defn query-ace [server port ace-class]
  "Minimal interface to a socket ace server"
  (let [ace (acetyl.AceSocket. server port)]
    (.transact ace (str "find " ace-class))
    (->> (.transactToStream ace "show -a -T")
         (reader)
         (line-seq)
         (aceobj-seq))))
