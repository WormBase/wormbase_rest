(ns pseudoace.pace
  (:require [datomic.api :as d :refer (q entity touch entity-db)]
            [clojure.string :as str]))

(defn- not-nil?
  [x]
  (not (nil? x)))

(defrecord AceNode [type value children]
  java.lang.Comparable
    (compareTo [this a] (compare (:value this) (:value a))))

(defn- tag-path [[tag & tags] children]
  (let [te (AceNode. :tag tag children)]
    (if (seq tags)
      (tag-path tags [te])
      te)))

(declare objectify*)

(defn- objectify-level [db ent schemas children]
  (mapcat
   (fn [schema]
     (let [k      (:db/ident schema)
           tags   (:pace/tags schema)]
       (when tags
         (let [v        (k ent)
               card     (:db/cardinality schema)
               type     (:db/valueType schema)
               vs       (if (= card :db.cardinality/one)
                          [v]
                          v)
               obj-ref  (:pace/obj-ref schema)
               nodetype (if obj-ref
                          (:pace/identifies-class (entity db obj-ref))
                          type)
               vv       (if obj-ref
                          (filter not-nil? (map obj-ref vs))
                          vs)]
           (cond
             (:db/isComponent schema)
             [(tag-path (reverse (str/split tags #" "))
                        (mapcat (partial objectify* db) vv))]

             (= nodetype :db.type/ref)
             [(tag-path (reverse (str/split tags #" "))
                        (for [v vv :let [e (entity db v)
                                         t (:pace/tags e)]
                                   :when t]
                          (AceNode. :tag t children)))]

             (= nodetype :db.type/boolean)
             [(tag-path (reverse (str/split tags #" ")) nil)]

             :default
             [(tag-path (reverse (str/split tags #" "))
                        (for [v vv]
                          (AceNode. nodetype v children)))])))))
   schemas))

(def is-hash?
  (let [hashes #{"evidence" "molecular-change" "phenotype-info"}]
    (fn [schema]
      (hashes (namespace (:db/ident schema)))))) 

(defn- objectify*
  "Convert a datomic entity with :pace annotations into an ACeDB-like
   object"
  [db ent]
  (let [schemas        (map #(touch (entity db %)) (keys ent))
        right-schemas  (filter is-hash? schemas)
        left-schemas   (filter (complement is-hash?) schemas)]
    (objectify-level db ent left-schemas (when right-schemas
                                      (objectify-level db ent right-schemas nil)))))

(defn- reverse-ref [kw]
  (keyword (namespace kw) (str "_" (name kw))))

(defn objectify-xref [db ent xref]
  (let [tags (:pace.xref/tags xref)
        attr (reverse-ref (:pace.xref/attribute xref))
        obj-ref (:pace.xref/obj-ref xref)]
    (if-let [values (seq (attr ent))]
      [(tag-path (reverse (str/split tags #" "))
                 (for [v values]
                   (AceNode. (:pace/identifies-class (entity db obj-ref))
                             (obj-ref v)
                             [])))]
      [])))

(defn- tag-walker [tag-filter node]
  (if (tag-filter node)
    [node]
    (mapcat (partial tag-walker tag-filter) (:children node))))

(defn- simplify [node]
  (println "Simplifying " (:value node) " " (map :value (:children node)))
  (assoc node
    :children
    (for [[v c] (group-by :value (:children node))]
      (do
        (println v (map :value c))
        (simplify
         (assoc (first c)
           :children
           (mapcat :children c)))))))

(defn objectify
  "Convert a datomic entity with :pace annotations into an ACeDB-like
   object"
 ([clazz ent]
  (let [db             (entity-db ent)
        clent          (touch (entity db clazz))
        classname      (:pace/identifies-class clent)
        xrefs          (:pace/xref clent)]
     (AceNode.
      classname
      (clazz ent)
      (concat (objectify* db ent)
              (mapcat (partial objectify-xref db ent) xrefs)))))
 ([clazz ent ^String tag]
  (let [{:keys [type value children] :as root} (objectify clazz ent)
        tag-filter (fn [node]
                     (and (= (:type node) :tag)
                          (.equalsIgnoreCase tag (:value node))))]
     (AceNode.
      type
      value
      (tag-walker tag-filter root)))))
             
             
    
(defn- squote [s]
  (str \" s \"))

(defmulti print-ace (fn [format node] format))

(defn flatten-ace [prefix nodes]
  (if (seq nodes)
    (mapcat
     (fn [n]
       (flatten-ace (conj prefix n) (:children n)))
     nodes)
    [prefix]))

(defn common-prefix [as bs]
  (loop [n 0
         as (seq as)
         bs (seq bs)]
    (cond
     (nil? as) n
     (nil? bs) n
     (= (:value (first as)) (:value (first bs))) (recur (inc n) (seq (rest as)) (seq (rest bs)))
     :default n)))
     

(defn dedup-flattened [flattened]
  (map (fn [prev cur]
         (let [n (common-prefix prev cur)]
           (concat (repeat n nil)
                   (drop n cur))))
       (cons [] flattened)
       flattened))

(def ^:private type-to-ace
  (let [type-tags {:db.type/string    "Text"
                   :db.type/long      "Int"
                   :db.type/float     "Float"
                   :db.type/double    "Float"
                   :db.type/instant   "DateType"
                   :tag               "tag"}]
    (fn [t]
      (or (type-tags t)
          t))))

(defmethod print-ace :ace-format
  [_ node]
  ;; Root node is a special case here
  (println (:type node) ":" (squote (:value node)))
  ;; In non-timestamp mode, actual ACeDB collapses some tag
  ;; paths.  Not clear if this is actually required...?
  (doseq [l (flatten-ace [] (:children node))]
    (println
     (str/join " "
       (for [pn l]
         (cond
          (= (:type pn) :tag)
          (:value pn)
          :default
          (squote (:value pn))))))))

(defmethod print-ace :java-format
  [_ node]
  (println)
  (doseq [l (dedup-flattened (flatten-ace [] [node]))]
    (println
     (str/join "\t"
      (for [pn l]
        (cond
         (nil? pn)
         ""
         :default
         (str "?" (type-to-ace (:type pn)) "?" (:value pn) "?")))))))

(defn wrap-lines [^String str ^Integer width]
  (lazy-seq
   (if (> (.length str) width)
     (let [i   (.lastIndexOf str 32 width)
           i2  (if (>= i 0)
                 i
                 (.indexOf str 32))]
       (cons (if (>= i2 0)
               (.substring str 0 i2)
               str)
             (if (and (>= i2 0) (< i2 (dec (.length str))))
               (wrap-lines (.substring str (inc i2)) width))))
     (cons str nil))))

(defmethod print-ace :human-readable-format
  [_ node]
  (println)
  (println (:type node) (:value node))
  (loop [lines          (dedup-flattened (flatten-ace [] (:children node)))
         old-tab-stops  [3]]
    ;; Keep a record of the tab-stops used on the previous line
    ;; and re-use when possible.
    (when-let [line (first lines)]
      (let [tab-stops (transient [])]
        (loop [column 0
               nodes  line
               ts     old-tab-stops]
          ;; Can't test (first nodes) because dedup may
          ;; put nils in the line vectors.
          (when (seq nodes)
            (let [n (first nodes)
                  maybe-ts (or (first ts) 0)
                  actual-ts (max (+ 2 column) maybe-ts)
                  target-width (max (- 100 actual-ts) 32)
                  ns (if n 
                       (str (:value n))
                       "")
                  [fs & rs] (wrap-lines ns target-width)]
              (conj! tab-stops actual-ts)
              (print (apply str (repeat (- actual-ts column) \space)))
              (print fs)
              (doseq [rl rs]
                (println)
                (print (apply str (repeat (+ actual-ts 2) \space)))
                (print rl))
              (recur (+ actual-ts (max (count fs)
                                       (reduce 
                                         max
                                         0
                                         (for [s rs]
                                           (+ 2 (count s))))))
                     (rest nodes)
                     (rest ts)))))
        (println)
        (recur (rest lines)
               (persistent! tab-stops))))))

(defmethod print-ace :perl-format
  [format node]
  ;; All versions of AcePerl I've seen actually use Java (-j) dump format.
  ;; The one consumer of -p format I know of is biojava-acedb, which I don't
  ;; think exists in any meaningful way now.
  ;;
  ;; So leaving this unimplemented unless a really compelling use case emerges.
  (throw (Exception. (str "Don't support ace dumping in Perl (-p) format"))))

(defmethod print-ace :default
  [format node]
  (throw (Exception. (str "Don't support ace dumping in " format))))


;;
;; Current impl of potential-follows is quite ugly since
;; we don't have quite enough metadata about component entities
;; to statically generate the queries to follow into them.
;;

(def any (partial some identity))

(defn- follow-into-component [ent]
  (let [db (entity-db ent)]
    (any (for [k (keys ent)
               :let [km (entity db k)]
               :when (and (= (:pace/tags km) "")
                          (:pace/obj-ref km))]
           [[(k ent)] (:pace/obj-ref km)]))))

(defn- follow-into-components [ents]
  (let [follows (map follow-into-component ents)
        type (any (map second follows))]
    (when type
      [(apply concat (map first follows)) type])))

(defn potential-follows [db tag]
  (concat
   ; Links out.
   (mapcat
    (fn [[f]]
      (let [fo (entity db f)]
        (when (.equalsIgnoreCase tag (last (str/split (:pace/tags fo) #" ")))
          (let [o (:pace/obj-ref fo)]
            (cond
             o
             [(juxt (:db/ident fo) (constantly o))]
             (:db/isComponent fo)
             [(comp follow-into-components (:db/ident fo))])))))
    (q '[:find ?f :where [?f :pace/tags ?t]] db))

   ; Links in
   (mapcat
    (fn [[x]]
      (let [xo (entity db x)]
        (println (last (str/split (:pace.xref/tags xo) #" ")))
        (when (.equalsIgnoreCase tag (last (str/split (:pace.xref/tags xo) #" ")))
          (when-let [o (:pace.xref/obj-ref xo)]
            [(juxt (reverse-ref (:pace.xref/attribute xo)) (constantly o))]))))
    (q '[:find ?x :where [_ :pace/xref ?x]] db))))
           
