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

(defn- expr-pattern-detail [expr-pattern qualifier]
  (pace-utils/vmap
   :Type (seq (expr-pattern-type expr-pattern))
   :Expression_pattern (pack-obj expr-pattern)
   :Paper (if-let [paper-holders (:expr-pattern/reference expr-pattern)]
            (->> paper-holders
                 (map :expr-pattern.reference/paper)
                 (map pack-obj)))
   :Expressed_in (->> (:qualifier/anatomy-term qualifier)
                      (map pack-obj)
                      (seq))
   :Expressed_during (->> (:qualifier/life-stage qualifier)
                          (map pack-obj)
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
     (assoc {}
            :curated_images
            (->> (:picture/_expr-pattern expr-pattern)
                 (map pack-image)
                 (seq)))

     :details
     {:evidence (expr-pattern-detail expr-pattern qualifier)}}))

(defn expressed-in [gene]
  (let [db (d/entity-db gene)]
    {:data
     (if-let [anatomy-relations
              (d/q '[:find ?at ?ep ?ah
                     :in $ ?gene
                     :where
                     [?gh :expr-pattern.gene/gene ?gene]
                     [?ep :expr-pattern/gene ?gh]
                     [?ep :expr-pattern/anatomy-term ?ah]
                     [?ah :expr-pattern.anatomy-term/anatomy-term ?at]]
                   db (:db/id gene))]
       (map #(expression-table-row db %) anatomy-relations))
     :description "the tissue that the gene is expressed in"}))

(defn expressed-during [gene]
  (let [db (d/entity-db gene)]
    {:data
     (if-let [life-stage-relations
              (d/q '[:find ?at ?ep ?ah
                     :in $ ?gene
                     :where
                     [?gh :expr-pattern.gene/gene ?gene]
                     [?ep :expr-pattern/gene ?gh]
                     [?ep :expr-pattern/life-stage ?ah]
                     [?ah :expr-pattern.life-stage/life-stage ?at]]
                   db (:db/id gene))]
       (map #(expression-table-row db %) life-stage-relations))
     :description "the tissue that the gene is expressed in"}))



(def widget
  {:name generic/name-field
   :expressed_in expressed-in
   :expressed_during expressed-during
   }
  )
