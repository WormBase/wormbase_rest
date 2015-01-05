(ns pseudoace.import-mongo
  (use pseudoace.utils
       clojure.instant)
  (require [datomic.api :as d :refer (db q entity touch)]
           [acetyl.parser :as ace]
           [clojure.string :as str]
           [monger.collection :as mc])
  (import java.io.FileInputStream java.util.zip.GZIPInputStream))

(defn mongo-name [ident]
  (-> (name ident)
      (str/replace #"-" "_")
      (keyword)))

(defrecord Importer [db tags])

(defn importer [con]
  (let [db (db con)]
    (Importer.
     db
     (->> (q '[:find ?ai
               :where [?ai :pace/tags _]]
             db)
          (map (fn [[ai]]
                 (touch (entity db ai))))
          (group-by #(namespace (:db/ident %)))))))

(declare import-acenodes)
(declare datomize-value)

(defn get-classes [db]
   (->> (for [[class ci] (q '[:find ?class ?ci
                              :where [?ci :pace/identifies-class ?class]]
                            db)]
          [class (touch (entity db ci))])
        (into {})))

(defn get-tags [imp nss]
  (->> (mapcat (:tags imp) nss)
       (map (fn [attr]
              [(last (str/split (:pace/tags attr) #" "))
               attr]))
       (into {})))
  

(defn datomize-tags [tags line]
  (if (seq line)
    (if-let [ltt (tags (first line))]
      [ltt (rest line)]
      (recur tags (rest line)))))

(defn datomize-objval [ti imp val]
  (let [ident      (:db/ident ti)
        tags       (get-tags imp #{(str (namespace ident) "." (name ident))
                                  "evidence"})
        maybe-obj  (tags (first val))]

    (cond
     maybe-obj
     (if (zero? (d/part (:db/id maybe-obj)))
       (except "Refers to a schema entity: " val)
       (mongo-name (:db/ident maybe-obj)))

     :default
     ;;; (except "We're confused..."))))
     (do
       (println "We're confused at " (:db/ident ti))
       {:db/doc   "confused placeholder!"}))))  ;; Temp workaround for ?Rearrangement
     

(defn datomize-value [ti imp val]
  (case (:db/valueType ti)
    :db.type/string
      (or (ace/unescape (first val))
          (if (:pace/fill-default ti) ""))
    :db.type/long
      (parse-int (first val))  
    :db.type/float
      (parse-double (first val))
    :db.type/double
      (parse-double (first val))
    :db.type/instant
      (if-let [v (first val)]  
        (-> (str/replace v #"_" "T")
            (read-instant-date))
        (if (:pace/fill-default ti)
          (read-instant-date "1977-10-29")))
    :db.type/boolean
      true      ; ACeDB just has tag presence/absence rather than booleans.  
    :db.type/ref
      (if-let [objref (:pace/obj-ref ti)]
        (first val)
        (datomize-objval ti imp val))
    ;;default
      (except "Can't handle " (:db/valueType ti))))

(defn- pace-items-for-ns [imp ns]
  ((:tags imp) ns))

(defn datomize-components [ti imp vals]
  (let [concs  (sort-by
                  :pace/order
                  (pace-items-for-ns
                    imp
                    (str (namespace (:db/ident ti)) "." (name (:db/ident ti)))))
        hashes (for [ns (:pace/use-ns ti)]
                 (entity (:db imp) (keyword ns "id")))]
    (for [[cvals hlines] (group-by (partial take (count concs)) vals)]
      (import-acenodes
       (if (seq cvals)
         (into {}
               (map
                (fn [conc val]
                  [(mongo-name (:db/ident conc)) (datomize-value conc imp [val])])
                concs cvals))
         {:dummy "placeholder"})
       (map (partial drop (count concs)) hlines)
       (get-tags imp (:pace/use-ns ti))
       imp))))           
      
(defn import-acenodes [base lines tags imp]
  (reduce
   (fn [ent [ti vals]]
     (vassoc ent
       (mongo-name (:db/ident ti))
       (if (:db/isComponent ti)
         (let [dc (datomize-components ti imp vals)]
           (if (= (:db/cardinality ti) :db.cardinality/one)
             (do
               (if (not= (count dc) 1)
                 (println "Expected only one value for " (:db/ident ti)))
               (first dc))
             dc))
         (if (= (:db/cardinality ti) :db.cardinality/one)
           (datomize-value ti imp (first vals))
           (seq (filter identity (map (partial datomize-value ti imp) vals))))))) ; drop nil values.
   
   base
   
   (reduce (fn [n line]
             (if-let [[ti data] (datomize-tags tags line)]
               (conj-in n ti data)
               n))
           {} lines)))

(defn import-aceobj [obj ci imp]
  (import-acenodes
   {:_id (:id obj)}
   (:lines obj)
   (get-tags imp #{(namespace (:db/ident ci))})
   imp))

(defn import-acefile
  "Read ACeDB objects from r and attempt to convert them to match the 
   pseudoace schema in con"
  [f imp mdb]
  (let [classes (get-classes (:db imp))]
    (doseq [obj (ace/ace-seq (ace/ace-reader f))]
      (case (:class obj)
        "LongText"
        (mc/insert mdb "LongText"
                   {:_id  (:id obj)
                    :text (ace/unescape (:text obj))})

        "DNA"
        (mc/insert mdb "DNA"
                   {:_id  (:id obj)
                    :sequence (:text obj)})
        
        "Peptide"
        (mc/insert mdb "DNA"
                   {:_id  (:id obj)
                    :sequence (:text obj)})

        ;; default
        (when-let [ci (classes (:class obj))]
          (mc/insert mdb (:class obj) (import-aceobj obj ci imp)))))))


(defn do-import [con path blocks]
  (let [imp (importer con)]
    (doseq [b blocks] 
      (println b)
      (let [txd (import-acefile (GZIPInputStream. (FileInputStream. (str path b ".ace.gz"))) imp)
            num (count txd)
            bs (int (Math/ceil (/ num 500)))]
        (println "Objects: " num " (" bs ")")
        (doseq [blk (partition-all bs txd)]
          (count @(d/transact 
                   con 
                   blk)))))))

(defn- make-index [mdb class props]
  (let [ind (keyword (str/join "." (map name props)))]
    (println class ind)
    (time (mc/ensure-index mdb class (array-map ind 1)))))

(defn- make-indices* [imp class tns prefix mdb]
  (doseq [i (pace-items-for-ns imp tns)]
    (cond
     (:db/index i)
      (make-index mdb class (conj prefix (mongo-name (:db/ident i))))
     (:db/isComponent i)
      (make-indices* imp class (str tns "." (name (:db/ident i))) (conj prefix (mongo-name (:db/ident i))) mdb)
     (and (= (:db/valueType i) :db.type/ref) (:pace/obj-ref i))
      (make-index mdb class (conj prefix (mongo-name (:db/ident i)))))))
    

(defn make-indices [imp class mdb]
  (when-let [ci ((get-classes (:db imp)) class)]
    (make-indices* imp class (namespace (:db/ident ci)) [] mdb)))

(defn make-all-indices [imp mdb]
  (doseq [class (keys (get-classes (:db imp)))]
    (make-indices imp class mdb)))
