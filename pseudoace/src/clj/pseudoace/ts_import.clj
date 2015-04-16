(ns pseudoace.ts-import
  (:use pseudoace.utils
        clojure.instant)
  (:require [pseudoace.import :refer [get-tags datomize-objval]]
            [datomic.api :as d :refer (db q entity touch tempid)]
            [acetyl.parser :as ace]
            [clojure.string :as str]
            [clojure.java.io :refer (file reader writer)])
  (:import java.io.FileInputStream java.util.zip.GZIPInputStream
           java.io.FileOutputStream java.util.zip.GZIPOutputStream))

;;
;; Logs are sets of datoms keyed by ACeDB-style timestamps.
;;
;; The datoms can optionally contain lookup-refs or augmented lookup-refs.
;; These behave as normal lookup-refs if their target already exists in the
;; database.  If not, it should be asserted as part of the first transaction
;; in which it appears.  Lookup refs can optionally contain a third part,
;; which should be the ident of the preferred partition for that entity.  If
;; the importer creates the entity, it will attempt to use this partition.
;; Partition idents are ignored for entities which already exist.
;;

(declare log-nodes)

(def timestamp-pattern #"(\d{4}-\d{2}-\d{2})_(\d{2}:\d{2}:\d{2})(?:\.\d+)?_(.*)")

(def pmatch @#'ace/pmatch)

(defn select-ts
  "Return any lines in acedb object `obj` with leading tags matching `path`" 
  [obj path]
  (for [l (:lines obj)
        :when (pmatch path l)]
    (with-meta
      (nthrest l (count path))
      {:timestamps (nthrest (:timestamps (meta l)) (count path))})))

(defn take-ts
  "`take` for sequences with :timestamps metadata."
  [n seq]
  (with-meta (take n seq)
    {:timestamps (take n (:timestamps (meta seq)))}))

(defn drop-ts
  "`drop` for sequences with :timestamps metadata."
  [n seq]
  (with-meta (drop n seq)
    {:timestamps (drop n (:timestamps (meta seq)))}))


(defn merge-logs [l1 l2]
  (cond
   (nil? l2)
   l1

   (nil? l1)
   l2

   :default
   (reduce
    (fn [m [key vals]]
      (assoc m key (into (get m key []) vals)))
    l1 l2)))

(defn- log-datomize-value [ti imp val]
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
          [objref (first val) (or (get-in imp [:classes objref :pace/prefer-part]) :db.part/user)])
        (datomize-objval ti imp val))
    ;;default
      (except "Can't handle " (:db/valueType ti))))


(defn- log-components [[_ _ part :as this] ti imp vals]
  (let [concs    (sort-by
                  :pace/order
                  ((:tags imp)
                   (str (namespace (:db/ident ti)) "." (name (:db/ident ti)))))
        nss      (:pace/use-ns ti)
        ordered? (get nss "ordered")
        hashes   (for [ns nss]
                   (entity (:db imp) (keyword ns "id")))]      ;; performance?
    (reduce
     (fn [log [index lines]]
       (if (and (> index 0)
                (not= (:db/cardinality ti) :db.cardinality/many))
         (do
           (println "WARNING: can't pack into cardinality-one component: " this lines)
           log)
         (let [cvals (take-ts (count concs) (first lines))
               compid [:importer/temp (d/squuid) part]]
           (->
            (merge-logs
             ;; concretes
             (reduce
              (fn [log [conc val stamp]]
                (if-let [lv (log-datomize-value conc imp [val])]
                  (update
                   log
                   stamp
                   conj
                   [:db/add compid (:db/ident conc) lv])
                  log))
              log
              (map vector concs cvals (:timestamps (meta cvals))))
             
             ;; hashes
             (log-nodes
              compid
              (map (partial drop-ts (count concs)) lines)
              imp
              nss))
            (update (first (:timestamps (meta (first lines))))
                    conj
                    [:db/add this (:db/ident ti) compid])
            (update (first (:timestamps (meta (first lines))))
                    conj-if
                    (if ordered?
                      [:db/add compid :ordered/index index]))))))
     {}
     (indexed (partition-by (partial take (count concs)) vals)))))



(defn log-nodes [this lines imp nss]
  (let [tags (get-tags imp nss)]
    (reduce
     (fn [log [ti lines]]
       (if (:db/isComponent ti)
         (merge-logs log (log-components this ti imp lines))
         (reduce
          (fn [log line]
            (if-let [lv (log-datomize-value ti imp line)]
              (update-in
               log
               [(first (:timestamps (meta line)))]
               conj
               [:db/add this (:db/ident ti) lv])
              log))
          log lines)))
     {}
     (reduce
      (fn [m line]
        (loop [[node & nodes]   line
               [stamp & stamps] (:timestamps (meta line))]
          (if node
            (if-let [ti (tags node)]
              (update-in m [ti] conjv (with-meta (or nodes []) {:timestamps (or (seq stamps) [stamp])}))
              (recur nodes stamps))
            m)))
      {} lines))))

(defn log-deletes [this lines imp nss]
  (let [tags (get-tags imp nss)]
    (reduce
     (fn [log line]
       (loop [[node & nodes]   line
              [stamp & stamps] (:timestamps (meta line))]
         (if node
           (if-let [ti (tags node)]
             (update
              log
              stamp
              conj
              (conj-if
               [:db/retract this (:db/ident ti)]
               (log-datomize-value     ;; If no value then this returns nil and
                ti                     ;; we get a "wildcard" retract that will be handled
                imp                    ;; at playback time.
                (if nodes
                  (with-meta nodes {:timestamps stamps})))))))))
     {} lines)))
     

(defmulti log-custom (fn [obj this imp] (:class obj)))

(defmethod log-custom "LongText" [{:keys [timestamp id text]} _ _]
  {timestamp
   [[:db/add [:longtext/id id] :longtext/text (ace/unescape text)]]})

(defmethod log-custom "DNA" [{:keys [timestamp id text]} _ _]
  {timestamp
   [[:db/add [:dna/id id] :dna/sequence text]]})

(defmethod log-custom "Peptide" [{:keys [timestamp id text]} _ _]
  {timestamp
   [[:db/add [:peptide/id id] :peptide/sequence text]]})

(defn- pair-ts [s]
  (map vector s (:timestamps (meta s))))

(defmethod log-custom "Position_Matrix" [{:keys [id timestamp] :as obj} _ _]
  (let [values (->> (select-ts obj ["Site_values"])
                    (map (juxt first (partial drop-ts 1)))
                    (into {}))
        bgs  (->> (select-ts obj ["Background_model"])
                  (map (juxt first (partial drop-ts 1)))
                  (into {}))]
    (->>
     (concat
      (if (seq bgs)
        (let [holder [:importer/temp (d/squuid)]]
          (conj
           (for [base ["A" "C" "G" "T"]
                 :let [val (bgs base)]]
             [(first (:timestamps (meta val)))
              [:db/add holder (keyword "position-matrix.value" (.toLowerCase base)) (parse-double (first val))]])
           [timestamp [:db/add [:position-matrix/id id] :position-matrix/background holder]])))
      (if (seq values)
        (mapcat
         (fn [index
              [a a-ts]
              [c c-ts]
              [g g-ts]
              [t t-ts]]
           (let [holder [:importer/temp (d/squuid)]]
             [[timestamp [:db/add [:position-matrix/id id] :position-matrix/values holder]]
              [timestamp [:db/add holder :ordered/index index]]
              [a-ts [:db/add holder :position-matrix.value/a (parse-double a)]]
              [c-ts [:db/add holder :position-matrix.value/c (parse-double c)]]
              [g-ts [:db/add holder :position-matrix.value/g (parse-double g)]]
              [t-ts [:db/add holder :position-matrix.value/t (parse-double t)]]]))
         (iterate inc 0)
         (pair-ts (values "A"))
         (pair-ts (values "C"))
         (pair-ts (values "G"))
         (pair-ts (values "T")))))
           
     (reduce
      (fn [log [ts datom]]
        (update log ts conjv datom))
      {}))))
           
(def ^:private s-child-types
  {"Gene_child"            :gene/id
   "CDS_child"             :cds/id
   "Transcript"            :transcript/id
   "Pseudogene"            :pseudogene/id
   "Pseudogene_child"      :pseudogene/id
   "Transposon"            :transposon/id
   "Genomic_non_canonical" :sequence/id
   "Nongenomic"            :sequence/id
   "PCR_product"           :pcr-product/id
   "Operon"                :operon/id
   "AGP_fragment"          :sequence/id
   "Allele"                :variation/id
   "Oligo_set"             :oligo-set/id
   "Feature_object"        :feature/id
   "Feature_data"          :feature-data/id
   "Homol_data"            :homol-data/id
   "Expr_profile"          :expr-profile/id})

(defmethod log-custom :default [obj this imp]
  ;;
  ;; Is it worth creating a custom hierarchy so this only gets called for
  ;; objects which might have S_children?
  ;;
  (if-let [sc (select-ts obj ["SMap" "S_child"])]
    (reduce
     (fn [log [[type link start end :as m] info-lines]]
       (if-let [ident (s-child-types type)]
         (let [child [ident link (or (get-in imp [:classes ident :pace/prefer-part])
                                     :db.part/user)]
               start (parse-int start)
               end   (parse-int end)]
           (if (and start end)
             (update
              log
              (second (:timestamps (meta m)))
              conj
              [:db/add child :locatable/parent this]
              [:db/add child :locatable/min (dec (min start end))]
              [:db/add child :locatable/max (max start end)]
              [:db/add child :locatable/strand (if (< start end)
                                                 :locatable.strand/positive
                                                 :locatable.strand/negative)])
             (update
              log
              (second (:timestamps (meta m)))
              conj
              [:db/add child :locatable/parent this])))
         log))
     {}
     (group-by (partial take-ts 4) sc))))

(defn obj->log [imp obj]
  (let [ci ((:classes imp) (:class obj))
        this (if ci
               [(:db/ident ci) (:id obj) (or (:pace/prefer-part ci) :db.part/user)])]
    (merge-logs
     (if this
       (cond
        (:delete obj)
        {nil
         [[:db.fn/retractEntity this]]}
        
        (:rename obj)
        {nil
         [[:db/add this (:db/ident ci) (:rename obj)]]}

        :default
        (merge-logs
         (log-nodes
          this
          (:lines obj)
          imp
          #{(namespace (:db/ident ci))})
         (if-let [dels (seq (filter #(= (first %) "-D") (:lines obj)))]
           (log-deletes
            this
            (map (partial drop-ts 1) dels)  ; Remove the leading "-D"
            imp
            #{(namespace (:db/ident ci))})))))
     (log-custom obj this imp))))

(defn objs->log [imp objs]
  (reduce
   (fn [log obj]
     (if-let [objlog (obj->log imp obj)]
       (merge-logs log objlog)
       log))
   {} objs))

(defn- temp-datom [db datom temps index]
  (let [ref (datom index)]
    (if (vector? ref)
      (let [[k v part] ref
            lref       [k v]]
        (if (entity db lref)
          [(assoc datom index lref) temps]    ; turn 3-element refs into normal lookup-refs
          (if-let [tid (temps ref)]
            [(assoc datom index tid) temps]
            (let [tid (d/tempid (or part :db.part/user))]
              [(assoc datom index tid)
               (assoc temps ref tid)
               [:db/add tid k v]]))))
      [datom temps])))

(defn fixup-datoms
  "Replace any lookup refs in `datoms` which can't be resolved in `db` with tempids,
   and expand wildcard :db/retracts"
  [db datoms]
  (->>
   (reduce
    (fn [{:keys [done temps]} datom]
      (let [[datom temps ex1] (temp-datom db datom temps 1)
            [datom temps ex2] (temp-datom db datom temps 3)]
        {:done  (conj-if done datom ex1 ex2)
         :temps temps}))
    {:done [] :temps {}}
    (mapcat
     (fn [[op e a v :as datom]]
       (if (and (= op :db/retract)
                (nil? v))
         (for [[_ _ v] (d/datoms db :eavt e a)]
           (conj datom v))
         [datom]))
     datoms))
   :done))

(defn play-log [con log]
  (doseq [[stamp datoms] (sort-by first log)
          :let [[_ ds ts name]
                (re-matches timestamp-pattern stamp)
                time (read-instant-date (str ds "T" ts))]]
    (let [db (db con)
          datoms (fixup-datoms db datoms)]
      @(d/transact con (conj datoms {:db/id        (d/tempid :db.part/tx)
                                     :db/txInstant time
                                     :importer/ts-name name})))))


(def log-fixups
  {nil        "1977-01-01_01:01:01_nil"
   "original" "1970-01-02_01:01:01_original"})

(defn clean-log-keys [log]
  (->> (for [[k v] log]
         [(or (log-fixups k) k) v])
       (into {})))

(defn logs-to-dir
  [logs dir]
  (doseq [[stamp logs] (clean-log-keys logs)
          :let  [[_ date time name]
                 (re-matches timestamp-pattern stamp)]]
    (with-open [w (-> (file dir (str date ".edn.gz"))
                      (FileOutputStream. true)
                      (GZIPOutputStream.)
                      (writer))]
      (binding [*out* w]
        (doseq [l logs]
          (println stamp (pr-str l)))))))

(defn split-logs-to-dir
  "Convert `objs` to log entries then spread them into .edn files split by date."
  [imp objs dir]
  (logs-to-dir (objs->log imp objs) dir))

(defn logfile-seq [r]
  (for [l (line-seq r)
        :let [i (.indexOf l " ")]]
    [(.substring l 0 i)
     (read-string (.substring l (inc i)))]))

(defn partition-log
  "Similar to partition-all but understands the log format, and will cut 
   after `max-text` chars of string data have been seen."
  [max-count max-text logs]
  (if-let [logs (seq logs)]
    (loop [logs     logs
           accum    []
           text     0]
      (cond
       (or (empty? logs)
           (>= (count accum) max-count)
           (>= text max-text))
       (cons accum (lazy-seq (partition-log max-count max-text logs)))

       :default
       (let [[[_ [_ _ _ v] :as log] & rest] logs]
         (recur rest
                (conj accum log)
                (if (string? v)
                  (+ text (count v))
                  text)))))))

(defn play-logfile [con logfile]
  (with-open [r (reader logfile)]
    (doseq [rblk (partition-log 1000 50000 (logfile-seq r))]
      (doseq [sblk (partition-by first rblk)
              :let [[_ ds ts name]
                    (re-matches timestamp-pattern (ffirst sblk))
                    time (read-instant-date (str ds "T" ts))]]
        (let [blk (map second sblk)
              db      (db con)
              fdatoms (filter (fn [[_ _ _ v]] (not (map? v))) blk)
              datoms  (fixup-datoms db fdatoms)]
          @(d/transact-async con (conj datoms {:db/id             (d/tempid :db.part/tx)
                                         :db/txInstant      time
                                         :importer/ts-name  name})))))))



