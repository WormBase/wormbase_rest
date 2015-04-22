(ns web.rest.object
  (:use pseudoace.utils)
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

(defmethod obj-label "do-term" [_ term]
  (:do-term/name term))

(defmethod obj-label "person" [_ person]
  (:person/standard-name person))

(defmethod obj-label "construct" [_ cons]
  (or (first (:construct/public-name cons))
      (:construct/id cons)))

(defmethod obj-label "transgene" [_ tg]
  (or (:transgene/public-name tg)
      (:transgene/id tg)))

(defmethod obj-label "go-term" [_ go]
  (first (:go-term/term go)))    ;; Not clear why multiples allowed here!

(defmethod obj-label "life-stage" [_ ls]
  (:life-stage/public-name ls))

(defmethod obj-label "protein" [_ prot]
  (or (first (:protein/gene-name prot))
      (:protein/id prot)))

(defmethod obj-label "interaction" [_ int]
  (let [db (d/entity-db int)]
    (->>
     (q '[:find [?interactor ...]
          :in $ ?int
          :where (or-join [?int ?interactor]
                   (and
                    [?int :interaction/pcr-interactor ?pi]
                    [?pi :interaction.pcr-interactor/pcr-product ?interactor])
                   (and
                    [?int :interaction/sequence-interactor ?si]
                    [?si :interaction.sequence-interactor/sequence ?interactor])
                   (and
                    [?int :interaction/interactor-overlapping-cds ?ci]
                    [?ci :interaction.interactor-overlapping-cds/cds ?interactor])
                   (and
                    [?int :interaction/interactor-overlapping-gene ?gi]
                    [?gi :interaction.interactor-overlapping-gene/gene ?interactor])
                   (and
                    [?int :interaction/interactor-overlapping-protein ?pi]
                    [?pi :interaction.interactor-overlapping-protein/protein ?interactor])
                   (and
                    [?int :interaction/molecule-interactor ?mi]
                    [?mi :interaction.molecule-interactor/molecule ?interactor])
                   (and
                    [?int :interaction/other-regulator ?ortor]
                    [?ortor :interaction.other-regulator/text ?interactor])
                   (and
                    [?int :interaction/other-regulated ?orted]
                    [?orted :interaction.other-regulated/text ?interactor])
                   (and
                    [?int :interaction/rearrangement ?ri]
                    [?ri :interaction.rearrangement/rearrangement ?interactor])
                   (and
                    [?int :interaction/feature-interactor ?fi]
                    [?fi :interaction.feature-interactor/feature ?interactor])
                   (and
                    [?int :interaction/variation-interactor ?vi]
                    [?vi :interaction.variation-interactor/variation ?interactor]))]
        db (:db/id int))
     (map
      (fn [interactor]
        (cond
         (string? interactor)
         interactor

         :default
         (:label (pack-obj (entity db interactor))))))
     (sort)
     (str/join " : "))))
                  
                  
        

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
   "anatomy-term"

   :default
   (if-let [k (first (filter #(= (name %) "id") (keys obj)))]
     (namespace k))))

(defn pack-obj
  ([obj]
     (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
   (if obj
     {:id       ((keyword class "id") obj)
      :label    (or label
                  (obj-label class obj))
      :class    (str/replace class "-" "_")
      :taxonomy "c_elegans"})))

(defn get-evidence [holder]
  (vmap-if
   :Inferred_automatically
   (seq
    (:evidence/inferred-automatically holder))

   :Curator_confirmed
   (seq
    (for [person (:evidence/curator-confirmed holder)]
      (pack-obj "person" person)))

   :Person_evidence
   (seq
    (for [person (:evidence/person-evidence holder)]
      (pack-obj "person" person)))
   
   :Paper_evidence
   (seq
    (for [paper (:evidence/paper-evidence holder)]
      (pack-obj "paper" paper)))

   :Date_last_updated
   (:evidence/date-last-updated holder)
   
   :Remark
   (seq
    (:evidence/remark holder))))

(defn humanize-ident [ident]
  (if ident
    (-> (name ident)
        (str/split #":")
        (last)
        (str/replace #"-" " ")
        (str/capitalize))))
