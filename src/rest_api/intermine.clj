(ns rest-api.intermine
  (:require
    [clojure.string :as str]
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :as sweet]
    [rest-api.db.main :refer [datomic-conn]]
    [datomic.api :as d]))

(def q-genes
  '[:find  [?gene ...]
    :in $
    :where [?gene :gene/id]
           [?gene :gene/strain]
          ])

(def q-gene-class
  '[:find  [?gc ...]
    :in $
    :where [?gc :gene-class/id]])

(defn get-genes []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-genes db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:gene/id obj)
                   :secondaryIdentifier (:gene/sequence-name obj)
                   :symbol (:gene/public-name obj)
                   :name (:gene/public-name obj)
                   :operon (some->> (:operon.contains-gene/_gene obj)
                                    (map :operon/_contains-gene)
                                    (map :operon/id))
                   :biotype (->> obj
                                 :gene/biotype
                                 :so-term/id)
                   :lastUpdated (->> obj ; this is never populated as far as I can tell
                                     :gene/evidence
                                     :evidence/date-last-updated)
                   :briefDescription (some->> (:gene/concise-description obj)
                                              (map :gene.concise-description/text)
                                              (first))
                   :description (some->> (:gene/automated-description obj) ;e.g. WBGene00105325
                                         (map :gene.automated-description/text))
                   :organism_name (->> obj
                                       :gene/species
                                       :species/id)
                   :transcript.primaryIdentifier (some->> (:gene/corresponding-transcript obj)
                                                          (map :gene.corresponding-transcript/transcript)
                                                          (map :transcript/id))
                   :variations.primaryIdentifier (some->> (:gene/reference-allele obj) ;e.g. WBGene00002363
                                                       (map :gene.reference-allele/variation)
                                                       (map :variation/id))
                   :CDSs.primaryIdenfier (some->> (:gene/corresponding-cds obj)
                                                  (map :gene.corresponding-cds/cds)
                                                  (map :cds/id))

                   :strains.primaryIdentifier (some->> (:gene/strain obj)
                                                       (map :strain/id))})))
         (seq))))

(defn my-flatten [coll]
  (lazy-seq
    (when-let [s  (seq coll)]
      (if (coll? (first s))
        (concat (flatten (first s)) (flatten (rest s)))
        (cons (first s) (flatten (rest s)))))))

(defn get-gene-class []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-gene-class db)
      (map (fn [id]
        (let [obj (d/entity db id)]
          {:primaryIdentifier (:gene-class/id obj)
           :description (:gene-class/description obj) ;e.g.
           :designatingLaboratory (some->> (:gene-class/designating-laboratory obj) ;e.g. WBGene00002363
                                           (:laboratory/id))
           :formerDesignatingLaboratory (some->> (:gene-class/former-designating-laboratory obj) ;e.g. WBGene00040036
                                                 (sort-by :gene-class.former-designating-laboratory/until)
                                                 (first)
                                                 :gene-class.former-designating-laboratory/laboratory
                                                 :laboratory/id)
           :variations (some->> (:variation/_gene-class obj)
                                (map :variation/id))
           :genes (some->> (:gene/_gene-class obj)
                           (map :gene/id))
           :formerGenes (some->> (:gene-class/old-member obj))
           :mainName (some->> (:gene-class/main-name obj)
                              (first)
                              (:gene-class/id))
           :otherName (some->> (:gene-class/other-name obj) ; I don't see this field in the database
                               (first)
                               (:gene-class/id))
           :remark (some->> (:gene-class/remark obj)
                            (map :gene-class.remark/text)
                            (first))})))
	 (seq))))

(defn gene-handler [request]
 (ok {:data (get-genes)}))

(defn gene-class-handler [request]
 (ok {:data (get-gene-class)}))

(def routes
  [(sweet/GET "/intermine/gene" [] :tags ["intermine"] gene-handler)
   (sweet/GET "/intermine/gene-class" [] :tags ["intermine"] gene-class-handler)])
