(ns datomic-rest-api.rest.fields.gene
  (:require [cheshire.core :as c :refer (generate-string)]
            [pseudoace.binning :refer (reg2bins xbin bins)]
            [datomic-rest-api.rest.helpers.date :as date-helper]
            [datomic-rest-api.rest.helpers.object :as rest-api-obj :refer (humanize-ident get-evidence author-list pack-obj)]
            [datomic-rest-api.rest.helpers.expression :as expression]
            [datomic.api :as d :refer (db q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap vmap-if vassoc cond-let those conjv]]
            [pseudoace.locatables :refer (root-segment)]
            [datomic-rest-api.db.sequence :refer (get-default-sequence-database)]
            [datomic-rest-api.helpers.sequence :refer (sequence-features)]
            [datomic-rest-api.helpers.species :as helpers.species :refer (parse-species-name)]
            [datomic-rest-api.rest.core :refer [def-rest-widget]]))

;;
;; "name" field, included on all widgets.
;;

(defn name-field [gene]
  (let [data (pack-obj "gene" gene)]
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

(defn transcript-type [transcript]
  (some transcript-types (keys transcript)))

(defn gene-classification [gene]
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

(defn gene-class [gene]
  {:data
   (if-let [class (:gene/gene-class gene)]
     {:tag (pack-obj "gene-class" class)
      :description (str (first (:gene-class/description class)))})
   :description "The gene class for this gene"})

(defn gene-operon [gene]
  {:data
   (if-let [operon (->> (:operon.contains-gene/_gene gene)
                        (first)
                        (:operon/_contains_gene))]
     (pack-obj "operon" operon))
   :description "Operon the gene is contained in"})

(defn gene-cluster [gene]
  {:data
   ;; discuss - should be gene-class? referenced in overview widget...
   ;; (if-let [data (->> (:gene-class/main-name/text gene))]
   ;;   data)
   nil
   :description "The gene cluster for this gene"})

(defn gene-other-names [gene]
   {:data (if-let [data (map #(get % "gene.other-name/text") (:gene/other-name gene))]
             data)
    :description (format "other names that have been used to refer to %s" (:gene/id gene))})

(defn gene-status [gene]
  {:data (if-let [class (:gene/status gene)]
           (humanize-ident (:gene.status/status class)))
   :description (format "current status of the Gene:%s %s" (:gene/id gene) "if not Live or Valid")})

(defn gene-taxonomy [gene]
  {:data (if-let [class (:gene/species gene)]
           (if-let [[_ genus species] (re-matches #"^(.*)\s(.*)$" (:species/id class))]
             {:genus genus :species species}
             {:genus (:gene/species gene)}))
   :description "the genus and species of the current object"})

(defn concise-description [gene]
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
      :evidence (or (get-evidence desc)
                    (get-evidence (first (:gene/provisional-description gene))))}
     {:text nil :evidence nil})
   :description "A manually curated description of the gene's function"})

(defn curatorial-remarks [gene]
  (let [data
        (->> (:gene/remark gene)
             (map (fn [rem]
                    {:text (:gene.remark/text rem)
                     :evidence (get-evidence rem)}))
             (seq))]
    {:data (if (empty? data) nil data)
     :description "curatorial remarks for the Gene"}))

(defn legacy-info [gene]
  {:data
   (if-let [data (seq (map :gene.legacy-information/text (:gene/legacy-information gene)))] data)
   :description
   "legacy information from the CSHL Press C. elegans I/II books"})

(defn named-by [gene]
  {:data
   (if-let [data (->> (:gene/cgc-name gene)
                      (get-evidence)
                      (mapcat val))]
     data)
   :description
   "the source where the approved name was first described"})

(defn parent-sequence [gene]
  {:data
   (if-let [data (pack-obj (:locatable/parent gene))]
     data)
   :description
   "parent sequence of this gene"})

(defn parent-clone [gene]
  (let [db (d/entity-db gene)
        data
        (->> (q '[:find [?clone ...]
                  :in $ ?gene
                  :where [?cg :clone.positive-gene/gene ?gene]
                         [?clone :clone/positive-gene ?cg]]
                db (:db/id gene))
             (map (fn [cid]
                    (let [clone (entity db cid)]
                      (pack-obj "clone" clone))))
             (seq))]
    {:data (if (empty? data) nil data)
     :description
     "parent clone of this gene"}))

(defn cloned-by [gene]
  {:data
   (if-let [ev (get-evidence (first (:gene/cloned-by gene)))]
     {:cloned_by (key (first ev))
      :tag       (key (first ev))
      :source    (first (val (first ev)))})
   :description
   "the person or laboratory who cloned this gene"})

(defn transposon [gene]
  {:data
   (if-let [data (pack-obj (first (:gene/corresponding-transposon gene)))]
     data)
   :description
   "Corresponding transposon for this gene"})

(defn sequence-name [gene]
  {:data
   (or (:gene/sequence-name gene)
       "unknown")
   :description
   "the primary corresponding sequence name of the gene, if known"})

(defn locus-name [gene]
  {:data
   (if-let [cgc (:gene/cgc-name gene)]
     (pack-obj "gene" gene :label (:gene.cgc-name/text cgc))
     "not assigned")
   :description "the locus name (also known as the CGC name) of the gene"})

(defn disease-relevance [gene]
  {:data (if-let [data
                  (->> (:gene/disease-relevance gene)
                       (map (fn [rel]
                              {:text (:gene.disease-relevance/note rel)
                               :evidence (get-evidence rel)}))
                       (seq))]
           data)
   :description
   "curated description of human disease relevance"})

(defn gene-version [gene]
  (let [data (str (:gene/version gene))]
    {:data (if (empty? data) nil data)
     :description "the current WormBase version of the gene"}))

(defn also-refers-to [gene]
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
                      (map #(pack-obj "gene" (entity db %)))
                      (seq))]
             data)
     :description
     "other genes that this locus name may refer to"}))

(defn merged-into [gene]
  (let [db (d/entity-db gene)
        data
        (->> (q '[:find ?merge-partner .
                  :in $ ?gene
                  :where [?gene :gene/version-change ?vc]
                         [?vc :gene-history-action/merged-into ?merge-partner]]
                db (:db/id gene))
             (entity db)
             (pack-obj "gene"))]
    {:data (if (empty? data) nil data)
     :description "the gene this one has merged into"}))

(defn- get-structured-description [gene type]
  (let [key     (keyword "gene" type)
        txt-key (keyword (str "gene." type) "text")]
    (->> (key gene)
         (map (fn [data]
                {:text     (txt-key data)
                 :evidence (get-evidence data)}))
         (seq))))

(defn structured-description [gene]
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
                      :evidence (get-evidence p)})))

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
   :label (str (rest-api-obj/author-list paper)
               ", "
               (if (= nil (:paper/publication-date paper))
                 ""
                 (first (str/split (:paper/publication-date paper) #"-"))))})

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn- create-pato-term [id label entity-term entity-type pato-term]
  (let [pato-id  (str/join "_" [id label pato-term])]
    {pato-id
     {:pato_evidence
      {:entity_term entity-term
       :entity_type label
       :pato_term pato-term}
      :key pato-id}}))

(defn- get-pato-from-holder [holder]
  (let [sot (for [eq-annotations {"anatomy-term" "anatomy-term"
                                  "life-stage" "life-stage"
                                  "go-term" "go-term"
                                  "molecule-affected" "molecule"}
                  :let [[eq-key label] eq-annotations]]
              (for [eq-term ((keyword "phenotype-info" eq-key) holder)]
                (let [make-key (partial keyword (str "phenotype-info." eq-key))
                      pato-name (first (:pato-term/name (-> eq-term ((make-key "pato-term")))))
                      id ((keyword eq-key "id")  (-> eq-term ((make-key eq-key))))
                      entity-term (pack-obj label (-> eq-term ((make-key label))))
                      pato-term (if (nil? pato-name) "abnormal" pato-name)]
                  (if (nil? id) nil (create-pato-term id label entity-term (str/capitalize (str/replace eq-key #"-" "_")) pato-term)))))
        var-combo (into {} (for [x sot] (apply merge x)))]
    {(str/join "_" (sort (keys var-combo))) (vals var-combo)}))

(defn- get-pato-combinations [db pid rnai-phenos var-phenos not?]
  (if-let [vp (distinct (concat (rnai-phenos pid) (var-phenos pid)))]
    (let [patos (for [v vp
                         :let [holder (entity db v)]]
                     (get-pato-from-holder holder))]
      (apply merge patos))))

(def not-nil? (complement nil?))

(defn create-tag [label]
  {:taxonomy "all"
   :class "tag"
   :label label
   :id  label})

(defn var-evidence [holder variation pheno]
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
           (:phenotype-info/remark holder)))

    :Recessive
    (if (contains? holder :phenotype-info/recessive) "")

    :Quantity_description
    (seq
      (map :phenotype-info.quantity-description/text
           (:phenotype-info/quantity-description holder)))

    :Dominant
    (if
      (contains? holder :phenotype-info/dominant) "")

    :Semi_dominant
    (if
      (contains? holder :phenotype-info/semi-dominant)
      (let [sd (:phenotype-info/semi-dominant holder)]
        (remove
          nil?
           [(if
             (contains? sd :evidence/person-evidence)
             (create-tag "Person_evidence"))
           (if
             (contains? sd :evidence/curator-confirmed)
             (create-tag "Curator_confirmed"))
           (if
             (contains? sd :evidence/paper-evidence)
             (create-tag "Paper_evidence"))])))

    :Penetrance
    (first
      (remove nil?
	      (flatten
	       (conj
		(if
		 (contains? holder :phenotype-info/low)
		 (for [low-holder (:phenotype-info/low holder)
		       :let [text (:phenotype-info.low/text low-holder)]]
				  (if (= text "") nil text)))
		(if
		 (contains? holder :phenotype-info/high)
		 (for [high-holder (:phenotype-info/high holder)
		       :let [text (:phenotype-info.high/text high-holder)]]
				   (if (= text "") nil text)))
		(if
		 (contains? holder :phenotype-info/complete)
		 (for [complete-holder (:phenotype-info/complete holder)
		       :let [text (:phenotype-info.complete/text complete-holder)]]
				       (if (= text "") nil text)))))))

    :Penetrance-range
    (if (not-nil? (:phenotype-info/range holder))
      (let [range-holder (:phenotype-info/range holder)]
        (if
          (contains? range-holder :phenotype-info.range/int-b)
          (let [range (str/join
            "-"
            [(str
               (:phenotype-info.range/int-a range-holder))
             (str
               (:phenotype-info.range/int-b range-holder))])]
            (if
              (= range "100-100")
              "100%"
              range))
          (:phenotype-info.range/int-a range-holder))))

    :Maternal
    (if
      (contains? holder :phenotype-info/maternal)
      (create-tag
        (humanize-ident
          (:phenotype-info.maternal/value
            (:phenotype-info/maternal holder)))))

    :Paternal
    (if
      (contains? holder :phenotype-info/paternal)
      (create-tag
        (humanize-ident
          (:phenotype-info.paternal/value
            (:phenotype-info/paternal holder)))))

    :Haplo_insufficient
    (if
      (contains? holder :phenotype-info/haplo-insufficient)
      (create-tag
        (humanize-ident
          (:phenotype-info.paternal/value
            (:phenotype-info/haplo-insufficient holder)))))

    :Variation_effect
    (if (contains? holder :phenotype-info/variation-effect)
      (first ; we should actually display all of them but catalyst template not displaying nested array
        (for [ve (:phenotype-info/variation-effect holder)]
        (remove
          nil?
          [(create-tag
             (humanize-ident (:phenotype-info.variation-effect/value ve)))
           (if
             (contains? ve :evidence/person-evidence)
             (create-tag "Person_evidence"))
           (if
             (contains? ve :evidence/curator-confirmed)
             (create-tag "Curator_confirmed"))
           (if
             (contains? ve :evidence/paper-evidence)
             (create-tag "Paper_evidence"))]))))

    :Affected_by_molecule
    (if
      (contains? holder :phenotype-info/molecule)
      (for [m (:phenotype-info/molecule holder)]
        (pack-obj (:phenotype-info.molecule/molecule m))))

    :Affected_by_pathogen
    (if
      (contains? holder :phenotype-info/pathogen)
      (for [m (:phenotype-info/pathogen holder)]
        (pack-obj (:phenotype-info.molecule/species m))))

    :Ease_of_scoring
    (if
      (contains? holder :phenotype-info/ease-of-scoring)
      (create-tag
        (humanize-ident
          (:phenotype-info.ease-of-scoring/value
            (:phenotype-info/ease-of-scoring holder)))))

    :Phenotype_assay
    (if
      (contains? pheno :phenotype/assay)
      (let [holder (:phenotype/assay pheno)]
        (:phenotype.assay/text holder)))

    :Male_mating_efficiency
    (if
      (contains? variation :variation/male-mating-efficiency)
      (humanize-ident
        (:variation.male-mating-efficiency/value
          (:variation/male-mating-efficiency variation))))


    :Temperature_sensitive
    (if
      (or
        (contains? holder :phenotype-info/heat-sensitive)
        (contains? holder :phenotype-info/cold-sensitive))
        (conj
          (if (contains? holder :phenotype-info/heat-sensitive)
            (create-tag "Heat-sensitive"))
          (if (contains? holder :phenotype-info/cold-sensitive)
            (create-tag "Cold-sensitive"))))

    :Strain
    nil

    :Treatment
    (if
      (contains? holder :phenotype-info/treatment)
      (first (for [treatment-holder (:phenotype-info/treatment holder)
    	:let [text (:phenotype-info.treatment/text treatment-holder)]]
        (if (= text "") nil text))))

    :Temperature
    (if
      (contains? holder :phenotype-info/temperature)
      (first (for [temp-holder (:phenotype-info/temperature holder)
    	:let [text (:phenotype-info.temperature/text temp-holder)]]
        (if (= text "") nil text))))

    :Ease_of_scoring
    nil
    ))

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
            {:class
             "variation"

             :id
             (:variation/id var)

             :label
             (:variation/public-name var)

             :style
             (if (= (:variation/seqstatus var)
                    :variation.seqstatus/sequenced)
               "font-weight:bold"
               0)

             :taxonomy
             "c_elegans"
             }
            :evidence (var-evidence holder var pheno)})))

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
              {:Genotype
               (:rnai/genotype rnai)

               :Strain
               (:strain/id (:rnai/strain rnai))

               :paper
               (if-let [paper (:rnai.reference/paper (:rnai/reference rnai))]
                 (evidence-paper paper))}
              (var-evidence holder rnai pheno))}))))})

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

(defn phenotype-not-observed-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) true)]
    {:data (if (empty? data) nil data)
     :description "The Phenotype not observed summary of the gene"}))

(defn phenotype-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) false)]
    {:data (if (empty? data) nil data)
     :description "The Phenotype summary of the gene"}))

(defn phenotype-by-interaction [gene]
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
                           [pid (pack-obj "phenotype" (entity db pid))]))
                    (into {}))
        ints (->> (mapcat second table)
                  (set)
                  (map (fn [iid]
                         (let [int (entity db iid)]
                           [iid
                            {:interaction (pack-obj "interaction" int)
                             :citations (map (partial pack-obj "paper") (:interaction/paper int))}])))
                  (into {}))
        data (map (fn [[pheno pints int-type]]
                    {:interaction_type
                     (rest-api-obj/humanize-ident int-type)

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
            transgeneobj (pack-obj "transgene" transgene)
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
            (pack-obj "person" curator))

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
            (pack-obj "gene" (:phenotype-info.caused-by-gene/gene cbg)))

          :Transgene transgeneobj

          :Paper_evidence
          (for [pe paperevidences]
            (pack-obj "paper" pe))

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
                              (pack-obj "phenotype" pheno)

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



;;
;; Mapping data widget
;;

;; Needs better support for non-gene things.

(defn gene-mapping-twopt [gene]
  {:data (let [db (d/entity-db gene)
               id (:db/id gene)]
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
                   {:mapper     (pack-obj "author" (first (:two-point-data/mapper tp)))
                    :date       (date-helper/format-date (:two-point-data/date tp))
                    :raw_data   (:two-point-data/results tp)
                    :genotype   (:two-point-data/genotype tp)
                    :comment    (let [comment (str/join "<br>" (map :two-point-data.remark/text (:two-point-data/remark tp)))]
                                  (if (empty? comment) "" comment ))
                    :distance   (format "%s (%s-%s)" (or (:two-point-data/calc-distance tp) "0.0")
                                        (or (:two-point-data/calc-lower-conf tp) "0")
                                        (or (:two-point-data/calc-upper-conf tp) "0"))
                    :point_1    (let [p1 (:two-point-data/gene-1 tp)]
                                  (remove nil? [(pack-obj "gene" (:two-point-data.gene-1/gene p1))
                                                (pack-obj "variation" (:two-point-data.gene-1/variation p1))]))
                    :point_2    (let [p2 (:two-point-data/gene-2 tp)]
                                  (remove nil? [(pack-obj "gene" (:two-point-data.gene-2/gene p2))
                                                (pack-obj "variation" (:two-point-data.gene-2/variation p2))]))}))
                (seq)))
   :description "Two point mapping data for this gene"}
  )

(defn gene-mapping-posneg [gene]
  {:data (let [db (d/entity-db gene)
               id (:db/id gene)]
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
                   (let [items (->> [(pack-obj ((some-fn (comp :pos-neg-data.gene-1/gene :pos-neg-data/gene-1)
                                                         (comp :pos-neg-data.locus-1/locus :pos-neg-data/locus-1)
                                                         :pos-neg-data/allele-1
                                                         :pos-neg-data/clone-1
                                                         :pos-neg-data/rearrangement-1)
                                                pn))
                                     (pack-obj ((some-fn (comp :pos-neg-data.gene-2/gene :pos-neg-data/gene-2)
                                                         (comp :pos-neg-data.locus-2/locus :pos-neg-data/locus-2)
                                                         :pos-neg-data/allele-2
                                                         :pos-neg-data/clone-2
                                                         :pos-neg-data/rearrangement-2)
                                                pn))]
                                    (map (juxt :label identity))
                                    (into {}))
                         result (str/split (:pos-neg-data/results pn) #"\s+")]
                     {:mapper    (pack-obj "author" (first (:pos-neg-data/mapper pn)))
                      :comment    (let [comment (str/join "<br>" (map :pos-neg-data.remark/text (:pos-neg-data/remark pn)))]
                                    (if (empty? comment) "" comment ))
                      :date      (date-helper/format-date (:pos-neg-data/date pn))
                      :result    (map #(or (items (str/replace % #"\." ""))
                                           (str % " "))
                                      result)})))
                (seq)))
   :description "Positive/Negative mapping data for this gene"}
  )

(defn gene-mapping-multipt [gene]
  {:data (let [db (d/entity-db gene)
               id (:db/id gene)]
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
                    :mapper   (pack-obj "author" (first (:multi-pt-data/mapper mp)))
                    :date     (if (nil? (:multi-pt-data/date mp)) "" (date-helper/format-date3 (str (:multi-pt-data/date mp))))
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
                                     (pack-obj obj)
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
                   ))
                (seq)))
   :description "Multi point mapping data for this gene"}
  )




;;
;; Human diseases widget
;;

(defn- construct-labs [construct]
  (seq (map #(pack-obj "laboratory" (:construct.laboratory/laboratory %))
            (:construct/laboratory construct))))

(defn- transgene-labs [tg]
  (seq (map #(pack-obj "laboratory" (:transgene.laboratory/laboratory %))
            (:transgene/laboratory tg))))


(defn- transgene-record [construct]
  (let [base {:construct (pack-obj "construct" construct)
              :used_in   (pack-obj "transgene" (first (:construct/transgene-construct construct)))
              :use_summary (:construct/summary construct)}]
    (cond-let [use]
      (:construct/transgene-construct construct)
      (for [t use]
        (assoc base :used_in_type "Transgene construct"
                    :use_summary (:transgene.summary/text (:transgene/summary t))
                    :used_in     (pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct)
                                     [])))

      (:construct/transgene-coinjection construct)
      (for [t use]
        (assoc base :used_in_type "Transgene coinjection"
                    :use_summary (:transgene.summary/text (:transgene/summary t))
                    :used_in     (pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct)
                                      [])))

      (:construct/engineered-variation construct)
      (for [v use]
        (assoc base :used_in_type "Engineered variation"
                    :used_in      (pack-obj "variation" v)
                    :use_lab      (construct-labs construct))))))

(def ^:private probe-types
  {:oligo-set.type/affymetrix-microarray-probe "Affymetrix"
   :oligo-set.type/washu-gsc-microarray-probe  "GSC"
   :oligo-set.type/agilent-microarray-probe    "Agilent"})

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

;;
;; New style GO widget
;;

(def ^:private division-names
  {:go-term.type/molecular-function "Molecular_function"
   :go-term.type/cellular-component "Cellular_component"
   :go-term.type/biological-process "Biological_process"})

(defn- go-anno-extension-fn [relation-type-tag target-tag]
  (fn [relation]
    [(relation-type-tag relation)
     (pack-obj (target-tag relation))]))

(defn- go-anno-extensions [anno]
  (->>
   (concat
    (map (go-anno-extension-fn :go-annotation.molecule-relation/text
                               :go-annotation.molecule-relation/molecule)
         (:go-annotation/molecule-relation anno))
    (map (go-anno-extension-fn :go-annotation.anatomy-relation/text
                               :go-annotation.anatomy-relation/anatomy-term)
         (:go-annotation/anatomy-relation anno))
    (map (go-anno-extension-fn :go-annotation.gene-relation/text
                               :go-annotation.gene-relation/gene)
         (:go-annotation/gene-relation anno))
    (map (go-anno-extension-fn :go-annotation.life-stage-relation/text
                               :go-annotation.life-stage-relation/life-stage)
         (:go-annotation/life-stage-relation anno))
    (map (go-anno-extension-fn :go-annotation.go-term-relation/text
                               :go-annotation.go-term-relation/go-term)
         (:go-annotation/go-term-relation anno)))
   (map (fn [[relation_type target]]
          {relation_type #{target}}))))

(defn- go-anno-xref [anno-db]
  (let [db-id (:database/id (:go-annotation.database/database anno-db))
        obj-id (:go-annotation.database/text anno-db)]
    {:id obj-id
     :class db-id
     :dbt (:database-field/id (:go-annotation.database/database-field anno-db))
     :label (str/join ":" [db-id obj-id])}))

(defn- term-table-full [db annos]
  (map
   (fn [{:keys [term code anno]}]
     {:anno_id
      (:go-annotation/id anno)

      :with
      (seq
       (concat
        (map pack-obj (:go-annotation/interacting-gene anno))
        (map pack-obj (:go-annotation/inferred-from-go-term anno))
        (map pack-obj (:go-annotation/motif anno))
        (map pack-obj (:go-annotation/rnai-result anno))
        (map pack-obj (:go-annotation/variation anno))
        (map pack-obj (:go-annotation/phenotype anno))
        (map go-anno-xref (:go-annotation/database anno))))

      :evidence_code
      {:text
       (:go-code/id code)

       :evidence
       (vmap
        :Date_last_updated
        (if-let [d (:go-annotation/date-last-updated anno)]
          [{:class "text"
            :id (date-helper/format-date3 (str d))
            :label (date-helper/format-date3 (str d))}])

        :Contributed_by
        [(pack-obj "analysis"
                   (:go-annotation/contributed-by anno))]
        :Reference
        (if (:go-annotation/reference anno)
          [(pack-obj "paper"
                     (:go-annotation/reference anno))])

        :GO_reference
        (if (:go-annotation/go-term-relation anno)
          (concat
           )))}

      :go_type
      (if-let [go-type (:go-term/type term)]
        (division-names go-type))

      :term (if-let [extensions (->> (go-anno-extensions anno)
                                     (apply (partial merge-with concat)))]
              {:evidence extensions})

      :term_id
      (pack-obj "go-term" term :label (:go-term/id term))

      :term_description
      (pack-obj "go-term" term)})
   annos))

(defn gene-ontology-full [gene]
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
            :term     (d/entity db term)
            :code     (d/entity db code)
            :anno     (d/entity db anno)}))
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
    (fn [[term term-annos]]
      (let [extensions (->> (map :anno term-annos)
                            (map go-anno-extensions)
                            (apply concat)
                            (apply (partial merge-with clojure.set/union)))]
        {:extensions extensions

         :term_id
         (pack-obj "go-term" term :label (:go-term/id term))

         :term_description
         (if (not-empty extensions)
           [(pack-obj term) {:evidence extensions}]
           [(pack-obj term)])

         })
      ))
   ))

(defn gene-ontology-summary [gene]
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
     (fn [[div term anno]]
       {:division div
        :term (d/entity db term)
        :anno (d/entity db anno)}))
    (group-by :division)
    (map
     (fn [[key annos]]
       [(division-names key)
        (term-summary-table db annos)]))
    (into {}))

   :description
   "gene ontology associations"}))



;;
;; Expression widget
;;

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

;; I haven't found an example for this to show that it works
(defn- curated-images [ep]
 (let [images (:picture/expr_pattern ep)]
     (map
      (fn [image]
        (pack-obj "picture" image))
      images)))

(defn fourd-expression-movies [gene]
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

(defn fpkm-expression-summary-ls [gene]
  (expression/fpkm-expression-summary-ls gene))

;;
;; Homology widget
;;

(defn- pack-ortholog [db oid]
  (let [ortho (entity db oid)]
    {:ortholog (pack-obj "gene" (:gene.ortholog/gene ortho))
     :species (if-let [[_ genus species] (re-matches #"^(\w)\w*\s+(.*)"
                                                     (:species/id (:gene.ortholog/species ortho)))]
                {:genus genus :species species})
     :method (map (partial pack-obj) (:evidence/from-analysis ortho))}))

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

;;
;; History widget
;;

(defn history-events [gene]
  (let [data
   (->>
    (:gene/version-change gene)
    (mapcat
     (fn [h]
       (let [result {:version (:gene.version-change/version h)
 ;;                    :date    (date-helper/format-date2 (:gene.version-change/date h))
                     :curator (pack-obj "person" (:gene.version-change/person h))
                     :remark  nil
                     :date    (date-helper/format-date (:gene.version-change/date h))
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
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/acquires-merge h)]
             (assoc result :action "Acquires_merge"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-from h)]
             (assoc result :action "Split_from"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-into h)]
             (assoc result :action "Split_into"
                    :gene (pack-obj "gene" info)))

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


(defn old-annot [gene]
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
                                (let [hobj (pack-obj (entity db hid))]
                                  {:class (clojure.string/upper-case (:class hobj))
                                   :name hobj})))
                         (seq))] data)
     :description "the historical annotations of this gene"}))




;;
;; Sequence widget
;;

(defn gene-models [gene]      ;; Probably needs more testing for non-coding/pseudogene/etc. cases.
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
                status (str (rest-api-obj/humanize-ident (:cds/prediction-status cds))
                            (if (:cds/matching-cdna cds)
                              " by cDNA(s)"))
                {:keys [remark-map footnotes]}
                (reduce (fn [{:keys [remark-map footnotes]} r]
                          (let [pr (if-let [ev (get-evidence r)]
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
               (map pack-obj seqs)

               :protein
               (pack-obj "protein" protein)

               :cds
               (vmap
                :text (vassoc (pack-obj "cds" cds) :footnotes footnotes)
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

;;
;; Sequence Features widget
;;

(defn associated-features [gene]
  (let [db (d/entity-db gene)
        data  (->>
                (q '[:find [?f ...]
                     :in $ ?gene
                     :where [?fg :feature.associated-with-gene/gene ?gene]
                            [?f :feature/associated-with-gene ?fg]]
                   db (:db/id gene))
                (map
                  (fn [fid]
                    (let [feature (entity db fid)
                          method (-> (:locatable/method feature)
                                     (:method/id))
                          interaction  (->> (:interaction.feature-interactor/_feature feature)
                                            (map #(pack-obj "interaction" (:interaction/_feature-interactor %)))
                                            (seq))
                          expr-pattern (->>
                                         (q '[:find [?e ...]
                                              :in $ ?f
                                              :where [?ef :expr-pattern.associated-feature/feature ?f]
                                              [?e :expr-pattern/associated-feature ?ef]
                                              [?e :expr-pattern/anatomy-term _]]
                                            db fid)
                                         (map
                                           (fn [eid]
                                             (let [expr (entity db eid)]
                                               {:text (map #(pack-obj "anatomy-term" (:expr-pattern.anatomy-term/anatomy-term %))
                                                           (:expr-pattern/anatomy-term expr))
                                                :evidence {:by (pack-obj "expr-pattern" expr)}})))
                                         (seq))
                          bounded-by (->> (:feature/bound-by-product-of feature)
                                          (map #(pack-obj "gene" (:feature.bound-by-product-of/gene %)))
                                          (seq))
                          tf (if (contains? feature :feature/associated-with-transcription-factor)
                               (pack-obj "transcription-factor" (:feature.associated-with-transcription-factor/transcription-factor
                                                                  (first (:feature/associated-with-transcription-factor feature))))
                               nil)]
                      {:name (pack-obj "feature" feature)
                       :description (first (:feature/description feature))
                       :method (if (nil? method) nil (str/replace method #"_" " "))
                       :interaction (if (nil? interaction) [] interaction)
                       :expr_pattern (if (nil? expr-pattern) [] expr-pattern)
                       :bounded_by (if (nil? bounded-by) [] bounded-by)
                       :tf tf })))
                (seq))]
    {:data (if (nil? data) nil data)
     :description
     "Features associated with this Gene"}))

(defn- get-segments [gene]
  (let [g-species (helpers.species/parse-species-name (:species/id (:gene/species gene)))
        sequence-database (datomic-rest-api.db.sequence/get-default-sequence-database g-species)
        features (datomic-rest-api.helpers.sequence/sequence-features sequence-database (:gene/id gene))]
    features))

(defn- longest-segment [segments]
  (first
      (sort-by #(- (:start %) (:end %)) segments)))

(defn- get-segment [gene]
  (let [segments (get-segments gene)
        segment (longest-segment segments)]
    segment))

(defn- segment-to-position [gene segment gbrowse]
  (let [[start, stop] (->> segment  ((juxt :start :end))  (sort-by +))
        padded-start (- start 2000)
        padded-stop (+ stop 2000)
        calc-browser-pos (fn  [x-op x y mult-offset]
                            (if gbrowse
                              (->> (reduce - (sort-by - [x y]))
                                   (double)
                                   (* mult-offset)
                                   (int)
                                   (x-op x))
                              y))
        browser-start (calc-browser-pos - padded-start padded-stop 0.2)
        browser-stop (calc-browser-pos + padded-stop padded-start 0.5)
        id (str (:seqname segment) ":" browser-start ".." browser-stop)]
    {:class "genomic_location" ;; To populate this correctly we will need sequence data
     :id id
     :label id
     :pos_string id
     :taxonomy (if-let [class (:gene/species gene)]
                 (if-let [[_ genus species] (re-matches #"^(.*)\s(.*)$" (:species/id class))]
                   (str/lower-case (str/join [(first genus) "_" species]))))
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
              "BINDING_REGIONS"]}))

(defn feature-image [gene]
  {:data (let [segment (get-segment gene)
               position (if ((comp not empty?) segment)
                 (segment-to-position gene segment true))]
           (if (empty? position) nil position))
   :description "The genomic location of the sequence to be displayed by GBrowse"})

;;
;; Genetics widget
;;

(defn reference-allele [gene]
  {:data (let [data
   (->> (:gene/reference-allele gene)
        (map :gene.reference-allele/variation)
        (map (partial pack-obj "variation")))]
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
           (pack-obj "strain" strain)
           :genotype (:strain/genotype strain)
           :transgenes (pack-obj "transgene" (first (:transgene/_strain strain)))))
        strains)))

(defn strains [gene]
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
            (nth (re-find #"\((\d+)\)" (:molecular-change.nonsense/text n)) 1)))

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
     (pack-obj "variation" var)

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
                 (pack-obj "cds" (:variation.predicted-cds/cds cc))))]
       (if (empty? isoform) nil isoform))

     :phen_count
     (count (:variation/phenotype var))

     :strain
     (map #(pack-obj "strain" (:variation.strain/strain %)) (:variation/strain var))

     :sources
     (if-let [sources (if (empty? (:variation/reference var))
                        (map #(let [packed (pack-obj %)]
                                (into packed {:label
                                              (str/replace (:label packed) #"_" " ")})) (:variation/analysis var))
                        (map #(pack-obj (:variation.reference/paper %)) (:variation/reference var)))]
       (if (empty? sources) nil sources)))))

(defn alleles [gene]
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

(defn alleles-other [gene]
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

(defn polymorphisms [gene]
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

(defn alleles-count [gene]
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

(defn rearrangements-positive [gene]
  (let [db (d/entity-db gene)]
    (->> (q '[:find [?ra ...]
               :in $ ?gene
               :where [?rag :rearrangement.gene-inside/gene ?gene]
                      [?ra :rearrangement/gene-inside ?rag]]
            db (:db/id gene))
         (map #(pack-obj (entity db %))))))

(defn rearrangements-negative [gene]
   (let [db (d/entity-db gene)]
    (->> (q '[:find [?ra ...]
               :in $ ?gene
               :where [?rag :rearrangement.gene-outside/gene ?gene]
                      [?ra :rearrangement/gene-outside ?rag]]
            db (:db/id gene))
         (map #(pack-obj (entity db %))))))

(defn rearrangements [gene]
  {:data (let [data {:positive (if-let [rearrangements (rearrangements-positive gene)]
                                 (if (empty? rearrangements) nil rearrangements))
                     :negative (if-let [rearrangements (rearrangements-negative gene)]
                                 (if (empty? rearrangements) nil rearrangements))}]
           (if (or (:positive data) (:negative data)) data nil))
   :description "rearrangements involving this gene"})




;;
;; external_links widget
;;

(defn xrefs [gene]
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
