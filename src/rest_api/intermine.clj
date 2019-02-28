(ns rest-api.intermine
  (:require
    [clojure.string :as str]
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :as sweet]
    [rest-api.db.main :refer [datomic-conn]]
    [datomic.api :as d]))

(def q-anatomy-term
  '[:find  [?at ...]
    :in $
    :where [?at :anatomy-term/id]])

(def q-rnai
  '[:find  [?rnai ...]
    :in $
    :where [?rnai :rnai/id]])

(def q-cds
  '[:find  [?cds ...]
    :in $
    :where [?cds :cds/id]])

(def q-expression-cluster
  '[:find  [?expression-cluster ...]
    :in $
    :where [?expression-cluster :expression-cluster/id]])

(def q-expr-pattern
  '[:find  [?expr-pattern ...]
    :in $
    :where [?expr-pattern :expr-pattern/id]])

(def q-laboratory
  '[:find  [?lab ...]
    :in $
    :where [?lab :laboratory/id]])

(def q-life-stage
  '[:find  [?ls ...]
    :in $
    :where [?ls :life-stage/id]])

(def q-phenotype
  '[:find  [?ls ...]
    :in $
    :where [?ls :phenotype/id]])

(def q-genes
  '[:find  [?gene ...]
    :in $
    :where [?gene :gene/id]])

(def q-gene-class
  '[:find  [?gc ...]
    :in $
    :where [?gc :gene-class/id]])

(def q-protein
  '[:find  [?p ...]
    :in $
    :where [?p :protein/id]])

(def q-species
  '[:find  [?s ...]
    :in $
    :where [?s :species/id]])

(def q-strain
  '[:find  [?s ...]
    :in $
    :where [?s :strain/id]])

(def q-transcript
  '[:find  [?t ...]
    :in $
    :where [?t :transcript/id]])

(defn get-transcript []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-transcript db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (str "Transcript:" (:transcript/id obj))
                   :symbol (:transcript/id obj)
                   :method (some->> obj :locatable/method :method/id)
                   :organism.name (->> obj :transcript/species :species/id)
                   :gene.primaryIdentifier (some->> (:gene/corresponding-transcript/_transcript obj)
                                                    (map :gene/_corresponding-transcript)
                                                    (map :gene/id))
                   :CDSs.primaryIdentifier (->> obj
                                                :transcript/corresponding-cds
                                                :transcript.corresponding-cds/cds
                                                :cds/id)})))
         (seq))))

(defn get-strain []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-strain db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:strain/id obj)
                   :genotype (:strain/genotype obj)
                   :otherName (some->> (:strain/other-name obj)
                                       (map :strain.other-name/text))
                   :gene.primaryIdentifier (some->> (:gene/_strain obj)
                                                    (map :gene/id))
                   :inbreedingState (when-let [state (:strain/inbreeding-state obj)]
                                      (name state))
                   :outcrossed (:strain/outcrossed obj)
                   :mutagen (:strain/mutagen obj)
                   :strainHistory (:strain/strain-history obj)
                   :dateFirstFrozen (:strain/date-first-frozen obj)
                   :CGCReceived (:strain/cgc-received obj)
                   :laboratory (some->> (:strain/location obj)
                                        (map :strain.location/laboratory)
                                        (map :laboratory/id))
                   :madeBy (some->> (:strain/made-by obj)
                                    (map :person/id))
                   :remark (some->> (:strain/remark obj)
                                    (map :strain.remark/text))
                   :species (->> obj :strain/species :species/id)
                   :ncbiTaxonomyID (->> obj :strain/species :species/ncbi-taxonomy)})))
         (seq))))

(defn get-species []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-species db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:name (:species/id obj)
                   :taxonId (:species/ncbi-taxonomy obj)})))
         (seq))))

(defn get-protein []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-protein db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:protein/id obj)
                   :symbol (:protein/gene-name obj)
                   :molecularWeight (->> obj :protein/molecular-weight :protein.molecular-weight/float)
                   :organism.name (->> obj :protein/species :species/id)
                   :CDSs.primaryIdentifier (some->> (:cds.corresponding-protein/_protein obj)
                                                    (map :cds/_corresponding-protein)
                                                    (map :cds/id))
                   :motifs.primaryIdentifier nil ; requires homology
                   })))
         (seq))))

(defn get-phenotype []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-phenotype db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:identifier (:phenotype/id obj)
                   :synonym (some->> (:phenotype/synonym obj)
                                     (map :phenotype.synonym/text))
                   :PhenotypeParents (some->> (:phenotype/specialisation-of obj)
                                              (map :phenotype/id))
                   :phenotypeChildren (some->> (:phenotype/_specialisation-of obj)
                                               (map :phenotype/id))})))
         (seq))))

(defn get-life-stage []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-life-stage db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:life-stage/id obj)
                   :definition (:life-stage/definition obj)
                   :publicName (:life-stage/public-name obj)
                   :remark (some->> (:life-stage/remark obj)
                                    (map :life-stage.remark/text))
                   :otherName nil
                   :containedIn.primaryIdentifier nil
                   :precededBy.primaryIdentifier (some->> (:life-stage/preceded-by obj)
                                                          (map :life-stage/id))
                   :followedBy.primaryIdentifier (some->> (:life-stage/_preceded-by obj)
                                                          (map :life-stage/id))
                   :subStages.primaryIdentifier (some->> (:life-stage/_contained-in obj)
                                                         (map :life-stage/id))
                   :anatomyTerms.primaryIdentifier (some->> (:life-stage/anatomy-term obj)
                                                            (map :anatomy-term/id))
                   :expressionPatterns.primaryIdentifier (some->> (:expr-pattern.life-stage/_life-stage obj)
                                                                  (map :expr-pattern/_life-stage)
                                                                  (map :expr-pattern/id))})))
         (seq))))

(defn get-laboratory []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-laboratory db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:laboratory/id obj)})))
         (seq))))

(defn get-expr-pattern []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-expr-pattern db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:expr-pattern/id obj)
                   :subcellularLocalization (:expr-pattern/subcellular-localization obj)
                   :pattern (:expr-pattern/pattern obj)
                   :remark (some->> (:expr-pattern/remark obj)
                                    (map :expr-pattern.remark/text))
                   :reporterGene (:expr-pattern/reporter-gene obj)
                   :gene.primaryIdentifier (some->> (:expr-pattern/reflects-endogenous-expression-of obj)
                                                    (map :gene/id))
                   :anatomyTerms.primaryIdentifier (some->> (:expr-pattern/anatomy-term obj)
                                                            (map :expr-pattern.anatomy-term/anatomy-term)
                                                            (map :anatomy-term/id))
                   :lifeStages.primaryIdentier (some->> (:expr-pattern/life-stage obj)
                                                        (map :expr-pattern.life-stage/life-stage)
                                                        (map :life-stage/id))
                   :GOTerms.identifier (some->> (:expr-pattern/go-term obj)
                                                (map :expr-pattern.go-term/go-term)
                                                (map :go-term/id))})))
         (seq))))

(defn get-expression-cluster []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-expression-cluster db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:expression-cluster/id obj)
                   :description (first (:expression-cluster/description obj))
                   :algorithm (first (:expression-cluster/algorithm obj))
                   :regulatedByTreatment (:expression-cluster/regulated-by-treatment obj)
                   :genes.primaryIdentifier (some->> (:expression-cluster/gene obj)
                                                     (map :expression-cluster.gene/gene)
                                                     (map :gene/id))
                   :regulatedByGene.primaryIdentifier (some->> (:expression-cluster/regulated-by-gene obj)
                                                               (map :gene/id))
                   :regulatedByMolucule.PrimaryIdentifier (some->> (:expression-cluster/regulated-by-molecule obj)
                                                                   (map :molecule/id))
                   :lifeStages.primaryIdentifier (some->> (:expression-cluster/life-stage obj)
                                                          (map :life-stage/id))
                   :anatomyTerms.primaryIdentifier (some->> (:expression-cluster/anatomy-term obj)
                                                            (map :expression-cluster.anatomy-term/anatomy-term)
                                                            (map :anatomy-term/id))
                   :processes.primaryIdentifier (some->> (:wbprocess.expression-cluster/_expression-cluster obj)
                                                         (map :wbprocess/_expression-cluster)
                                                         (map :wbprocess/id))})))
         (seq))))

(defn get-cds []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-cds db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (str "CDS:" (:cds/id obj))
                   :symbol (:cds/id obj)
                   :organism.name (->> obj :cds/species :species/id)
                   :gene.primaryIdentifier (some->> (:gene.corresponding-cds/_cds obj)
                                                    (map :gene/_corresponding-cds)
                                                    (map :gene/id))
                   :protein.primaryIdentifier (->> obj
                                                   :cds/corresponding-protein
                                                   :cds.corresponding-protein/protein
                                                   :protein/id)
                   :transcripts.primaryIdentifier (some->> (:gene.corresponding-cds/_cds obj)
                                                           (map :gene/_corresponding-cds)
                                                           (map (fn [g]
                                                                  (some->> (:gene/corresponding-transcript g)
                                                                           (map :gene.corresponding-transcript/transcript)
                                                                           (map :transcript/id))))
                                                           (into [])
                                                           (distinct)
                                                           (remove nil?))})))
         (seq)
         )))

(defn get-anatomy-term []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-anatomy-term db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:anatomy-term/id obj)
                   :name (->> obj :anatomy-term/term :anatomy-term.term/text)
                   :definition (->> obj :anatomy-term/definition :anatomy-term.definition/text)
                   :synonym (some->> (:anatomy-term/synonym obj)
                                     (map :anatomy-term.synonym/text))
                   :parents (->> obj :anatomy-term/parent-term :anatomy-term/id)
                   :children (some->> (:anatomy-term/_parent-term obj)
                                      (map :anatomy-term/id))})))
         (seq))))

(defn get-rnai []
  (let [db (d/db datomic-conn)]
    (->> (d/q q-rnai db)
         (map (fn [id]
                (let [obj (d/entity db id)]
                  {:primaryIdentifier (:rnai/id obj)
                   :secondaryIdentifier (:rnai/history-name obj)
                   :DNA (some->> (:rnai/dna-text obj)
                                 (map :rnai.dna-text/name))
                   :if.uniquelyMapped (if (:rnai/uniquely-mapped obj)
                                        true
                                        false)
                   :phenotypeRemark (some->> (:rnai/phenotype obj)
                                             (map :phenotype-info/remark)
                                             (map (fn [pi]
                                                   (some->> pi
                                                            (map :phenotype-info.remark/text))))
                                             (remove nil?)
                                             (flatten))
                   :remark (some->> (:rnai/remark obj)
                                    (map :rnai.remark/text))
                   :inhibitsGene (some->> (:rnai/gene obj)
                                          (map :rnai.gene/gene)
                                          (map :gene/id))
                   :inhibitsPredictedGene (some->> (:rnai/predicted-gene obj)
                                                   (map :rnai.predicted-gene/cds)
                                                   (map :cds/id))
                   :organism (->> obj :rnai/species :species/id)
                   :phenotype (some->> (:rnai/phenotype obj)
                                       (map :rnai.phenotype/phenotype)
                                       (map :phenotype/id))
                   :phenotype_not_observed (some->> (:rnai/phenotype-not-observed obj)
                                                    (map :rnai.phenotype-not-observed/phenotype)
                                                    (map :phenotype/id))
                   :laboratories (some->> (:rnai/laboratory obj)
                                          (map :laboratory/id))
                   :stain (->> obj :rnai/strain :strain/id)
                   :lifeStage (->> obj :rnai/life-stage :life-stage/id) ; This is different from what is in the XML query
                   :reference (->> obj :rnai/reference :rnai.reference/paper :paper/id)})))
         (seq))))

(defn get-gene []
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

(defn anatomy-term-handler [request]
 (ok {:data (get-anatomy-term)}))

(defn rnai-handler [request]
 (ok {:data (get-rnai)}))

(defn cds-handler [request]
 (ok {:data (get-cds)}))

(defn expression-cluster-handler [request]
 (ok {:data (get-expression-cluster)}))

(defn expr-pattern-handler [request]
 (ok {:data (get-expr-pattern)}))

(defn gene-handler [request]
 (ok {:data (get-gene)}))

(defn gene-class-handler [request]
 (ok {:data (get-gene-class)}))

(defn laboratory-handler [request]
 (ok {:data (get-laboratory)}))

(defn life-stage-handler [request]
 (ok {:data (get-life-stage)}))

(defn phenotype-handler [request]
(ok {:data (get-phenotype)}))

(defn protein-handler [request]
(ok {:data (get-protein)}))

(defn species-handler [request]
(ok {:data (get-species)}))

(defn strain-handler [request]
(ok {:data (get-strain)}))

(defn transcript-handler [request]
(ok {:data (get-transcript)}))


(def routes
  [(sweet/GET "/intermine/rnai" [] :tags ["intermine"] rnai-handler)
   (sweet/GET "/intermine/anatomy-term" [] :tags ["intermine"] anatomy-term-handler)
   (sweet/GET "/intermine/cds" [] :tags ["intermine"] cds-handler)
   (sweet/GET "/intermine/expression-cluster" [] :tags ["intermine"] expression-cluster-handler)
   (sweet/GET "/intermine/expr-pattern" [] :tags ["intermine"] expr-pattern-handler)
   (sweet/GET "/intermine/laboratory" [] :tags ["intermine"] laboratory-handler)
   (sweet/GET "/intermine/life-stage" [] :tags ["intermine"] life-stage-handler)
   (sweet/GET "/intermine/phenotype" [] :tags ["intermine"] phenotype-handler)
   (sweet/GET "/intermine/gene" [] :tags ["intermine"] gene-handler)
   (sweet/GET "/intermine/species" [] :tags ["intermine"] species-handler)
   (sweet/GET "/intermine/protein" [] :tags ["intermine"] protein-handler)
   (sweet/GET "/intermine/strain" [] :tags ["intermine"] strain-handler)
   (sweet/GET "/intermine/transcript" [] :tags ["intermine"] transcript-handler)
   (sweet/GET "/intermine/gene-class" [] :tags ["intermine"] gene-class-handler)])
