(ns rest-api.classes.laboratory.widgets.alleles
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn alleles [laboratory]
  (let [db (d/entity-db laboratory)
        data (->> (d/q '[:find [?varent ...]
                         :in $ ?laboratory
                         :where 
                         [?laboratory :laboratory/alleles ?varent] ]
                       db (:db/id laboratory))
                  (map (fn [wbvar]
                         (let [allele (d/entity db wbvar)]
                           (pace-utils/vmap
                             :sequenced 
                             (if (= (str (:variation/seqstatus allele))
                                    ":variation.seqstatus/sequenced") "yes" "no")
                             :type 
                             (if (:method/id (:locatable/method allele))
                               (-> (:method/id (:locatable/method allele))
                                   (str/replace #"_allele" "")
                                   (str/replace #"Allele" "")))
                             :allele (pack-obj "variation" allele)
                             :gene 
                             (first (->> (d/q '[:find [?gene ...]
                                                :in $ ?variation
                                                :where 
                                                [?variation :variation/gene ?vargeneent]
                                                [?vargeneent :variation.gene/gene ?gene]]
                                              db wbvar)
                                         (map (fn [geneobj]
                                                (let [gene (d/entity db geneobj)]
                                                  (pack-obj "gene" gene))))))))))
                  (seq))]
    {:data data
     :description "gene classes assigned to laboratory"}))

(def widget
  {:alleles alleles})
