(ns web.rest.object
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]))

(defn obj-get [class db id]
  (entity db [(keyword class "id") id]))

(defn obj-tax [class obj]
  (let [species-ident (keyword class "species")]
    (if-let [species (species-ident obj)]
      (:species/id species)
      "all")))
          

(defmulti obj-label (fn [class obj] class))

(defmethod obj-label "gene" [_ obj]
  (or (:gene/public-name obj)
      (:gene/id obj)))

(defmethod obj-label :default [class obj]
  ((keyword class "id") obj))

(defmulti obj-name (fn [class db id] class))

(defmethod obj-name "gene" [class db id]
  (let [obj (obj-get class db id)]
    {:id    (:gene/id obj)
     :label (or (:gene/public-name obj)
                (:gene/id obj))
     :class "gene"
     :taxonomy (obj-tax class obj)}))

(defn obj-class [obj]
  (cond
   (:gene/id obj)
   "gene"

   (:cds/id obj)
   "cds"

   (:protein/id obj)
   "protein"))

(defn pack-obj
  ([obj]
     (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
     {:id       ((keyword class "id") obj)
      :label    (or label
                  (obj-label class obj))
      :class    class
      :taxonomy "c_elegans"}))
  
