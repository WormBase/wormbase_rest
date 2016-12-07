(ns datomic-rest-api.rest.gene
  (:require [cheshire.core :as c :refer (generate-string)]
            [pseudoace.binning :refer (reg2bins xbin bins)]
            [datomic-rest-api.helpers :refer(format-date)]
            [datomic-rest-api.rest.object :refer (humanize-ident get-evidence author-list)]
            [datomic.api :as d :refer (db q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap vmap-if vassoc cond-let those conjv]]
            [pseudoace.locatables :refer (root-segment)]
            [datomic-rest-api.rest.core :refer [def-rest-widget]]
            ))

;;
;; "name" field, included on all widgets.
;;

(defn- name-field [gene]
  (let [data (datomic-rest-api.rest.object/pack-obj "gene" gene)]
    {:data (if (empty? data) nil data)
     :description (format "The name and WormBase internal ID of %s" (:gene/id gene))}))

;;
;; Overview widget
;;

(def ^:private transcript-types
  {:transcript/asrna             "asRNA"
   :transcript/lincrna           "lincRNA"

   :transcript/processed-mrna    "mRNA"
   :transcript/unprocessed-mrna  "mRNA"
   :transcript/mirna             "miRNA"
   :transcript/ncrna             "ncRNA"
   :transcript/pirna             "piRNA"
   :transcript/rrna              "rRNA"
   :transcript/scrna             "scRNA"
   :transcript/snorna            "snoRNA"
   :transcript/snrna             "snRNA"
   :transcript/snlRNA            "snlRNA"
   :transcript/stRNA             "stRNA"
   :transcript/tRNA              "tRNA"})

(defn- transcript-type [transcript]
  (some transcript-types (keys transcript)))

(defn- gene-classification [gene]
 (let [data
   (let [db   (d/entity-db gene)
         cds  (:gene/corresponding-cds gene)
         data {:defined_by_mutation (if (not (empty? (:variation.gene/_gene gene))) 1 0)
               :type (cond
                      ;; This is pretty-much the reverse order of the Perl code
                      ;; because we never over-write anything
                      (q '[:find ?trans .
                           :in $ ?gene
                           :where [?gene :gene/version-change ?hist]
                                  [?hist :gene-history-action/transposon-in-origin ?trans]]
                         db (:db/id gene))
                      "Transposon in origin"

                      (:gene/corresponding-pseudogene gene)
                      "pseudogene"

                      cds
                      "protein coding"

                      :default
                      (some #(transcript-type (:gene.corresponding-transcript/transcript %))
                            (:gene/corresponding-transcript gene)))
               :associated_sequence (if (not (empty? cds)) 1 0)
               :confirmed (if (q '[:find ?conf-gene .
                                   :in $ ?conf-gene
                                   :where [?conf-gene :gene/corresponding-cds ?gc]
                                          [?gc :gene.corresponding-cds/cds ?cds]
                                          [?cds :cds/prediction-status :cds.prediction-status/confirmed]]
                                 db (:db/id gene))
                            "Confirmed")}]
     (assoc data
       :prose_description
       (str/join " "
         (those
          (cond
           (:associated_sequence data)
           "This gene is known only by sequence.")

          (cond
           (= (:confirmed data) "Confirmed")
           "Gene structures have been confirmed by a curator."

           (:gene/matching-cdna gene)
           "Gene structures have been confirmed by matching cDNA."

           :default
           "Gene structures have not been confirmed.")))))]
  {:data (if (empty? data) nil data)
   :description "gene type and status"}))

(defn- gene-class [gene]
  {:data
   (if-let [class (:gene/gene-class gene)]
     {:tag (datomic-rest-api.rest.object/pack-obj "gene-class" class)
      :description (str (first (:gene-class/description class)))})
   :description "The gene class for this gene"})

(defn- gene-operon [gene]
  {:data
   (if-let [operon (->> (:operon.contains-gene/_gene gene)
                        (first)
                        (:operon/_contains_gene))]
     (datomic-rest-api.rest.object/pack-obj "operon" operon))
   :description "Operon the gene is contained in"})

(defn- gene-cluster [gene]
   {:data
     (if-let [data (->> (:gene/main-name/text gene))] data)
    :description "The gene cluster for this gene"})

(defn- gene-other-names [gene]
   {:data (if-let [data (map #(get % "gene.other-name/text") (:gene/other-name gene))]
             data)
    :description (format "other names that have been used to refer to %s" (:gene/id gene))})

(defn- gene-status [gene]
  {:data (if-let [class (:gene/status gene)]
           (:status/status class))
   :description (format "current status of the Gene:%s %s" (:gene/id gene) "if not Live or Valid")})

(defn- gene-taxonomy [gene]
  {:data (if-let [class (:gene/species gene)]
           (if-let [[_ genus species] (re-matches #"^(.*)\s(.*)$" (:species/id class))]
             {:genus genus :species species}
             {:genus (:gene/species gene)}))
   :description "the genus and species of the current object"})

(defn- concise-description [gene]
  {:data
   (if-let [desc (or (first (:gene/concise-description gene))
                     (first (:gene/automated-description gene))
                     (->> (:gene/corresponding-cds gene)
                          (first)
                          (:cds/brief-identification))
                     (->> (:gene/corresponding-transcript gene)
                          (first)
                          (:transcript/brief-identification)))]
     {:text (some (fn [[k v]] (if (= (name k) "text") v)) desc)
      :evidence (or (datomic-rest-api.rest.object/get-evidence desc)
                    (datomic-rest-api.rest.object/get-evidence (first (:gene/provisional-description gene))))}
     {:text nil :evidence nil})
   :description "A manually curated description of the gene's function"})

(defn- curatorial-remarks [gene]
  (let [data
        (->> (:gene/remark gene)
             (map (fn [rem]
                    {:text (:gene.remark/text rem)
                     :evidence (datomic-rest-api.rest.object/get-evidence rem)}))
             (seq))]
    {:data (if (empty? data) nil data)
     :description "curatorial remarks for the Gene"}))

(defn- legacy-info [gene]
  {:data
   (if-let [data (seq (map :gene.legacy-information/text (:gene/legacy-information gene)))] data)
   :description
   "legacy information from the CSHL Press C. elegans I/II books"})

(defn- named-by [gene]
  {:data
   (if-let [data (->> (:gene/cgc-name gene)
                      (datomic-rest-api.rest.object/get-evidence)
                      (mapcat val))]
     data)
   :description
   "the source where the approved name was first described"})

(defn- parent-sequence [gene]
  {:data
   (if-let [data (datomic-rest-api.rest.object/pack-obj (:locatable/parent gene))]
     data)
   :description
   "parent sequence of this gene"})

(defn- parent-clone [gene]
  (let [db (d/entity-db gene)
        data
        (->> (q '[:find [?clone ...]
                  :in $ ?gene
                  :where [?cg :clone.positive-gene/gene ?gene]
                         [?clone :clone/positive-gene ?cg]]
                db (:db/id gene))
             (map (fn [cid]
                    (let [clone (entity db cid)]
                      (datomic-rest-api.rest.object/pack-obj "clone" clone))))
             (seq))]
    {:data (if (empty? data) nil data)
     :description
     "parent clone of this gene"}))

(defn- cloned-by [gene]
  {:data
   (if-let [ev (datomic-rest-api.rest.object/get-evidence (first (:gene/cloned-by gene)))]
     {:cloned_by (key (first ev))
      :tag       (key (first ev))
      :source    (first (val (first ev)))})
   :description
   "the person or laboratory who cloned this gene"})

(defn- transposon [gene]
  {:data
   (if-let [data (datomic-rest-api.rest.object/pack-obj (first (:gene/corresponding-transposon gene)))]
     data)
   :description
   "Corresponding transposon for this gene"})

(defn- sequence-name [gene]
  {:data
   (or (:gene/sequence-name gene)
       "unknown")
   :description
   "the primary corresponding sequence name of the gene, if known"})

(defn- locus-name [gene]
  {:data
   (if-let [cgc (:gene/cgc-name gene)]
     (datomic-rest-api.rest.object/pack-obj "gene" gene :label (:gene.cgc-name/text cgc))
     "not assigned")
   :description "the locus name (also known as the CGC name) of the gene"})

(defn- disease-relevance [gene]
  {:data (if-let [data
                  (->> (:gene/disease-relevance gene)
                       (map (fn [rel]
                              {:text (:gene.disease-relevance/note rel)
                               :evidence (datomic-rest-api.rest.object/get-evidence rel)}))
                       (seq))]
           data)
   :description
   "curated description of human disease relevance"})

(defn- gene-version [gene]
  (let [data (str (:gene/version gene))]
    {:data (if (empty? data) nil data)
     :description "the current WormBase version of the gene"}))

(defn- also-refers-to [gene]
  (let [db (d/entity-db gene)]
    {:data (if-let [data
                    (->>
                      (q '[:find [?other-gene ...]
                           :in $ ?gene
                           :where [?gene :gene/cgc-name ?cgc]
                                  [?cgc :gene.cgc-name/text ?cgc-name]
                                  [?other-name :gene.other-name/text ?cgc-name]
                                  [?other-gene :gene/other-name ?other-name]]
                         db (:db/id gene))
                      (map #(datomic-rest-api.rest.object/pack-obj "gene" (entity db %)))
                      (seq))]
             data)
     :description
     "other genes that this locus name may refer to"}))

(defn- merged-into [gene]
  (let [db (d/entity-db gene)
        data
        (->> (q '[:find ?merge-partner .
                  :in $ ?gene
                  :where [?gene :gene/version-change ?vc]
                         [?vc :gene-history-action/merged-into ?merge-partner]]
                db (:db/id gene))
             (entity db)
             (datomic-rest-api.rest.object/pack-obj "gene"))]
    {:data (if (empty? data) nil data)
     :description "the gene this one has merged into"}))

(defn- get-structured-description [gene type]
  (let [key     (keyword "gene" type)
        txt-key (keyword (str "gene." type) "text")]
    (->> (key gene)
         (map (fn [data]
                {:text     (txt-key data)
                 :evidence (datomic-rest-api.rest.object/get-evidence data)}))
         (seq))))

(defn- structured-description [gene]
  (let [data (vmap
               :Provisional_description
               (let [cds (->> (:gene/concise-description gene)
                              (map :gene.concise-description/text)
                              (set))]
                 (seq
                   (for [p (:gene/provisional-description gene)
                         :let [txt (:gene.provisional-description/text p)]
                         :when (not (cds txt))]
                     {:text txt
                      :evidence (datomic-rest-api.rest.object/get-evidence p)})))

               :Other_description
               (get-structured-description gene "other-description")

               :Sequence_features
               (get-structured-description gene "sequence-features")

               :Functional_pathway
               (get-structured-description gene "functional-pathway")

               :Functional_physical_interaction
               (get-structured-description gene "functional-physical-interaction")

               :Molecular_function
               (get-structured-description gene "molecular-function")

               :Biological_process
               (get-structured-description gene "biological-process")

               :Expression
               (get-structured-description gene "expression"))]
    {:data (if (empty? data) nil data)
     :description
     "structured descriptions of gene function"}))

(def-rest-widget overview [gene]
  {:name                     (name-field gene)
   :version                  (gene-version gene)
   :classification           (gene-classification gene)
   :also_refers_to           (also-refers-to gene)
   :merged_into              (merged-into gene)
   :gene_class               (gene-class gene)
   :concise_description      (concise-description gene)
   :remarks                  (curatorial-remarks gene)
   :operon                   (gene-operon gene)
   :gene_cluster             (gene-cluster gene)
   :other_names              (gene-other-names gene)
   :taxonomy                 (gene-taxonomy gene)
   :status                   (gene-status gene)
   :legacy_information       (legacy-info gene)
   :named_by                 (named-by gene)
   :parent_sequence          (parent-sequence gene)
   :clone                    (parent-clone gene)
   :cloned_by                (cloned-by gene)
   :transposon               (transposon gene)
   :sequence_name            (sequence-name gene)
   :locus_name               (locus-name gene)
   :human_disease_relevance  (disease-relevance gene)
   :structured_description   (structured-description gene)})

;;
;; Phenotypes widget
;;

(def q-gene-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-gene-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype-not-observed ?ph]
           [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(def q-gene-var-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :variation.gene/gene ?g]
           [?var :variation/gene ?gh]
           [?var :variation/phenotype ?ph]
           [?ph :variation.phenotype/phenotype ?pheno]])

(def q-gene-cons-transgene
  '[:find ?tg (distinct ?tg)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]
           [?cons :construct/transgene-construct ?tg]])

(def q-gene-cons-transgene-test
  '[:find ?cons (distinct ?cons)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]])


(def q-gene-cons-transgene-phenotype
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]
           [?cons :construct/transgene-construct ?tg]
           [?tg :transgene/phenotype ?ph]
           [?ph :transgene.phenotype/phenotype ?pheno]])

(def q-gene-var-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :variation.gene/gene ?g]
           [?var :variation/gene ?gh]
           [?var :variation/phenotype-not-observed ?ph]
           [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

(defn- evidence-paper [paper]
  {:class "paper"
   :id (:paper/id paper)
   :taxonomy "all"
   :label (str (datomic-rest-api.rest.object/author-list paper) ", " (first (str/split (:paper/publication-date paper) #"-")))})

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn- create-pato-term [id entity-term entity-type pato-term]
  (let [pato-id  (str/join "_" [id pato-term])]
    {pato-id
     {:pato_evidence
      {:entity-term entity-term
       :entity_type "Anatomy_term"
       :pato_term pato-term}
      :key pato-id}}))

(defn- get-pato-from-holder [holder]
  (let [sot (for [eq-annotations {"anatomy-term" "anatomy-term"
                                  "life-stage" "life-stage"
                                  "go-term" "go-term"
                                  "molecule-affected" "molecule"}
                  :let [[eq-key label] eq-annotations]]
              (for [anatomy (:phenotype-info/anatomy-term holder)]
                (let [make-key (partial keyword (str "phenotype-info." eq-key))
                      pato-name (first (:pato-term/name (-> anatomy ((make-key "pato-term")))))
                      id (:anatomy-term/id  (-> anatomy ((make-key eq-key))))
                      entity-term (datomic-rest-api.rest.object/pack-obj label (-> anatomy ((make-key label))))
                      pato-term (if (nil? pato-name) "abnormal" pato-name)]
                  (if (nil? id) nil (create-pato-term id entity-term (str/capitalize (str/replace eq-key #"-" "_")) pato-term)))))
        var-combo (into {} (for [x sot] (apply merge x)))]
    {(str/join "_" (sort (keys var-combo))) (vals var-combo)}))

(defn- get-pato-combinations [db pid rnai-phenos var-phenos not?]
  (if-let [vp (distinct (concat (rnai-phenos pid) (var-phenos pid)))]
    (let [patos (for [v vp
                         :let [holder (entity db v)]]
                     (get-pato-from-holder holder))]
      (apply merge patos))))

(defn var-evidence [holder]
  (vmap
    :Person_evidence
    (seq
      (for [person (:phenotype-info/person-evidence holder)]
        {:class "person"
         :id (:person/id person)
         :label (:person/standard-name person)
         :taxonomy "all"}))

    :Curator
    (seq
      (for [person (:phenotype-info/curator-confirmed holder)]
        {:class "person"
         :id (:person/id person)
         :label (:person/standard-name person)
         :taxonomy "all"}))

    :Paper_evidence
    (seq
      (for [paper (:phenotype-info/paper-evidence holder)]
        (evidence-paper paper)))

    :Remark
    (seq
      (map :phenotype-info.remark/text
           (:phenotype-info/remark holder)))))



(defn- phenotype-table-entity [db pheno pato-key entity pid var-phenos rnai-phenos not?]
  {:entity entity
   :phenotype
   {:class "phenotype"
    :id (:phenotype/id pheno)
    :label (:phenotype.primary-name/text (:phenotype/primary-name pheno))
    :taxonomy "all"}
   :evidence
   (vmap
     "Allele:"
     (if-let [vp (seq (var-phenos pid))]
       (for [v vp
             :let [holder (d/entity db v)
                   var ((if not?
                          :variation/_phenotype-not-observed
                          :variation/_phenotype)
                        holder)
                   var-pato-key  (first (keys (get-pato-from-holder holder)))]]
         (if (= pato-key var-pato-key)
           {:text
            {:class "variation"
             :id (:variation/id var)
             :label (:variation/public-name var)
             :style (if (= (:variation/seqstatus var)
                           :variation.seqstatus/sequenced)
                      "font-weight:bold"
                      0)
             :taxonomy "c_elegans"}
            :evidence (var-evidence holder)})))

     "RNAi:"
     (if-let [rp (seq (rnai-phenos pid))]
       (for [r rp
             :let [holder (d/entity db r)
                   rnai ((if not?
                           :rnai/_phenotype-not-observed
                           :rnai/_phenotype)
                         holder)
                   rnai-pato-key  (first (keys (get-pato-from-holder holder)))]]
         (if (= rnai-pato-key pato-key)
           {:text
            {:class "rnai"
             :id (:rnai/id rnai)
             :label (str (parse-int (:rnai/id rnai)))
             :taxonomy "c_elegans"}
            :evidence
            (merge
              {:genotype
               (:rnai/genotype rnai)

               :strain
               (:strain/id (:rnai/strain rnai))

               :paper
               (if-let [paper (:rnai.reference/paper (:rnai/reference rnai))]
                 (evidence-paper paper))}
              (var-evidence holder))}))))})

(defn- phenotype-table [db gene not?]
  (let [var-phenos (into {} (q (if not?
                                 q-gene-var-not-pheno
                                 q-gene-var-pheno)
                               db gene))
        rnai-phenos (into {} (q (if not?
                                  q-gene-rnai-not-pheno
                                  q-gene-rnai-pheno)
                                db gene))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->>
      (flatten
        (for [pid phenos
              :let [pheno (entity db pid)]]
          (let [pcs (get-pato-combinations db pid rnai-phenos var-phenos not?)]
            (if (nil? pcs)
              (phenotype-table-entity db pheno nil nil pid var-phenos rnai-phenos not?)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db pheno pato-key entity pid var-phenos rnai-phenos not?))))))
      (into []))))

(defn- phenotype-not-observed-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) true)]
    {:data (if (empty? data) nil data)
     :description "The Phenotype not observed summary of the gene"}))

(defn- phenotype-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) false)]
    {:data (if (empty? data) nil data)
     :description "The Phenotype summary of the gene"}))

(defn- phenotype-by-interaction [gene]
  (let [db (d/entity-db gene)
        gid (:db/id gene)
        table (q '[:find ?pheno (distinct ?int) ?int-type
                   :in $ ?gene
                   :where [?ig :interaction.interactor-overlapping-gene/gene ?gene]
                          [?int :interaction/interactor-overlapping-gene ?ig]
                          [?int :interaction/interaction-phenotype ?pheno]
                          [?int :interaction/type ?type-id]
                          [?type-id :db/ident ?int-type]]
                 db gid)
        phenos (->> (map first table)
                    (set)
                    (map (fn [pid]
                           [pid (datomic-rest-api.rest.object/pack-obj "phenotype" (entity db pid))]))
                    (into {}))
        ints (->> (mapcat second table)
                  (set)
                  (map (fn [iid]
                         (let [int (entity db iid)]
                           [iid
                            {:interaction (datomic-rest-api.rest.object/pack-obj "interaction" int)
                             :citations (map (partial datomic-rest-api.rest.object/pack-obj "paper") (:interaction/paper int))}])))
                  (into {}))
        data (map (fn [[pheno pints int-type]]
                    {:interaction_type
                     (datomic-rest-api.rest.object/humanize-ident int-type)

                     :phenotype
                     (phenos pheno)

                     :interactions
                     (map #(:interaction (ints %)) pints)

                     :citations
                     (map #(:citations (ints %)) pints)})
                  table)]
    {:data (if (empty? data) nil data)
     :description
     "phenotype based on interaction"}))

(defn get-transgene-evidence [holders phenotypeid transgene]
  (for [h holders
        :let [pid (:phenotype/id (:transgene.phenotype/phenotype h))]]
    (if (= pid phenotypeid)
      (let [remark (map :phenotype-info.remark/text
                        (:phenotype-info/remark h))
            transgeneobj (datomic-rest-api.rest.object/pack-obj
                           "transgene" transgene)
            causedbygenes (:phenotype-info/caused-by-gene h)
            paperevidences (:phenotype-info/paper-evidence h)
            curators (:phenotype-info/curator-confirmed h)
            ]
        {:text
         [transgeneobj
          (str "<em>" (:transgene.summary/text (:transgene/summary transgene)) "</em>")
          remark]

         :evidence
         {:Phenotype_assay
          (remove
            nil?
            (flatten
              (for [term ["treatment"
                          "temperature"
                          "genotype"]]
                (let [make-key (partial keyword (str "phenotype-info"))
                      label (str/capitalize term)]
                  (if (contains? h (make-key term))
                    {:taxonomy "all"
                     :class "tag"
                     :label label
                     :id label})))))

          :Curator
          (for [curator curators]
            (datomic-rest-api.rest.object/pack-obj "person" curator))

          :EQ_annotations
          (remove
            nil?
            (flatten
              (for [term ["anatomy-term"
                          "life-stage"
                          "go-term"
                          "molecule-affected"]]
                (let [make-key (partial keyword (str "phenotype-info"))
                      label (str/capitalize term)]
                  (if (contains? h (make-key term))
                    {:taxonomy "all"
                     :class "tag"
                     :label label
                     :id label})))))

          :Caused_by_gene
          (for [cbg causedbygenes]
            (datomic-rest-api.rest.object/pack-obj
              "gene"
              (:phenotype-info.caused-by-gene/gene cbg)))

          :Transgene transgeneobj

          :Paper_evidence
          (for [pe paperevidences]
            (datomic-rest-api.rest.object/pack-obj "paper" pe))

          :remark remark}}))))

(defn drives-overexpression [gene]
  (let [db (d/entity-db gene)
        transgenes
        (q '[:find [?tg ...]
             :in $ ?gene
             :where [?cbg :construct.driven-by-gene/gene ?gene]
                    [?cons :construct/driven-by-gene ?cbg]
                    [?cons :construct/transgene-construct ?tg]]
           db (:db/id gene))
        phenotype
        (->> transgenes
             (map
               (fn [tg]
                 (->> (q '[:find [?pheno ...]
                           :in $ ?tg
                           :where [?tg :transgene/phenotype ?ph]
                                  [?ph :transgene.phenotype/phenotype ?pheno]]
                         db tg)
                      (map
                        (fn [p]
                          (let [pheno (entity db p)
                                phenotypeid (:phenotype/id pheno)]
                            {phenotypeid
                             {:object
                              (datomic-rest-api.rest.object/pack-obj "phenotype" pheno)

                              :evidence
                              (flatten
                                (for [tg transgenes
                                      :let [transgene (d/entity db tg)
                                            holders  (:transgene/phenotype transgene)
                                            evidence (get-transgene-evidence holders phenotypeid transgene)]]
                                  evidence)) }})))
                      (into {}))))
             (into{}))]
    {:data (if (empty? phenotype) nil {:Phenotype phenotype})
     :description "phenotypes due to overexpression under the promoter of this gene"}))

(def-rest-widget phenotype [gene]
  {:name                     (name-field gene)
   :drives_overexpression    (drives-overexpression gene)
   :phenotype                (phenotype-field gene)
   :phenotype_not_observed   (phenotype-not-observed-field gene)
   :phenotype_by_interaction (phenotype-by-interaction gene)})

;;
;; Mapping data widget
;;

;; Needs better support for non-gene things.

(defn gene-mapping-twopt
  [db id]
  (->> (q '[:find [?tp ...]
            :in $ ?gene
            :where (or-join [?gene ?tp]
                            (and [?tpg1 :two-point-data.gene-1/gene ?gene]
                                 [?tp :two-point-data/gene-1 ?tpg1])
                            (and [?tpg2 :two-point-data.gene-2/gene ?gene]
                                 [?tp :two-point-data/gene-2 ?tpg2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [tp]
          {:mapper     (datomic-rest-api.rest.object/pack-obj "author" (first (:two-point-data/mapper tp)))
           :date       (datomic-rest-api.helpers/format-date (:two-point-data/date tp))
           :raw_data   (:two-point-data/results tp)
           :genotype   (:two-point-data/genotype tp)
           :comment    (let [comment (str/join "<br>" (map :two-point-data.remark/text (:two-point-data/remark tp)))]
                         (if (empty? comment) "" comment ))
           :distance   (format "%s (%s-%s)" (or (:two-point-data/calc-distance tp) "0.0")
                               (or (:two-point-data/calc-lower-conf tp) "0")
                               (or (:two-point-data/calc-upper-conf tp) "0"))
           :point_1    (let [p1 (:two-point-data/gene-1 tp)]
                         (remove nil? [(datomic-rest-api.rest.object/pack-obj "gene" (:two-point-data.gene-1/gene p1))
                                       (datomic-rest-api.rest.object/pack-obj "variation" (:two-point-data.gene-1/variation p1))]))
           :point_2    (let [p2 (:two-point-data/gene-2 tp)]
                         (remove nil? [(datomic-rest-api.rest.object/pack-obj "gene" (:two-point-data.gene-2/gene p2))
                                       (datomic-rest-api.rest.object/pack-obj "variation" (:two-point-data.gene-2/variation p2))]))}))))

(defn gene-mapping-posneg
  [db id]
  (->> (q '[:find [?pn ...]
            :in $ ?gene
            :where (or-join [?gene ?pn]
                            (and [?png1 :pos-neg-data.gene-1/gene ?gene]
                                 [?pn :pos-neg-data/gene-1 ?png1])
                            (and [?png2 :pos-neg-data.gene-2/gene ?gene]
                                 [?pn :pos-neg-data/gene-2 ?png2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [pn]
          (let [items (->> [(datomic-rest-api.rest.object/pack-obj ((some-fn (comp :pos-neg-data.gene-1/gene :pos-neg-data/gene-1)
                                                (comp :pos-neg-data.locus-1/locus :pos-neg-data/locus-1)
                                                :pos-neg-data/allele-1
                                                :pos-neg-data/clone-1
                                                :pos-neg-data/rearrangement-1)
                                       pn))
                            (datomic-rest-api.rest.object/pack-obj ((some-fn (comp :pos-neg-data.gene-2/gene :pos-neg-data/gene-2)
                                                (comp :pos-neg-data.locus-2/locus :pos-neg-data/locus-2)
                                                :pos-neg-data/allele-2
                                                :pos-neg-data/clone-2
                                                :pos-neg-data/rearrangement-2)
                                       pn))]
                           (map (juxt :label identity))
                           (into {}))
                result (str/split (:pos-neg-data/results pn) #"\s+")]
          {:mapper    (datomic-rest-api.rest.object/pack-obj "author" (first (:pos-neg-data/mapper pn)))
           :comment    (let [comment (str/join "<br>" (map :pos-neg-data.remark/text (:pos-neg-data/remark pn)))]
                          (if (empty? comment) "" comment ))
           :date      (datomic-rest-api.helpers/format-date (:pos-neg-data/date pn))
           :result    (map #(or (items (str/replace % #"\." ""))
                                (str % " "))
                           result)})

          ))))

(defn gene-mapping-multipt
  [db id]
  (->> (q '[:find [?mp ...]
            :in $ % ?gene
            :where (mc-obj ?mc ?gene)
            (or
              [?mp :multi-pt-data/a-non-b ?mc]
              [?mp :multi-pt-data/b-non-a ?mc]
              [?mp :multi-pt-data/combined ?mc])]
          db
          '[[(mc-obj ?mc ?gene) [?mcg :multi-counts.gene/gene ?gene]
             [?mc :multi-counts/gene ?mcg]]
            [(mc-obj ?mc ?gene) (mc-obj ?mc2 ?gene)
             [?mc :multi-counts/gene ?mc2]]]
          id)
       (map (partial entity db))
       (map
         (fn [mp]
           {:comment (let [comment (->> mp
                                        :multi-pt-data/remark
                                        first
                                        :multi-pt-data.remark/text)]
                       (if (empty? comment) "" comment))
           :mapper   (datomic-rest-api.rest.object/pack-obj "author" (first (:multi-pt-data/mapper mp)))
           :date     (if (nil? (:multi-pt-data/date mp)) "" (datomic-rest-api.helpers/format-date3 (str (:multi-pt-data/date mp))))
           :genotype (:multi-pt-data/genotype mp)
           :result   (let [res (loop [node (:multi-pt-data/combined mp)
                                      res  []]
                                 (cond
                                  (:multi-counts/gene node)
                                  (let [obj (:multi-counts/gene node)]
                                    (recur obj (conj res [(:multi-counts.gene/gene obj)
                                                          (:multi-counts.gene/int obj)])))

                                  :default res))
                           tot (->> (map second res)
                                    (filter identity)
                                    (reduce +))
                           sum (atom 0)
                           open-paren (atom 0)]
                       (->>
                        (mapcat
                         (fn [[obj count]]
                           [(if (and (= @open-paren 0) (= count 0) (< @sum tot))
                              (do
                                (swap! open-paren inc)
                                "("))
                            (datomic-rest-api.rest.object/pack-obj obj)
                            (if (and (not (= count 0)) (= @open-paren 1))
                              (do
                                (reset! open-paren 0)
                                ")"))
                            (if (and count (not (= count 0)))
                              (do
                               (swap! sum (fn[n] (+ n count)))
                               (str " (" count "/" tot ") ")))])
                         res)
                        (filter identity)))}
          ))))

(def-rest-widget mapping-data [gene]
  {:name      (name-field gene)

   :two_pt_data
   {:data (seq (gene-mapping-twopt (d/entity-db gene) (:db/id gene)))
    :description "Two point mapping data for this gene"}

   :pos_neg_data
   {:data (seq (gene-mapping-posneg (d/entity-db gene) (:db/id gene)))
    :description "Positive/Negative mapping data for this gene"}

   :multi_pt_data
   {:data (seq (gene-mapping-multipt (d/entity-db gene) (:db/id gene)))
    :description "Multi point mapping data for this gene"}})


;;
;; Human diseases widget
;;

(defn- disease-models [gene]
  (let [db (d/entity-db gene)]
    {:data
     {:potential_model
      (seq
        (for [d (:gene/disease-potential-model gene)]
          (assoc (datomic-rest-api.rest.object/pack-obj (:gene.disease-potential-model/do-term d))
                 :ev (datomic-rest-api.rest.object/get-evidence d))))

      :experimental_model
      (seq
       (for [d (:gene/disease-experimental-model gene)]
         (assoc (datomic-rest-api.rest.object/pack-obj (:gene.disease-experimental-model/do-term d))
           :ev (datomic-rest-api.rest.object/get-evidence d))))

      :gene
      (seq
       (q '[:find [?o ...]
            :in $ ?gene
            :where [?gene :gene/database ?dbent]
                   [?omim :database/id "OMIM"]
                   [?field :database-field/id "gene"]
                   [?dbent :gene.database/database ?omim]
                   [?dbent :gene.database/field ?field]
                   [?dbent :gene.database/accession ?o]]
          db (:db/id gene)))

      :disease
      (seq
       (q '[:find [?o ...]
            :in $ ?gene
            :where [?gene :gene/database ?dbent]
                   [?omim :database/id "OMIM"]
                   [?field :database-field/id "disease"]
                   [?dbent :gene.database/database ?omim]
                   [?dbent :gene.database/field ?field]
                   [?dbent :gene.database/accession ?o]]
          db (:db/id gene)))}}))


(def-rest-widget human-diseases [gene]
  {:name                    (name-field gene)
   :human_disease_relevance (disease-relevance gene)
   :human_diseases          (disease-models gene)})

;;
;; Assembly-twiddling stuff (should be in own namespace?)
;;

#_(defn locatable-root-segment [loc]
  (loop [parent (:locatable/parent loc)
         min    (:locatable/min    loc)
         max    (:locatable/max    loc)]
    (if parent
      (if-let [ss (first (:sequence.subsequence/_sequence parent))]
        (recur (:sequence/_subsequence ss)
               (+ min (:sequence.subsequence/start ss) -1)
               (+ max (:sequence.subsequence/start ss) -1))
        {:sequence (:sequence/id parent)
         :seq-id   (:db/id parent)
         :min      min
         :max      max}))))

;;
;; Reagents widget
;;

(defn- construct-labs [construct]
  (seq (map #(datomic-rest-api.rest.object/pack-obj "laboratory" (:construct.laboratory/laboratory %))
            (:construct/laboratory construct))))

(defn- transgene-labs [tg]
  (seq (map #(datomic-rest-api.rest.object/pack-obj "laboratory" (:transgene.laboratory/laboratory %))
            (:transgene/laboratory tg))))


(defn- transgene-record [construct]
  (let [base {:construct (datomic-rest-api.rest.object/pack-obj "construct" construct)
              :used_in   (datomic-rest-api.rest.object/pack-obj "transgene" (first (:construct/transgene-construct construct)))
              :use_summary (:construct/summary construct)}]
    (cond-let [use]
      (:construct/transgene-construct construct)
      (for [t use]
        (assoc base :used_in_type "Transgene construct"
                    :use_summary (:transgene.summary/text (:transgene/summary t))
                    :used_in     (datomic-rest-api.rest.object/pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct)
                                     [])))

      (:construct/transgene-coinjection construct)
      (for [t use]
        (assoc base :used_in_type "Transgene coinjection"
                    :use_summary (:transgene.summary/text (:transgene/summary t))
                    :used_in     (datomic-rest-api.rest.object/pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct)
                                      [])))

      (:construct/engineered-variation construct)
      (for [v use]
        (assoc base :used_in_type "Engineered variation"
                    :used_in      (datomic-rest-api.rest.object/pack-obj "variation" v)
                    :use_lab      (construct-labs construct))))))

(defn- transgenes [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cons ...]
               :in $ ?gene
               :where [?cbg :construct.driven-by-gene/gene ?gene]
                      [?cons :construct/driven-by-gene ?cbg]]
             db (:db/id gene))
          (map (partial entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes expressed by this gene"}))

(defn- transgene-products [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cons ...]
               :in $ ?gene
               :where [?cg :construct.gene/gene ?gene]
                      [?cons :construct/gene ?cg]]
             db (:db/id gene))
          (map (partial entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes that express this gene"}))

(def ^:private probe-types
  {:oligo-set.type/affymetrix-microarray-probe "Affymetrix"
   :oligo-set.type/washu-gsc-microarray-probe  "GSC"
   :oligo-set.type/agilent-microarray-probe    "Agilent"})

(defn- microarray-probes [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?oligo ...]
               :in $ ?gene [?type ...]
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?ocds :oligo-set.overlaps-cds/cds ?cds]
                      [?oligo :oligo-set/overlaps-cds ?ocds]
                      [?oligo :oligo-set/type ?type]]
             db (:db/id gene) (keys probe-types))
          (map (fn [oid]
                 (let [oligo (entity db oid)]
                   (assoc (datomic-rest-api.rest.object/pack-obj "oligo-set" oligo)
                          :class "pcr_oligo"
                          :label (format
                                   "%s [%s]"
                                   (:oligo-set/id oligo)
                                   (some probe-types (:oligo-set/type oligo)))))))
          (seq))
     :description "microarray probes"}))

(defn- matching-cdnas [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cdna ...]
               :in $ ?gene
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?cds :cds/matching-cdna ?mcdna]
                      [?mcdna :cds.matching-cdna/sequence ?cdna]]
             db (:db/id gene))
          (map #(datomic-rest-api.rest.object/pack-obj "sequence" (entity db %)))
          (seq))
     :description "cDNAs matching this gene"}))

(defn- antibodies [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?ab ...]
               :in $ ?gene
               :where [?gab :antibody.gene/gene ?gene]
                      [?ab :antibody/gene ?gab]]
             db (:db/id gene))
          (map
            (fn [abid]
              (let [ab (entity db abid)]
                {:antibody (datomic-rest-api.rest.object/pack-obj "antibody" ab)
                 :summary (:antibody.summary/text (:antibody/summary ab))
                 :laboratory (map (partial datomic-rest-api.rest.object/pack-obj "laboratory") (:antibody/location ab))})))
          (seq))
     :description "antibodies generated against protein products or gene fusions"}))

#_(def ^:private child-rule  '[[(child ?parent ?min ?max ?method ?c) [(pseudoace.binning/reg2bins ?min ?max) [?bin ...]]
                                                                   [(pseudoace.binning/xbin ?parent ?bin) ?xbin]
                                                                   [?c :locatable/xbin ?xbin]
                                                                   [?c :locatable/parent ?parent]
                                                                   [?c :pcr-product/method ?method]
                                                                   [?c :locatable/min ?cmin]
                                                                   [?c :locatable/max ?cmax]
                                                                   [(<= ?cmin ?max)]
                                                                   [(>= ?cmax ?min)]]])

(def ^:private child-rule
  '[[(child ?parent ?min ?max ?method ?c) [?parent :sequence/id ?seq-name]
     [(pseudoace.binning/bins ?seq-name ?min ?max) [?bin ...]]
     [?c :locatable/murmur-bin ?bin]
     [?c :locatable/parent ?parent]
     [?c :locatable/min ?cmin]
     [?c :locatable/max ?cmax]
     [?c :pcr-product/id _]
     [?c :locatable/method ?method]
     [(<= ?cmin ?max)]
     [(>= ?cmax ?min)]]])

(defn- orfeome-primers [gene]
  (let [db  (d/entity-db gene)
        [parent start end] (root-segment gene)]
    {:data
     ;;
     ;; Big assembly-navigation query should probably be factored out somewhere
     ;; once we're a bit more solid about how this stuff should work.
     ;;
     (if parent
       (->> (q '[:find [?p ...]
                 :in $ % ?seq ?min ?max
                 :where [?method :method/id "Orfeome"]
                        (or-join [?seq ?min ?max ?method ?p]
                          (and
                            [?ss-seq :locatable/assembly-parent ?seq]
                            [?ss-seq :locatable/min ?ss-min]
                            [?ss-seq :locatable/max ?ss-max]
                            [(<= ?ss-min ?max)]
                            [(>= ?ss-max ?min)]
                            [(- ?min ?ss-min -1) ?rel-min]
                            [(- ?max ?ss-min -1) ?rel-max]
                            (child ?ss-seq ?rel-min ?rel-max ?method ?p))
                          (child ?seq ?min ?max ?method ?p))]
               db
               child-rule
               (:db/id parent) start end)
            (map
              (fn [ppid]
                (let [pp (entity db ppid)]
                  {:id    (:pcr-product/id pp)
                   :class "pcr_oligo"
                   :label (:pcr-product/id pp)})))
            (seq)))
     :description "ORFeome Project primers and sequences"}))

(defn- primer-pairs [gene]
  (let [db                 (d/entity-db gene)
        [parent start end] (root-segment gene)]
    {:data
     (if parent
       (->> (q '[:find [?p ...]
                 :in $ % ?seq ?min ?max
                 :where [?method :method/id "GenePairs"]
                        (or-join [?seq ?min ?max ?method ?p]
                          (and
                            [?ss-seq :locatable/assembly-parent ?seq]
                            [?ss-seq :locatable/min ?ss-min]
                            [?ss-seq :locatable/max ?ss-max]
                            [(<= ?ss-min ?max)]
                            [(>= ?ss-max ?min)]
                            [(- ?min ?ss-min -1) ?rel-min]
                            [(- ?max ?ss-min -1) ?rel-max]
                            (child ?ss-seq ?rel-min ?rel-max ?method ?p))
                          (child ?seq ?min ?max ?method ?p))]
               db
               child-rule
               (:db/id parent) start end)
            (map
              (fn [ppid]
                (let [pp (entity db ppid)]
                  {:id    (:pcr-product/id pp)
                   :class "pcr_oligo"
                   :label (:pcr-product/id pp)})))
            (seq)))
     :description "Primer pairs"}))

(defn- sage-tags [gene]
  {:data
   (seq (map #(datomic-rest-api.rest.object/pack-obj "sage-tag" (:sage-tag/_gene %)) (:sage-tag.gene/_gene gene)))

   :description
   "SAGE tags identified"})

(def-rest-widget reagents [gene]
  {:name               (name-field gene)
   :transgenes         (transgenes gene)
   :transgene_products (transgene-products gene)
   :microarray_probes  (microarray-probes gene)
   :matching_cdnas     (matching-cdnas gene)
   :antibodies         (antibodies gene)
   :orfeome_primers    (orfeome-primers gene)
   :primer_pairs       (primer-pairs gene)
   :sage_tags          (sage-tags gene)})
;;
;; New style GO widget
;;

(def ^:private division-names
  {:go-term.type/molecular-function "Molecular_function"
   :go-term.type/cellular-component "Cellular_component"
   :go-term.type/biological-process "Biological_process"})

(defn- term-table-full [db annos]
  (map
    (fn [{:keys [term code anno]}]
      {:anno_id
       (:go-annotation/id anno)

      :with
      (seq
       (concat
        (map (partial datomic-rest-api.rest.object/pack-obj "gene")      (:go-annotation/interaction-gene anno))
        (map (partial datomic-rest-api.rest.object/pack-obj "go-term")   (:go-annotation/inferred-from-go-term anno))
        (map (partial datomic-rest-api.rest.object/pack-obj "motif")     (:go-annotation/motif anno))
        (map (partial datomic-rest-api.rest.object/pack-obj "rnai")      (:go-annotation/rnai-result anno))
        (map (partial datomic-rest-api.rest.object/pack-obj "variation") (:go-annotation/variation anno))
        (map (partial datomic-rest-api.rest.object/pack-obj "phenotype") (:go-annotation/phenotype anno))
        ;; Also DB fields...
        ))


      :evidence_code
      {:text
       (:go-code/id code)

       :evidence
       (vmap
        :Date_last_updated
        (if-let [d (:go-annotation/date-last-updated anno)]
          [{:class "text"
           :id (datomic-rest-api.helpers/format-date3 (str d))
           :label (datomic-rest-api.helpers/format-date3 (str d))}])

        :Contributed_by
           [(datomic-rest-api.rest.object/pack-obj "analysis"
                (:go-annotation/contributed-by anno))]
        :Reference
         (if (:go-annotation/reference anno)
           [(datomic-rest-api.rest.object/pack-obj "paper"
                  (:go-annotation/reference anno))])

        :GO_reference
         (if (:go-annotation/go-term-relation anno)
            (concat
             (for [{rel :go-annotation.go-term-relation/text
                    gt :go-annotation.go-term-relation/go-term}
                   (:go-annotation/go-term-relation anno)]
                    {:class "Gene Ontology Consortium"
                     :dbt "GO_REF"
                     :id (:go-term/id gt)
                     :label (:go-term/id gt) }))))}

      :go_type
      (if-let [go-type (:go-term/type term)]
           (division-names go-type))

      :term-test  (:go-annotation/molecule-relation anno)

      :term
      (if (:go-annotation/molecule-relation anno)
      [(vmap
       :evidence
       (->>
        (concat
         (for [{rel :go-annotation.molecule-relation/text
                mol :go-annotation.molecule-relation/molecule}
               (:go-annotation/molecule-relation anno)]
           [rel (datomic-rest-api.rest.object/pack-obj "molecule" mol)]))
        (reduce
         (fn [m [rel obj]]
           (assoc m rel obj))
         nil)))])

      :term_id
      (datomic-rest-api.rest.object/pack-obj "go-term" term :label (:go-term/id term))

      :term_description
      (datomic-rest-api.rest.object/pack-obj "go-term" term)})
   annos))

(defn- gene-ontology-full [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find ?div ?term ?code ?anno
            :in $ ?gene
            :where [?anno :go-annotation/gene ?gene]
                   [?anno :go-annotation/go-term ?term]
                   [?anno :go-annotation/go-code ?code]
                   [?term :go-term/type ?tdiv]
                   [?tdiv :db/ident ?div]]
          db (:db/id gene))
       (map
         (fn [[div term code anno]]
           {:division div
            :term     (entity db term)
            :code     (entity db code)
            :anno     (entity db anno)}))
       (group-by :division)
       (map
         (fn [[key annos]]
           [(division-names key)
            (term-table-full db annos)]))
       (into {}))

     :description
     "gene ontology associations"}))

(defn- term-summary-table [db annos]
  (->>
    (group-by :term annos)
    (map
      (fn [[term annos]]
        {:extensions ""

       :term_id
       (datomic-rest-api.rest.object/pack-obj "go-term" term :label (:go-term/id term))

       :term_description
       (datomic-rest-api.rest.object/pack-obj "go-term" term)}))))

(defn- gene-ontology-summary [gene]
 (let [db (d/entity-db gene)]
  {:data
   (->>
    (q '[:find ?div ?term ?anno
         :in $ ?gene
         :where [?anno :go-annotation/gene ?gene]
                [?anno :go-annotation/go-term ?term]
                [?term :go-term/type ?tdiv]
                [?tdiv :db/ident ?div]]
       db (:db/id gene))
    (map
     (fn [[div term code anno]]
       {:division div
        :term     (entity db term)
        :anno     (entity db anno)}))
    (group-by :division)
    (map
     (fn [[key annos]]
       [(division-names key)
        (term-summary-table db annos)]))
    (into {}))

   :description
   "gene ontology associations"}))

(def-rest-widget gene-ontology [gene]
  {:name                   (name-field gene)
   :gene_ontology_summary  (gene-ontology-summary gene)
   :gene_ontology          (gene-ontology-full gene)})

;;
;; Expression widget
;;

(defn- anatomy-terms [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?at ...]
               :in $ ?gene
               :where [?epg :expr-pattern.gene/gene ?gene]
                      [?ep :expr-pattern/gene ?epg]
                      [?ep :expr-pattern/anatomy-term ?epa]
                      [?epa :expr-pattern.anatomy-term/anatomy-term ?at]]
             db (:db/id gene))
          (map (fn [at-id]
                 (datomic-rest-api.rest.object/pack-obj "anatomy-term" (entity db at-id)))))
     :description "anatomy terms from expression patterns for the gene"}))

(defn- expr-pattern-type [ep]
  (some (set (keys ep)) [:expr-pattern/reporter-gene
                         :expr-pattern/in-situ
                         :expr-pattern/antibody
                         :expr-pattern/northern
                         :expr-pattern/western
                         :expr-pattern/rt-pcr
                         :expr-pattern/localizome
                         :expr-pattern/microarray
                         :expr-pattern/tiling-array
                         :expr-pattern/epic
                         :expr-pattern/cis-regulatory-element]))

(defn- expression-patterns [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
      (q '[:find [?ep ...]
           :in $ ?gene
           :where [?epg :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?epg]
                  (not
                    [?ep :expr-pattern/microarray _])
                  (not
                    [?ep :expr-pattern/tiling-array _])]
         db (:db/id gene))
      (map
       (fn [ep-id]
         (let [ep (entity db ep-id)]
           (vmap
            :expression_pattern
            (datomic-rest-api.rest.object/pack-obj "expr-pattern" ep)

            :description
            (if-let [desc (or (:expr-pattern/pattern ep)
                              (:expr-pattern/subcellular-localization ep)
                              (:expr-pattern.remark/text (:expr-pattern/remark ep)))]
              {:text desc
               :evidence (vmap
                          :Reference (datomic-rest-api.rest.object/pack-obj "paper" (first (:expr-pattern/reference ep))))})

            :type
            (datomic-rest-api.rest.object/humanize-ident (expr-pattern-type ep))

            :expressed_in
            (map #(datomic-rest-api.rest.object/pack-obj "anatomy-term" (:expr-pattern.anatomy-term/anatomy-term %))
                 (:expr-pattern/anatomy-term ep))

            :life_stage
            (map #(datomic-rest-api.rest.object/pack-obj "life-stage" (:expr-pattern.life-stage/life-stage %))
                 (:expr-pattern/life-stage ep))

            :go_term
            (if-let [go (:expr-pattern/go-term ep)]
              {:text (map #(datomic-rest-api.rest.object/pack-obj "go-term" (:expr-pattern.go-term/go-term %)) go)
               :evidence {"Subcellular localization" (:expr-pattern/subcellular-localization ep)}})

            :transgene
            (if (:expr-pattern/transgene ep)
              (map
               (fn [tg]
                 (let [packed (datomic-rest-api.rest.object/pack-obj "transgene" tg)
                       cs     (:transgene/construction-summary tg)]
                   (if cs
                     {:text packed
                      :evidence {:Construction_summary cs}}
                     packed)))
               (:expr-pattern/transgene ep))
              (map
               (fn [cons]
                 (let [packed (datomic-rest-api.rest.object/pack-obj "construct" cons)
                       cs     (:construct/construction-summary cons)]
                   (if cs
                     {:text packed
                      :evidence {:Construction_summary cs}}
                     packed)))
               (:expr-pattern/construct ep)))))

           )))
     :description (format "expression patterns associated with the gene:%s" (:gene/id gene))}))


(defn- expression-clusters [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find [?ec ...]
            :in $ ?gene
            :where [?ecg :expression-cluster.gene/gene ?gene]
                   [?ec :expression-cluster/gene ?ecg]]
          db (:db/id gene))
       (map
         (fn [ec-id]
           (let [ec (entity db ec-id)]
             {:expression_cluster (datomic-rest-api.rest.object/pack-obj "expression-cluster" ec)
              :description     (apply str (:expression-cluster/description ec))}))))
     :description
     "expression cluster data"}))

(defn- anatomic-expression-patterns [gene]
  {:data  {}
   :description "expression patterns for the gene"})

(defn- anatomy-function [gene]
  (let [db (d/entity-db gene)]
   {:data
    (->>
     (q '[:find [?af ...]
          :in $ ?gene
          :where [?afg :anatomy-function.gene/gene ?gene]
                 [?af :anatomy-function/gene ?afg]]
        db (:db/id gene))
      (map
       (fn [af-id]
        (let [af (entity db af-id)]
           {:anatomy-function (datomic-rest-api.rest.object/pack-obj "expression-cluster" af)})))) ;; need to still make this packed object - so far have not seen an exmample of it filled in
      :description "anatomy functions associatated with this gene"}))

;; I haven't found an example for this to show that it works
(defn- curated-images [ep]
 (let [images (:picture/expr_pattern ep)]
     (map
      (fn [image]
        (datomic-rest-api.rest.object/pack-obj "picture" image))
      images)))

(defn- expression-profiling-graphs [gene]
  (let [db (d/entity-db gene)]
   {:data
    (->>
     (q '[:find [?ep ...]
          :in $ ?gene
          :where [?epg :expr-pattern.gene/gene ?gene]
                 [?ep :expr-pattern/gene ?epg]]
        db (:db/id gene))
      (map
       (fn [ep-id]
        (let [ep (entity db ep-id)]
          {;;:data-test {:id (:expr-pattern/id ep)
  ;;                    :gene (:expr-pattern/gene ep) }
  ;;                    :rnaseq (:expr-pattern/rnaseq ep)
    ;;                  :pattern (:expr-pattern/pattern ep)
      ;;                :reference (:expr-pattern/reference ep)}
;;           :data (keys ep)
            :database nil
           :description nil
           :expressed_in nil
           :expression_pattern {:class "expr_pattern"
                                :curated_images (curated-images ep) ;; should be array of pack-obj (datomic-rest-api.rest.object/pack-obj "picture )
                                :id (:expr-pattern/id ep)
                                :label (:expr-pattern/id ep)
                                :taxonomy "all"}
           :go_term (first (:expr-pattern/go-term ep)) ;; need to see example
           :life_stage (:expr-pattern/life-stage ep) ;; need to see example
           :transgene (:expr-pattern/transgene ep) ;; need to see example
           :type (expr-pattern-type ep)}))))
    :description (format "expression patterns associated with the gene:%s" (:gene/id gene))}))

(defn- fourd-expression-movies [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
      (q '[:find [?ep ...]
           :in $ ?gene
           :where [?epg :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?epg]
 ;;                 [?ep :expr-pattern/author ?author]
 ;;                 [(re-find #".*Mohler.*" ?author)]
                  (not
                    [?ep :expr-pattern/microarray _])
                  (not
                    [?ep :expr-pattern/tiling-array _])]
         db (:db/id gene))
      (map
       (fn [ep-id]
         (let [ep (entity db ep-id)]
           (vmap
              :movie
                (first (:expr-pattern/movieurl ep))

              :test
                   (expr-pattern-type ep)

              :details
                (:expr-pattern/pattern ep)

              :object "movie")))))

    :description "interactive 4D expression movies"}))

(defn- microarray-topology-map-position [gene]
  {:data nil ;; this resquires segment data and can be found in the file: lib/WormBase/API/Role/Expression.pm
   :description "microarray topography map"})

(def-rest-widget expression [gene]
  {:name                (name-field gene)
   :anatomy_terms       (anatomy-terms gene)
   :expression_patterns (expression-patterns gene)
   :expression_cluster  (expression-clusters gene)
   :expression_profiling_graphs (expression-profiling-graphs gene)
   :anatomic_expression_patterns (anatomic-expression-patterns gene)
   :microarray_topology_map_position (microarray-topology-map-position gene)
   :fourd_expression_movies (fourd-expression-movies gene)
   :anatomy_function (anatomy-function gene)})

;;
;; Homology widget
;;

(defn- pack-ortholog [db oid]
  (let [ortho (entity db oid)]
    {:ortholog (datomic-rest-api.rest.object/pack-obj "gene" (:gene.ortholog/gene ortho))
     :species (if-let [[_ genus species] (re-matches #"^(\w)\w*\s+(.*)"
                                                     (:species/id (:gene.ortholog/species ortho)))]
                {:genus genus :species species})
     :method (map (partial datomic-rest-api.rest.object/pack-obj) (:evidence/from-analysis ortho))}))

(defn- homology-orthologs [gene species]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find [?ortho ...]
            :in $ ?gene [?species-id ...]
            :where [?gene :gene/ortholog ?ortho]
                   [?ortho :gene.ortholog/species ?species]
                   [?species :species/id ?species-id]]
          db (:db/id gene) species)
       (map (partial pack-ortholog db)))
     :description
     "precalculated ortholog assignments for this gene"}))

(defn- homology-orthologs-not [gene species]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find [?ortho ...]    ;; Look into why this can't be done with Datomic "not"
            :in $ ?gene ?not-species
            :where [?gene :gene/ortholog ?ortho]
                   [?ortho :gene.ortholog/species ?species]
                   [?species :species/id ?species-id]
                   [(get ?not-species ?species-id :dummy) ?smember]
                   [(= ?smember :dummy)]]
          db (:db/id gene) (set species))
       (map (partial pack-ortholog db)))
     :description
     "precalculated ortholog assignments for this gene"}))

(defn- homology-paralogs [gene]
  {:data
   (map
     (fn [para]
       {:ortholog (datomic-rest-api.rest.object/pack-obj "gene" (:gene.paralog/gene para))
        :species (if-let [[_ genus species] (re-matches #"^(\w)\w*\s+(.*)"
                                                        (:species/id (:gene.paralog/species para)))]
                   {:genus genus :species species})
        :method (map (partial datomic-rest-api.rest.object/pack-obj) (:evidence/from-analysis para))})
     (:gene/paralog gene))
   :description
   "precalculated ortholog assignments for this gene"})

(defn- protein-domains [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find [?motif ...]
            :in $ ?gene
            :where [?gene :gene/corresponding-cds ?gcds]
                   [?gcds :gene.corresponding-cds/cds ?cds]
                   [?cds :cds/corresponding-protein ?cprot]
                   [?cprot :cds.corresponding-protein/protein ?prot]
                   [?homol :locatable/parent ?prot]
                   [?homol :homology/motif ?motif]
                   [?motif :motif/id ?mid]
                   [(.startsWith ^String ?mid "INTERPRO:")]]
          db (:db/id gene))
       (map
         (fn [motif-id]
           (let [motif (entity db motif-id)]
             [(first (:motif/title motif))
              (datomic-rest-api.rest.object/pack-obj "motif" motif)])))
       (into {}))
     :description
     "protein domains of the gene"}))

(defn- best-blastp-matches [gene]
  (let [db    (d/entity-db gene)]
    (if-let [[prot length]
             (->> (q '[:find ?prot ?pep-len
                       :in $ ?gene
                       :where [?gene :gene/corresponding-cds ?gcds]
                              [?gcds :gene.corresponding-cds/cds ?cds]
                              [?cds :cds/corresponding-protein ?cprot]
                              [?cprot :cds.corresponding-protein/protein ?prot]
                              [?prot :protein/peptide ?pep]
                              [?pep :protein.peptide/length ?pep-len]]
                     db (:db/id gene))
                  (sort-by second)
                  (last))]   ; longest protein
      {:data
       {:hits
        (->>
          (q '[:find ?prot ?score ?min ?max
               :in $ ?ref
               :where [?homol :locatable/parent ?ref]
                      [?homol :homology/protein ?prot]
                      [?homol :locatable/score ?score]
                      [?homol :locatable/min ?min]
                      [?homol :locatable/max ?max]]
             db prot)
          (group-by (partial take 2))   ; ?prot and ?score as groupers
          (map
            (fn [[[prot score] lines]]
              (let [prot (entity db prot)]
                {:protein prot
                 :species (:protein/species prot)
                 :score score
                 :coverage (reduce + (map (fn [[_ _ min max]] (- max min)) lines))})))
          (remove #(.startsWith (:protein/id (:protein %)) "MSP"))
          (group-by :species)
          (map
            (fn [[_ hits]]
              (let [{:keys [protein species score coverage]}
                    (->> (sort-by :score hits)
                         (last))]
                {:evalue (format "%7.3g" (Math/pow 10 (- score)))
                 :percent (format "%2.1f%%" (float (* 100 (/ coverage length))))
                 :taxonomy (if-let [[_ g spec] (re-matches #"(.).* +(.+)" (or (:species/id species) ""))]
                             {:genus   g
                              :species spec})
                 :hit (datomic-rest-api.rest.object/pack-obj protein)
                 :description
                 (or
                   (:protein/description protein)
                   (:protein/gene-name protein)
                   (->> (:cds.corresponding-protein/_protein protein)
                        (first)
                        (:cds/_corresponding-protein)
                        (:cds/brief-identification)
                        (:cds.brief-identification/text))
                   "unknown")}))))}
       :description
       "best BLASTP hits from selected species"}
      {:data         nil
       :description  "no proteins found, no best blastp hits to display"})))



(def nematode-species
  ["Ancylostoma ceylanicum"
   "Ascaris suum"
   "Brugia malayi"
   "Bursaphelenchus xylophilus"
   "Caenorhabditis angaria"
   "Caenorhabditis brenneri"
   "Caenorhabditis briggsae"
   "Caenorhabditis elegans"
   "Caenorhabditis japonica"
   "Caenorhabditis remanei"
   "Caenorhabditis sp. 5"
   "Caenorhabditis tropicalis"
   "Dirofilaria immitis"
   "Haemonchus contortus"
   "Heterorhabditis bacteriophora"
   "Loa loa"
   "Meloidogyne hapla"
   "Meloidogyne incognita"
   "Necator americanus"
   "Onchocerca volvulus"
   "Panagrellus redivivus"
   "Pristionchus exspectatus"
   "Pristionchus pacificus"
   "Strongyloides ratti"
   "Trichinella spiralis"
   "Trichuris suis"])

(def-rest-widget homology [gene]
  {:name                (name-field gene)
   :nematode_orthologs  (homology-orthologs gene nematode-species)
   :human_orthologs     (homology-orthologs gene ["Homo sapiens"])
   :other_orthologs     (homology-orthologs-not gene (conj nematode-species "Homo sapiens"))
   :paralogs            (homology-paralogs gene)
   :best_blastp_matches (best-blastp-matches gene)
   :protein_domains     (protein-domains gene)})

;;
;; History widget
;;

(defn- history-events [gene]
  (let [data
   (->>
    (:gene/version-change gene)
    (mapcat
     (fn [h]
       (let [result {:version (:gene.version-change/version h)
 ;;                    :date    (datomic-rest-api.helpers/format-date2 (:gene.version-change/date h))
                     :curator (datomic-rest-api.rest.object/pack-obj "person" (:gene.version-change/person h))
                     :remark  nil
                     :date    (datomic-rest-api.helpers/format-date (:gene.version-change/date h))
                     :type    "Version_change"
                     :gene    nil
                     :action  "Unknown"}]
         (those
           (if (:gene-history-action/created h)
             (assoc result :action "Created"))

           (if (:gene-history-action/killed h)
             (assoc result :action "Killed"))

           (if (:gene-history-action/suppressed h)
             (assoc result :action "Suppressed"))

           (if (:gene-history-action/resurrected h)
             (assoc result :action "Resurrected"))

           (if (:gene-history-action/transposon-in-origin h)
             (assoc result :action "Transposon_in_origin"))

           (if (:gene-history-action/changed-class h)
             (assoc result :action "Changed_class"))

           (if-let [info (:gene-history-action/merged-into h)]
             (assoc result :action "Merged_into"
                    :gene (datomic-rest-api.rest.object/pack-obj "gene" info)))

           (if-let [info (:gene-history-action/acquires-merge h)]
             (assoc result :action "Acquires_merge"
                    :gene (datomic-rest-api.rest.object/pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-from h)]
             (assoc result :action "Split_from"
                    :gene (datomic-rest-api.rest.object/pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-into h)]
             (assoc result :action "Split_into"
                    :gene (datomic-rest-api.rest.object/pack-obj "gene" info)))

           (if-let [info (:gene-history-action/imported h)]
             (assoc result :action "Imported"
                    :remark (first info)))

           (if-let [name (:gene-history-action/cgc-name-change h)]
             (assoc result :action "CGC_name" :remark name))

           (if-let [name (:gene-history-action/other-name-change h)]
             (assoc result :action "Other_name" :remark name))

           (if-let [name (:gene-history-action/sequence-name-change h)]
             (assoc result :action "Sequence_name" :remark name)))))))]
  {:data  (if (empty? data) nil data)
   :description
   "the curatorial history of the gene"}))


(defn- old-annot [gene]
  (let [db (d/entity-db gene)]
    {:data (if-let [data
                    (->> (q '[:find [?historic ...]
                              :in $ ?gene
                              :where (or
                                       [?gene :gene/corresponding-cds-history ?historic]
                                       [?gene :gene/corresponding-pseudogene-history ?historic]
                                       [?gene :gene/corresponding-transcript-history ?historic])]
                            db (:db/id gene))
                         (map (fn [hid]
                                (let [hobj (datomic-rest-api.rest.object/pack-obj (entity db hid))]
                                  {:class (clojure.string/upper-case (:class hobj))
                                   :name hobj})))
                         (seq))] data)
     :description "the historical annotations of this gene"}))

(def-rest-widget history [gene]
  {:name      (name-field gene)
   :history   (history-events gene)
   :old_annot (old-annot gene)})


;;
;; Sequence widget
;;

(defn- gene-models [gene]      ;; Probably needs more testing for non-coding/pseudogene/etc. cases.
  (let [db      (d/entity-db gene)
        coding? (:gene/corresponding-cds gene)
        seqs (q '[:find [?seq ...]
                  :in $ % ?gene
                  :where (or
                           (gene-transcript ?gene ?seq)
                           (gene-cdst-or-cds ?gene ?seq)
                           (gene-pseudogene ?gene ?seq))]
                db
                '[[(gene-transcript ?gene ?seq) [?gene :gene/corresponding-transcript ?ct]
                   [?ct :gene.corresponding-transcript/transcript ?seq]]
                  ;; Per Perl code, take transcripts if any exist, otherwise take the CDS itself.
                  [(gene-cdst-or-cds ?gene ?seq) [?gene :gene/corresponding-cds ?cc]
                   [?cc :gene.corresponding-cds/cds ?cds]
                   [?ct :transcript.corresponding-cds/cds ?cds]
                   [?seq :transcript/corresponding-cds ?ct]]
                  [(gene-cdst-or-cds ?gene ?seq) [?gene :gene/corresponding-cds ?cc]
                   [?cc :gene.corresponding-cds/cds ?seq]
                   (not [_ :transcript.corresponding-cds/cds ?seq])]
                  [(gene-pseudogene ?gene ?seq) [?gene :gene/corresponding-pseudogene ?cp]
                   [?cp :gene.corresponding-pseudogene/pseudogene ?seq]]]
                (:db/id gene))]
    {:data
     (->>
       (map (partial entity db) seqs)
       (sort-by (some-fn :cds/id :transcript/id :pseudogene/id))
       (reduce
        (fn [{:keys [table remark-map]} sequence]
          (let [cds (or
                     (and (:cds/id sequence) sequence)
                     (:transcript.corresponding-cds/cds (:transcript/corresponding-cds sequence))
                     (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds sequence)))
                protein (:cds.corresponding-protein/protein (:cds/corresponding-protein cds))
                seqs (or (seq (map :transcript.corresponding-cds/_cds (:transcript/_corresponding-cds cds)))
                         [sequence])
                status (str (datomic-rest-api.rest.object/humanize-ident (:cds/prediction-status cds))
                            (if (:cds/matching-cdna cds)
                              " by cDNA(s)"))
                {:keys [remark-map footnotes]}
                (reduce (fn [{:keys [remark-map footnotes]} r]
                          (let [pr (if-let [ev (datomic-rest-api.rest.object/get-evidence r)]
                                     {:text     (:cds.remark/text r)
                                      :evidence ev}
                                     (:cds.remark/text r))]
                            (if-let [n (get remark-map pr)]
                              {:remark-map remark-map
                               :footnotes  (conjv footnotes n)}
                              (let [n (inc (count remark-map))]
                                {:remark-map (assoc remark-map pr n)
                                 :footnotes  (conjv footnotes n)}))))
                        {:remark-map remark-map}
                        (:cds/remark cds))]
            {:remark-map
             remark-map
             :table
             (conjv table
              (vmap
               :model
               (map datomic-rest-api.rest.object/pack-obj seqs)

               :protein
               (datomic-rest-api.rest.object/pack-obj "protein" protein)

               :cds
               (vmap
                :text (vassoc (datomic-rest-api.rest.object/pack-obj "cds" cds) :footnotes footnotes)
                :evidence (if (not (empty? status))
                            {:status status}))

               :length_spliced
               (if coding?
                 (if-let [exons (seq (:cds/source-exons cds))]
                   (->> (map (fn [ex]
                               (- (:cds.source-exons/end ex)
                                  (:cds.source-exons/start ex)
                                  -1))
                             exons)
                        (reduce +))))

               :length_unspliced
               (str/join
                "<br>"
                (for [s seqs]
                  (if (and (:locatable/max s) (:locatable/min s))
                    (- (:locatable/max s) (:locatable/min s))
                    "-")))

               :length_protein
               (:protein.peptide/length (:protein/peptide protein))

               :type (if seqs
                       (if-let [mid (:method/id
                                     (or (:transcript/method sequence)
                                         (:pseudogene/method sequence)
                                         (:cds/method sequence)))]
                         (str/replace mid #"_" " ")))))}))
        {})
       ((fn [{:keys [table remark-map]}]
         (vmap
          :table table
          :remarks (if-not (empty? remark-map)
                     (into (sorted-map)
                           (for [[r n] remark-map]
                             [n r])))))))



     :description
     "gene models for this gene"}))

(def-rest-widget sequences [gene]
  {:name         (name-field gene)
   :gene_models  (gene-models gene)})

;;
;; Sequence Features widget
;;

(defn- associated-features [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
       (q '[:find [?f ...]
            :in $ ?gene
            :where [?fg :feature.associated-with-gene/gene ?gene]
                   [?f :feature/associated-with-gene ?fg]]
          db (:db/id gene))
       (map
         (fn [fid]
           (let [feature (entity db fid)]
             (vmap
               :name (datomic-rest-api.rest.object/pack-obj "feature" feature)
               :description (first (:feature/description feature))
               :method (-> (:feature/method feature)
                           (:method/id))
               :interaction (->> (:interaction.feature-interactor/_feature feature)
                                 (map #(datomic-rest-api.rest.object/pack-obj "interaction" (:interaction/_feature-interactor %)))
                                 (seq))
               :expr_pattern (->>
                               (q '[:find [?e ...]
                                    :in $ ?f
                                    :where [?ef :expr-pattern.associated-feature/feature ?f]
                                           [?e :expr-pattern/associated-feature ?ef]
                                           [?e :expr-pattern/anatomy-term _]]
                                  db fid)
                               (map
                                 (fn [eid]
                                   (let [expr (entity db eid)]
                                     {:text (map #(datomic-rest-api.rest.object/pack-obj "anatomy-term" (:expr-pattern.anatomy-term/anatomy-term %))
                                                 (:expr-pattern/anatomy-term expr))
                                      :evidence {:by (datomic-rest-api.rest.object/pack-obj "expr-pattern" expr)}})))
                               (seq))
               :bound_by (->> (:feature/bound-by-product-of feature)
                              (map #(datomic-rest-api.rest.object/pack-obj "gene" (:feature.bound-by-product-of/gene %)))
                              (seq))
               :tf  (datomic-rest-api.rest.object/pack-obj "transcription-factor" (:feature/transcription-factor feature))))))
       (seq))
     :description
     "Features associated with this Gene"}))

(defn- feature-image [gene]
  {:data {:class "genomic_location" ;; To populate this correctly we will need sequence data
          :id nil
          :label nil
          :pos_string nil
          :taxonomy (if-let [class (:gene/species gene)]
                      (if-let [[_ genus species] (re-matches #"^(.*)\s(.*)$" (:species/id class))]
                        (clojure.string/lower-case (clojure.string/join [(first genus) "_" species]))))
          :tracks ["GENES"
                   "RNASEQ_ASYMMETRIES"
                   "RNASEQ"
                   "RNASEQ_SPLICE"
                   "POLYSOMES"
                   "MICRO_ORF"
                   "DNASEI_HYPERSENSITIVE_SITE"
                   "REGULATORY_REGIONS"
                   "PROMOTER_REGIONS"
                   "HISTONE_BINDING_SITES"
                   "TRANSCRIPTION_FACTOR_BINDING_REGION"
                   "TRANSCRIPTION_FACTOR_BINDING_SITE"
                   "BINDING_SITES_PREDICTED"
                   "BINDING_SITES_CURATED"
                   "BINDING_REGIONS"]}
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def-rest-widget features [gene]
  {:feature_image (feature-image gene)
   :name       (name-field gene)
   :features   (associated-features gene)})

;;
;; Genetics widget
;;

(defn- reference-allele [gene]
  {:data (let [data
   (->> (:gene/reference-allele gene)
        (map :gene.reference-allele/variation)
        (map (partial datomic-rest-api.rest.object/pack-obj "variation")))]
     (if (empty? data) nil data))
   :description "the reference allele of the gene"})

(defn- is-cgc? [strain]
  (some #(= (->> (:strain.location/laboratory %)
                 (:laboratory/id))
            "CGC")
        (:strain/location strain)))

(defn- strain-list [strains]
  (seq
   (map (fn [strain]
          (vassoc
           (datomic-rest-api.rest.object/pack-obj "strain" strain)
           :genotype (:strain/genotype strain)
           :transgenes (datomic-rest-api.rest.object/pack-obj "transgene" (first (:transgene/_strain strain)))))
        strains)))

(defn- strains [gene]
  (let [strains (:gene/strain gene)]
    {:data
     (vmap
      :carrying_gene_alone_and_cgc
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (is-cgc? %))
                           strains))

      :carrying_gene_alone
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (not (is-cgc? %)))
                           strains))

      :available_from_cgc
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (is-cgc? %))
                           strains))

      :others
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (not (is-cgc? %)))
                           strains)))

     :description
     "strains carrying this gene"}))

(defn- process-aa-change [molecular-change]
  (cond-let [n]
            (first (:molecular-change/missense molecular-change))
            (:molecular-change.missense/text n)

            (:molecular-change/nonsense molecular-change)
            (:molecular-change.nonsense/text n)))

(defn- process-aa-position [molecular-change]
  (cond-let [n]
            (first (:molecular-change/missense molecular-change))
            (:molecular-change.missense/int n)

            (:molecular-change/nonsense molecular-change)
            (nth (re-find #"\((\d+)\)" (:molecular-change.nonsense/text n)) 1))
  )

(defn- process-aa-composite [molecular-change]
  (let [change (process-aa-change molecular-change)
        position (process-aa-position molecular-change)]
    (if (and change position)
      (let [change-parts (str/split change #"\s+")]
        (->>
         [(first change-parts) position (nth change-parts 2)]
         (map str/capitalize)
         (str/join ""))))))

(defn- process-variation [var]
  (let [cds-changes (seq (take 20 (:variation/predicted-cds var)))
        trans-changes (seq (take 20 (:variation/transcript var)))
        gene-changes (seq (take 20 (:variation/gene var)))]
    (vmap
     :variation
     (datomic-rest-api.rest.object/pack-obj "variation" var)

     :type
     (if (:variation/transposon-insertion var)
       "transposon insertion"
       (str/join ", "
                 (or
                  (those
                   (if (:variation/engineered-allele var)
                     "Engineered allele")
                   (if (:variation/allele var)
                     "Allele")
                   (if (:variation/snp var)
                     "SNP")
                   (if (:variation/confirmed-snp var)
                     "Confirmed SNP")
                   (if (:variation/predicted-snp var)
                     "Predicted SNP")
                   (if (:variation/reference-strain-digest var)
                     "RFLP"))
                  ["unknown"])))

     :method_name
     (if-let [method (:variation/method var)]
       (format "<a class=\"longtext\" tip=\"%s\">%s</a>"
               (or (:method.remark/text (first (:method/remark methods))) "")
               (str/replace (:method/id method) #"_" " ")))

     :gene
     nil ;; don't populate since we're coming from gene...

     :molecular_change
     (cond
       (:variation/substitution var)
       "Substitution"

       (:variation/insertion var)
       "Insertion"

       (:variation/deletion var)
       "Deletion"

       (:variation/inversion var)
       "Inversion"

       (:variation/tandem-duplication var)
       "Tandem_duplication"

       :default
       "Not curated")

     :locations
     (let [changes (set (mapcat keys (concat cds-changes gene-changes trans-changes)))]
       (filter
        identity
        (map {:molecular-change/intron "Intron"
              :molecular-change/coding-exon "Coding exon"
              :molecular-change/utr-5 "5' UTR"
              :molecular-change/utr-3 "3' UTR"}
             changes)))

     :effects
     (let [changes (set (mapcat keys (concat cds-changes gene-changes trans-changes)))]
       (if-let [effect (set (filter
                             identity
                             (map {:molecular-change/missense "Missense"
                                   :molecular-change/nonsense "Nonsense"
                                   :molecular-change/frameshift "Frameshift"
                                   :molecular-change/silent "Silent"
                                   :molecular-change/splice-site "Splice site"
                                   :molecular-change/promoter "Promoter"
                                   :molecular-change/genomic-neighbourhood "Genomic neighbourhood"
                                   :molecular-change/regulatory-feature "Regulatory feature"
                                   :molecular-change/readthrough "Readthrough"}
                                  changes)))]
         (if (empty? effect) nil effect)))

     :aa_change
     (if-let [aa-changes (->> (map process-aa-change cds-changes)
                              (filter identity))]
       (if (empty? aa-changes) nil aa-changes))

     :aa_position
     (if-let [aa-positions (->> (map process-aa-position cds-changes)
                                (filter identity))]
       (if (empty? aa-positions) nil aa-positions))

     :composite_change
     (if-let [aa-changes (->> (map process-aa-composite cds-changes)
                              (filter identity))]
       (if (empty? aa-changes) nil aa-changes))

     :isoform
     (if-let [isoform
              (seq
               (for [cc cds-changes
                     :when (or (:molecular-change/missense cc)
                               (:molecular-change/nonsense cc))]
                 (datomic-rest-api.rest.object/pack-obj "cds" (:variation.predicted-cds/cds cc))))]
       (if (empty? isoform) nil isoform))

     :phen_count
     (count (:variation/phenotype var))

     :strain
     (map #(datomic-rest-api.rest.object/pack-obj "strain" (:variation.strain/strain %)) (:variation/strain var))

     :sources
     (if-let [sources (if (empty? (:variation/reference var))
                        (map #(let [packed (datomic-rest-api.rest.object/pack-obj %)]
                                (into packed {:label
                                              (str/replace (:label packed) #"_" " ")})) (:variation/analysis var))
                        (map #(datomic-rest-api.rest.object/pack-obj (:variation.reference/paper %)) (:variation/reference var)))]
       (if (empty? sources) nil sources)))))

(defn- alleles [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?var ...]
               :in $ ?gene
               :where [?vh :variation.gene/gene ?gene]
                      [?var :variation/gene ?vh]
                      [?var :variation/phenotype _]]
             db (:db/id gene))
          (map #(process-variation (entity db %))))
     :description "alleles and polymorphisms with associated phenotype"}))

(defn- alleles-other [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?var ...]
               :in $ ?gene
               :where [?vh :variation.gene/gene ?gene]
                      [?var :variation/gene ?vh]
                      [?var :variation/allele _]
                      (not [?var :variation/phenotype _])]
             db (:db/id gene))
          (map #(process-variation (entity db %))))
     :description "alleles currently with no associated phenotype"}))

(defn- polymorphisms [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?var ...]
               :in $ ?gene
               :where [?vh :variation.gene/gene ?gene]
                      [?var :variation/gene ?vh]
                      (not [?var :variation/allele _])
                      (not [?var :variation/phenotype _])]
             db (:db/id gene))
          (map #(process-variation (entity db %))))
     :description "polymorphisms and natural variations currently with no associated phenotype"}))

(defn- alleles-count [gene]
  (let [db (d/entity-db gene)]
    {:data
     (-> {}
         (assoc :polymorphisms (q '[:find (count ?var) .
                                    :in $ ?gene
                                    :where [?vh :variation.gene/gene ?gene]
                                           [?var :variation/gene ?vh]
                                           (not [?var :variation/allele _])
                                           (not [?var :variation/phenotype _])]
                                  db (:db/id gene)))
         (assoc :alleles_other (q '[:find (count ?var) .
                                    :in $ ?gene
                                    :where [?vh :variation.gene/gene ?gene]
                                           [?var :variation/gene ?vh]
                                           [?var :variation/allele _]
                                           (not [?var :variation/phenotype _])]
                                  db (:db/id gene))))
     :description "counts for alleles-other and polymorphisms"}))

(defn- rearrangements-positive [gene]
  (let [db (d/entity-db gene)]
    (->> (q '[:find [?ra ...]
               :in $ ?gene
               :where [?rag :rearrangement.gene-inside/gene ?gene]
                      [?ra :rearrangement/gene-inside ?rag]]
            db (:db/id gene))
         (map #(datomic-rest-api.rest.object/pack-obj (entity db %))))))

(defn- rearrangements-negative [gene]
   (let [db (d/entity-db gene)]
    (->> (q '[:find [?ra ...]
               :in $ ?gene
               :where [?rag :rearrangement.gene-outside/gene ?gene]
                      [?ra :rearrangement/gene-outside ?rag]]
            db (:db/id gene))
         (map #(datomic-rest-api.rest.object/pack-obj (entity db %))))))

(defn- rearrangements [gene]
  {:data (let [data {:positive (if-let [rearrangements (rearrangements-positive gene)]
                                 (if (empty? rearrangements) nil rearrangements))
                     :negative (if-let [rearrangements (rearrangements-negative gene)]
                                 (if (empty? rearrangements) nil rearrangements))}]
           (if (or (:positive data) (:negative data)) data nil))
   :description "rearrangements involving this gene"})


(defn genetics [gene]
  {:reference_allele (reference-allele gene)
   :rearrangements   (rearrangements gene)
   :strains          (strains gene)
   :alleles          (alleles gene)
   :alleles_count    (alleles-count gene)
;;   :alleles_other    (alleles-other gene)  ;; can be requested through /rest/field/
;;   :polymorphisms    (polymorphisms gene)  ;; can be requested through /rest/field/
   :name             (name-field gene)
   })

;;
;; external_links widget
;;

(defn- xrefs [gene]
  {:data
   (reduce
     (fn [refs db]
       (update-in refs
                  [(:database/id (:gene.database/database db))
                   (:database-field/id (:gene.database/field db))
                   :ids]
                  conjv
                  (let [acc (:gene.database/accession db)]
                    (if-let [[_ rest] (re-matches #"(?:OMIM:|GI:)(.*)" acc)]
                      rest
                      acc))))
     {}
     (:gene/database gene))
   :description
   "external databases and IDs containing additional information on the object"})

(def-rest-widget external-links [gene]
  {:name  (name-field gene)
   :xrefs (xrefs gene)})
