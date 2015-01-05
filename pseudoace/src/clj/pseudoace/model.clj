(ns pseudoace.model
  (:require [clojure.string :as str])
  (:import [java.util.regex Matcher]))

(defrecord ModelNode [type name unique? repeat? xref children])

(defn- indexed-tokens* [^Matcher m]
  (lazy-seq
   (when-let [tok (re-find m)]
     (cons
      [tok (.start m)]
      (indexed-tokens* m)))))

(defn- indexed-tokens [s]
  "Return the tokens of s, along with the column indices in which they start"
  (doall (indexed-tokens* (re-matcher #"[A-Za-z0-9_\?\#\^-]+" s))))

(defn- positive? [i]
  (> i 0))

(defn- conj-if [col maybe-add]
  (if maybe-add
    (conj col maybe-add)
    col))

(defn- uncomment [^String s]
  (let [i (.indexOf s "//")]
    (cond
     (zero? i)
     ""
     
     (positive? i)
     (.substring s 0 i)
     
     :default
     s)))

(declare parse-model-line)

(defn- parse-model-line* [n toks]
  (let [[peek _] (first toks)]
    (cond
     (nil? peek)
       n 
     (= peek "UNIQUE")
       (parse-model-line* (assoc n :unique? true) (rest toks))
     (= peek "REPEAT")
       (parse-model-line* (assoc n :repeat? true) (rest toks))  
     (#{"XREF" "NOXREF" "YESXREF"} peek)
       (parse-model-line* (assoc n :xref (first (second toks))
                                   :suppress-xref (= peek "NOXREF"))
                          (drop 2 toks))
     (= peek "FILL_DEFAULT")
       (parse-model-line* (assoc n :fill-default true) (rest toks))   

     (.startsWith peek "^")
       (parse-model-line* (assoc n :alt-name (.substring peek 1)) (rest toks))
     :default
       (assoc n :children [(parse-model-line toks)]))))

(def ^:private prim-types
  {"?Text"    :text
   "Text"     :text
   "DateType" :date
   "Int"      :int
   "INT"      :int
   "Float"    :float})

(defn- parse-model-line [toks]
  (when-let [[token index] (first toks)]
    (let [n        (with-meta
                     (ModelNode. (or (prim-types token)
                                     (when (.startsWith token "?")
                                       :ref)
                                     (when (.startsWith token "#")
                                       :hash)
                                     :tag)
                                 token
                                 false
                                 false
                                 nil
                                 nil)
                     {:index index})]
      (parse-model-line* n (rest toks)))))

(defn- append-model-line [model line]
  (when (not model)
    (throw (Exception. (str "Couldn't insert " (:name line)))))
  (if line
    (let [children    (:children model)
          new-index   (:index (meta line))
          insert-index (or (:index (meta (last children))) 0)
          index-dif   (- new-index insert-index)]
      (cond
       (positive? index-dif)
       (assoc model :children
              (assoc
                children
                (dec (count children))
                (append-model-line (last children) line)))

       (zero? index-dif)
       (assoc model :children
              (conj children line))

       :default
       (throw (Exception. (str "Couldn't insert model node " (:name line) " at index " new-index ", tried at " insert-index)))))
    model))
    
(defn parse-models [r]
  "Read ACeDB models from Reader `r`"
  (loop [lines          (map uncomment (line-seq r))
         models         []
         cmodel         nil]
    (let [toks (if-let [line (first lines)]
                 (indexed-tokens line))]
      (cond
       (nil? toks)
       (conj-if models cmodel)     ; return

       (empty? toks)
       (recur (rest lines) models cmodel)

       (zero? (second (first toks)))   ; Start on a new line
       (recur (rest lines)
              (conj-if models cmodel)
              (parse-model-line toks))

       :default
       (recur (rest lines)
              models
              (append-model-line cmodel (parse-model-line toks)))))))
