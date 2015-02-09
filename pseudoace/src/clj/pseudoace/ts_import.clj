(ns pseudoace.ts-import
  (:use pseudoace.utils
        clojure.instant)
  (:require [pseudoace.import :refer [get-tags]]
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
      nil  
    ;:db.type/ref
    ;  (if-let [objref (:pace/obj-ref ti)]
    ;    (if (first val)
    ;      {:db/id (tempid :db.part/user)
    ;       objref (first val)})
    ;    (datomize-objval ti imp val))
    ;;default
      (except "Can't handle " (:db/valueType ti))))

      
(defn log-nodes [this lines imp nss]
  (let [tags (get-tags imp nss)]
    (reduce
     (fn [log line]
       (loop [[node & nodes]   line
              [stamp & stamps] (:timestamps (meta line))]
         (if-let [ti (tags node)]
           (if-let [lv (log-datomize-value ti imp nodes)]
             (update-in log [(or (first stamps) stamp)] conj [:db/add this (:db/ident ti) lv])
             log)
           (if node
             (recur nodes stamps)
             log))))    ;; No match => no log update.
     {} lines)))
     

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
