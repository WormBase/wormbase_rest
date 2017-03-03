(ns rest-api.classes.gene.widgets.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.gene.generic :as generic]))

(defn- expr-pattern-type [expr-pattern]
  (let [type-keys
        [:expr-pattern/antibody
         :expr-pattern/cis-regulatory-element
         :expr-pattern/epic
         :expr-pattern/genome-editing
         :expr-pattern/in-situ
         :expr-pattern/localizome
         :expr-pattern/microarray
         :expr-pattern/northern
         :expr-pattern/reporter-gene
         :expr-pattern/rnaseq
         :expr-pattern/rt-pcr
         :expr-pattern/tiling-array
         :expr-pattern/western]]
    (->> (filter #(% expr-pattern) type-keys)
         (map obj/humanize-ident))))

(defn- short-term-list [expr-pattern terms]
  (let [capacity 5
        size (count terms)]
    (if (> size capacity)
      (concat (map pack-obj (take capacity terms))
              [(assoc (pack-obj expr-pattern)
                   :label (format "<strong>and %s more</strong>" (- size capacity)))])
      (map pack-obj terms))))

(defn- expr-pattern-detail [expr-pattern qualifier]
  (pace-utils/vmap
   :Type (seq (expr-pattern-type expr-pattern))
   :Paper (if-let [paper-holders (:expr-pattern/reference expr-pattern)]
            (->> paper-holders
                 (map :expr-pattern.reference/paper)
                 (map pack-obj)))
   :Expressed_in (->> (:qualifier/anatomy-term qualifier)
                      (short-term-list expr-pattern)
                      (seq))
   :Expressed_during (->> (:qualifier/life-stage qualifier)
                          (short-term-list expr-pattern)
                          (seq))

   ))

(defn- pack-image [picture]
  (let [prefix (if (re-find #"<Journal_URL>" (or (:picture/acknowledgement-template picture) ""))
                 (:paper/id (first (:picture/reference picture)))
                 (:person/id (first (:picture/contact picture))))
        [_ picture-name format-name] (re-matches #"(.+)\.(.+)" (:picture/name picture))]
    (-> picture
        (pack-obj)
        (assoc :thumbnail
               {:format (or format-name "")
                :name (str prefix "/" (or picture-name (:picture/name picture)))
                :class "/img-static/pictures"}))))

(defn- expression-table-row [db [ontology-term-dbid expr-pattern-dbid qualifier-dbid]]
  (let [ontology-term (d/entity db ontology-term-dbid)
        expr-pattern (d/entity db expr-pattern-dbid)
        qualifier (d/entity db qualifier-dbid)]
    {:ontology_term (pack-obj ontology-term)

     :expression_pattern
     (if-let [packed-images (->> (:picture/_expr-pattern expr-pattern)
                                 (map pack-image)
                                 (seq))]
       (assoc (pack-obj expr-pattern) :curated_images packed-images)
       (pack-obj expr-pattern))

     :details
     {:evidence (expr-pattern-detail expr-pattern qualifier)}}))

(defn expressed-in [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [anatomy-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?ep :expr-pattern/anatomy-term ?th]
                  [?th :expr-pattern.anatomy-term/anatomy-term ?t]]
                db (:db/id gene))]
       (seq (map #(expression-table-row db %) anatomy-relations)))
     :description "the tissue that the gene is expressed in"}))

(defn expressed-during [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [life-stage-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?ep :expr-pattern/life-stage ?th]
                  [?th :expr-pattern.life-stage/life-stage ?t]]
                db (:db/id gene))]
       (seq (map #(expression-table-row db %) life-stage-relations)))
     :description "the tissue that the gene is expressed in"}))

(defn subcellular-localization [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [go-term-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?ep :expr-pattern/go-term ?th]
                  [?th :expr-pattern.go-term/go-term ?t]]
                db (:db/id gene))]
       (seq (map #(expression-table-row db %) go-term-relations)))
     :description "the tissue that the gene is expressed in"}))

(def widget
  {:name generic/name-field
   :expressed_in expressed-in
   :expressed_during expressed-during
   :subcellular_localization subcellular-localization
   }
  )
