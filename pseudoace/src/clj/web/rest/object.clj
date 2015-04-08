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

(defmethod obj-label "phenotype" [_ obj]
  (or (->> (:phenotype/primary-name obj)
           (:phenotype.primary-name/text))
      (:phenotype/id obj)))

(defmethod obj-label "variation" [_ obj]
  (or (:variation/public-name obj)
      (:variation/id obj)))

(defn- author-lastname [author-holder]
  (or
   (->> (:affiliation/person author-holder)
        (first)
        (:person/last-name))
   (-> (:paper.author/author author-holder)
       (:author/id)
       (str/split #"\s+")
       (last))))

(defn- author-list [paper]
  (let [authors (->> (:paper/author paper)
                     (sort-by :ordered/index))]
    (cond
     (= (count authors) 1)
     (author-lastname (first authors))

     (< (count authors) 6)
     (let [names (map author-lastname authors)]
       (str (str/join ", " (butlast names)) " & " (last names)))

     :default
     (str (author-lastname (first authors)) " et al."))))

(defmethod obj-label "paper" [_ paper]
  (str (author-list paper) ", " (:paper/publication-date paper)))

(defmethod obj-label "feature" [_ feature]
  (or (:feature/public-name feature)
      (:feature/id feature)))

(defmethod obj-label "anatomy-term" [_ term]
  (or (:anatomy-term.term/text (:anatomy-term/term term))
      (:anatomy-term/id term)))

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
   "protein"
   
   (:feature/id obj)
   "feature"

   (:rearrangement/id obj)
   "rearrangement"

   (:variation/id obj)
   "variation"

   (:anatomy-term/id obj)
   "anatomy-term"))

(defn pack-obj
  ([obj]
     (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
   (if obj
     {:id       ((keyword class "id") obj)
      :label    (or label
                  (obj-label class obj))
      :class    class
      :taxonomy "c_elegans"})))
  
