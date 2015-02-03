(ns pseudoace.import
  (use pseudoace.utils
       clojure.instant)
  (require [datomic.api :as d :refer (db q entity touch tempid)]
           [acetyl.parser :as ace]
           [clojure.string :as str])
  (import java.io.FileInputStream java.util.zip.GZIPInputStream))

;;
;; TODO
;;   - Ensure correct ordering of multiple concretes per component
;;   - What happens if a single component has more than one hash?
;;

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
       (:db/ident maybe-obj))

     :default
     ;;; (except "We're confused..."))))
     (do
       (println "We're confused at " (:db/ident ti))
       {:db/id    (tempid :db.part/user)
        :db/doc   "confused placeholder!"}))))  ;; Temp workaround for ?Rearrangement
     

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
        (if (first val)
          {:db/id (tempid :db.part/user)
           objref (first val)})
        (datomize-objval ti imp val))
    ;;default
      (except "Can't handle " (:db/valueType ti))))

(defn- pace-items-for-ns [imp ns]
  ((:tags imp) ns))

(defn datomize-components [ti imp vals]
  (let [concs    (sort-by
                    :pace/order
                    (pace-items-for-ns
                      imp
                      (str (namespace (:db/ident ti)) "." (name (:db/ident ti)))))
        nss      (:pace/use-ns ti)
        ordered? (if nss
                   (nss "ordered"))
        hashes   (for [ns nss]
                   (entity (:db imp) (keyword ns "id")))]

    (map-indexed
     (fn [idx [cvals hlines]]
       (let [comp
             (import-acenodes
              (vassoc
               (if (seq cvals)
                 (into {}
                       (map
                        (fn [conc val]
                          [(:db/ident conc) (datomize-value conc imp [val])])
                        concs cvals))
                 {})
               :ordered/index
               (if ordered?
                 idx))
              (map (partial drop (count concs)) hlines)
              (get-tags imp nss)
              imp)]
         (if (empty? comp)
           {:db/doc "placeholder"}
           comp)))
     (group-by (partial take (count concs)) vals))))

(defn import-acenodes [base lines tags imp]
  (reduce
   (fn [ent [ti vals]]
     (vassoc ent
       (:db/ident ti)
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
   {:db/id          (tempid :db.part/user)
    (:db/ident ci)  (:id obj)}
   (:lines obj)
   (get-tags imp #{(namespace (:db/ident ci))})
   imp))

(defmulti import-custom :class)

(defmethod import-custom "Position_Matrix" [obj]
  (let [values (->> (ace/select obj ["Site_values"])
                    (map (juxt first #(mapv parse-double (rest %))))
                    (into {}))
        bgs  (->> (ace/select obj ["Background_model"])
                  (map (juxt first #(parse-double (second %))))
                  (into {}))]
    (vmap
     :position-matrix/values
     (map-indexed
      (fn [idx item]
        (assoc item :ordered/index idx))
      (map 
       (fn [a c g t]
         {:position-matrix.value/a a
          :position-matrix.value/c c
          :position-matrix.value/g g
          :position-matrix.value/t t})
       (values "A")
       (values "C")
       (values "G")
       (values "T")))

     :position-matrix/background
     (if (seq bgs)
       {:position-matrix.value/a (bgs "A")
        :position-matrix.value/c (bgs "C")
        :position-matrix.value/g (bgs "G")
        :position-matrix.value/t (bgs "T")}))))

(defmethod import-custom :default [obj]
  {})

(defn import-acefile
  "Read ACeDB objects from r and attempt to convert them to match the 
   pseudoace schema in con"
  [f imp]
  (let [classes (get-classes (:db imp))]
    (doall
     (mapcat
      (fn [obj]
        (case (:class obj)
          "LongText"
          [{:db/id            (d/tempid :db.part/user)
            :longtext/id      (:id obj)
            :longtext/text    (ace/unescape (:text obj))}]

          "DNA"
          [{:db/id            (d/tempid :db.part/user)
            :dna/id           (:id obj)
            :dna/sequence     (:text obj)}]

          "Peptide"
          [{:db/id            (d/tempid :db.part/user)
            :peptide/id       (:id obj)
            :peptide/sequence (:text obj)}]

          ;; default
          (when-let [ci (classes (:class obj))]
            [(merge 
              (import-aceobj obj ci imp)
              (import-custom obj))])))
      (ace/ace-seq (ace/ace-reader f))))))


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
