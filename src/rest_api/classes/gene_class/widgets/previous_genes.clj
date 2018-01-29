(ns rest-api.classes.gene-class.widgets.previous-genes
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def q-former-genes-other-name
  '[:find [?gene ...]
    :in $ ?name
    :where [?gene :gene/other-name ?h]
           [?h :gene.other-name/text ?name]])

(def q-former-genes-public-name
  '[:find [?gene ...]
    :in $ ?name
    :where [?gene :gene/public-name ?name]])

(def q-reassigned-genes
  '[:find [?gene ...]
    :in $ ?name
    :where [?gene :gene/other-name ?h]
           [?h :gene.other-name/text ?on]
           [(str "(" ?name ".*)") ?matcher]
           [(re-pattern ?matcher) ?regex]
           [(re-find ?regex ?on)]])

(defn stash-former-member [gene-name old-gene]
  {:former_name gene-name
   :new_name (pack-obj old-gene)
   :species (when-let [species-name (:species/id (:gene/species old-gene))]
              {:id species-name
               :label species-name
               :class "species"
               :taxonomy "all"})
   :sequence (when-let [sequence-name (:gene/sequence-name old-gene)]
               {:id sequence-name
                :label sequence-name
                :class "gene_name"
                :taxonomy "all"})
   :species-name (:species/id (:gene/species old-gene))
   :reason "reassigned to new class"})

(defn reassigned-genes [g]
  {:data (let [db (d/entity-db g)
                 gene-name (:gene-class/id g)]
           (not-empty
             (some->> (d/q q-reassigned-genes db gene-name)
                      (map
                        (fn [id]
                          (let [gene (d/entity db id)
                                public-name (:gene/public-name gene)]
                            (some->> (:gene/other-name gene)
                                     (map :gene.other-name/text)
                                     (filter #(.contains % public-name))
                                     (map #(stash-former-member % gene))))))
                      (flatten)
                      (group-by :species-name))))
   :description "genes that have been reassigned a new name in the same class"})

(defn former-genes [g]
  {:data (let [db (d/entity-db g)]
           (some->> (:gene-class/old-member g)
                    (map
                      (fn [gene-name]
                        (some->> (or
                                   (d/q q-former-genes-other-name db gene-name)
                                   (d/q q-former-genes-public-name db gene-name))
                                 (map #(d/entity db %))
                                 (map #(stash-former-member gene-name %)))))
                    (flatten)
                    (group-by :species-name)))
   :description "genes formerly in the class that have been reassigned to a new class"})

(def widget
  {:name generic/name-field
   :reassigned_genes reassigned-genes
   :former_genes former-genes})
