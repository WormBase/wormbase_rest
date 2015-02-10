(ns pseudoace.ts-import
  (:use pseudoace.utils
        clojure.instant)
  (:require [pseudoace.import :refer [get-tags datomize-objval]]
            [datomic.api :as d :refer (db q entity touch tempid)]
            [acetyl.parser :as ace]
            [clojure.string :as str])
  (:import java.io.FileInputStream java.util.zip.GZIPInputStream))

(defn merge-logs [l1 l2]
  (reduce
   (fn [m [key vals]]
     (assoc m key (concat (get m key) vals)))
   l1 l2))

(defn log-datomize-value [ti imp val]
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
          [objref (first val)])
        (datomize-objval ti imp val))
    ;;default
      (except "Can't handle " (:db/valueType ti))))

(defn take-ts [n seq]
  (with-meta (take n seq)
    {:timestamps (take n (:timestamps (meta seq)))}))

(defn drop-ts [n seq]
  (with-meta (drop n seq)
    {:timestamps (drop n (:timestamps (meta seq)))}))

(defn log-components [this ti imp vals]
  (let [concs    (sort-by
                  :pace/order
                  ((:tags imp)
                   (str (namespace (:db/ident ti)) "." (name (:db/ident ti)))))
        nss      (:pace/use-ns ti)
        ordered? (get nss "ordered")
        hashes   (for [ns nss]
                   (entity (:db imp) (keyword ns "id")))]      ;; performance?
    (reduce
     (fn [log [index [cvals hlines]]]
       (let [compid [:importer/temp (d/squuid)]]
         (update 
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
            (map (partial drop-ts (count concs)) hlines)
            imp
            nss))
          (first (:timestamps (meta (first hlines))))
          conj
          [:db/add this (:db/ident ti) compid])))
     {}
     (indexed (group-by (partial take-ts (count concs)) vals)))))
      
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
              (update-in m [ti] conj (with-meta (or nodes []) {:timestamps (or (seq stamps) [stamp])}))
              (recur nodes stamps))
            m)))
      {} lines))))
     

(defn obj->log [imp obj]
  (when-let [ci ((:classes imp) (:class obj))]
    (log-nodes (:lines obj) imp #{(namespace (:db/ident ci))})))

(defn objs->log [imp objs]
  (reduce
   (fn [log obj]
     (if-let [objlog (obj->log imp obj)]
       (merge-logs log objlog)
       log))
   {} objs))

(defn temp-datom [db datom temps index]
  (let [ref (datom index)]
    (if (vector? ref)
      (if (entity db ref)
        [datom temps]
        (if-let [tid (temps ref)]
          [(assoc datom index tid) temps]
          (let [tid (d/tempid :db.part/user)]
            [(assoc datom index tid)
             (assoc temps ref tid)
             [:db/add tid (first ref) (second ref)]])))
      [datom temps])))

(defn temp-datoms
  "Replace any lookup refs in `datoms` which can't be resolved in `db` with tempids"
  [db datoms]
  (->>
   (reduce
    (fn [{:keys [done temps]} datom]
      (let [[datom temps ex1] (temp-datom db datom temps 1)
            [datom temps ex2] (temp-datom db datom temps 3)]
        {:done  (conj-if done datom ex1 ex2)
         :temps temps}))
    {:done [] :temps {}}
    datoms)
   :done))
           
            

(defn play-log [con log]
  (doseq [[stamp datoms] (sort-by first log)
          :let [[_ ds ts name]
                (re-matches #"(\d{4}-\d{2}-\d{2})_(\d{2}:\d{2}:\d{2})_(.*)" stamp)
                time (read-instant-date (str ds "T" ts))]]
    (let [db (db con)
          datoms (temp-datoms db datoms)]
      @(d/transact con (conj datoms {:db/id        (d/tempid :db.part/tx)
                                     :db/txInstant time
                                     :db/doc       name})))))
