(ns rest-api.classes.gene.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

;;; move?
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
;; end-move?

(defn also-refers-to [gene]
  (let [db (d/entity-db gene)]
    {:data
     (if-let [data
              (->> (d/q '[:find [?other-gene ...]
                          :in $ ?gene
                          :where
                          [?gene :gene/cgc-name ?cgc]
                          [?cgc :gene.cgc-name/text ?cgc-name]
                          [?other-name :gene.other-name/text ?cgc-name]
                          [?other-gene :gene/other-name ?other-name]]
                        db (:db/id gene))
                   (map #(pack-obj "gene" (d/entity db %)))
                   (seq))]
       data)
     :description
     "other genes that this locus name may refer to"}))

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
      :evidence (let [pdesc (first (:gene/provisional-description gene))]
                      (or (obj/get-evidence desc)
                          (obj/get-evidence pdesc)))}
     {:text nil :evidence nil})
   :description
   "A manually curated description of the gene's function"})

(defn curatorial-remarks [gene]
  (let [data (->> (:gene/remark gene)
                  (map (fn [rem]
                         {:text (:gene.remark/text rem)
                          :evidence (obj/get-evidence rem)}))
                  (seq))]
    {:data data
     :description "curatorial remarks for the Gene"}))

(defn gene-operon [gene]
  {:data
   (if-let [operon (->> (:operon.contains-gene/_gene gene)
                        (first)
                        (:operon/_contains-gene))]
     (pack-obj "operon" operon))
   :description "Operon the gene is contained in"})

(defn gene-version [gene]
  (let [data (str (:gene/version gene))]
    {:data data
     :description "the current WormBase version of the gene"}))

(defn- gene-classification-data-type
  [gene cds]
  (cond
    ;; This is pretty-much the reverse order of the
    ;; Perl code because we never over-write anything
    (d/q '[:find ?trans .
           :in $ ?gene
           :where
           [?gene :gene/version-change ?hist]
           [?hist
            :gene-history-action/transposon-in-origin
            ?trans]]
         (d/entity-db gene)
         (:db/id gene))
    "Transposon in origin"

    (:gene/corresponding-pseudogene gene)
    "pseudogene"

    cds
    "protein coding"

    :default (some
              #(transcript-type
                (:gene.corresponding-transcript/transcript %))
              (:gene/corresponding-transcript gene))))

(defn gene-class [gene]
  {:data (if-let [class (:gene/gene-class gene)]
           {:tag (pack-obj "gene-class" class)
            :description (str (first (:gene-class/description class)))})
   :description "The gene class for this gene"})

(defn gene-classification [gene]
  (let [data
        (let [db (d/entity-db gene)
              cds (:gene/corresponding-cds gene)
              data {:defined_by_mutation
                    (if (not (empty? (:variation.gene/_gene gene)))
                      1
                      0)
                    :type (gene-classification-data-type gene cds)
                    :associated_sequence (if (not (empty? cds))
                                           1
                                           0)
                    :confirmed
                    (if (d/q '[:find ?conf-gene .
                               :in $ ?conf-gene
                               :where
                               [?conf-gene :gene/corresponding-cds ?gc]
                               [?gc :gene.corresponding-cds/cds ?cds]
                               [?cds
                                :cds/prediction-status
                                :cds.prediction-status/confirmed]]
                             (d/entity-db gene)
                             (:db/id gene))
                      "Confirmed")}]
          (assoc
           data
           :prose_description
           (str/join
            " "
            (pace-utils/those
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
    {:data data
     :description "gene type and status"}))

(defn merged-into [gene]
  (let [db (d/entity-db gene)
        data
        (->> (d/q '[:find ?merge-partner .
                    :in $ ?gene
                    :where
                    [?gene :gene/version-change ?vc]
                    [?vc :gene-history-action/merged-into ?merge-partner]]
                  db (:db/id gene))
             (d/entity db)
             (pack-obj "gene"))]
    {:data data
     :description "the gene this one has merged into"}))

(defn gene-cluster [gene]
  (let [db (d/entity-db gene)
        gc (->> (d/q '[:find ?gc .
                       :in $ ?gene
                       :where
                       [?gc :gene-cluster/contains-gene ?gene]]
                     db (:db/id gene))
                (d/entity db))]

    {:data (if gc (pack-obj gc))
     :description "The gene cluster for this gene"}))

(defn gene-other-names [gene]
  {:data (let [other-names (:gene/other-name gene)
               data (map :gene.other-name/text other-names)]
           data)
   :description
   (format "other names that have been used to refer to %s"
           (:gene/id gene))})

(defn gene-status [gene]
  {:data (if-let [class (:gene/status gene)]
           (obj/humanize-ident (:gene.status/status class)))
   :description
   (format "current status of the Gene:%s %s"
           (:gene/id gene)
           "if not Live or Valid")})

(defn gene-taxonomy [gene]
  {:data
   (if-let [class (:gene/species gene)]
     (if-let [[_ genus species] (re-matches #"^(.*)\s(.*)$"
                                            (:species/id class))]
       {:genus genus :species species}
       {:genus (:gene/species gene)}))
   :description "the genus and species of the current object"})

(defn legacy-info [gene]
  {:data (let [data (map :gene.legacy-information/text
                         (:gene/legacy-information gene))]
           data)
   :description
   "legacy information from the CSHL Press C. elegans I/II books"})

(defn locus-name [gene]
  {:data
   (let [cgc (:gene/cgc-name gene)]
     (pack-obj "gene" gene :label (:gene.cgc-name/text cgc))
     "not assigned")
   :description
   "the locus name (also known as the CGC name) of the gene"})

(defn named-by [gene]
  {:data (let [data (->> (:gene/cgc-name gene)
                         (obj/get-evidence)
                         (mapcat val))]
           data)
   :description
   "the source where the approved name was first described"})

(defn parent-sequence [gene]
  {:data (let [data (pack-obj (:locatable/parent gene))]
           data)
   :description "parent sequence of this gene"})

(defn parent-clone [gene]
  (let [db (d/entity-db gene)
        data (->> (d/q '[:find [?clone ...]
                         :in $ ?gene
                         :where
                         [?cg :clone.positive-gene/gene ?gene]
                         [?clone :clone/positive-gene ?cg]]
                       db (:db/id gene))
                  (map (fn [cid]
                         (let [clone (d/entity db cid)]
                           (pack-obj "clone" clone))))
                  (seq))]
    {:data data
     :description "parent clone of this gene"}))

(defn cloned-by [gene]
  {:data (if-let [ev (obj/get-evidence (first (:gene/cloned-by gene)))]
           {:cloned_by (key (first ev))
            :tag       (key (first ev))
            :source    (first (val (first ev)))})
   :description
   "the person or laboratory who cloned this gene"})

(defn disease-relevance [gene]
  {:data (let [data
               (->> (:gene/disease-relevance gene)
                    (map (fn [rel]
                           {:text (:gene.disease-relevance/note rel)
                            :evidence (obj/get-evidence rel)}))
                    (seq))]
           data)
   :description
   "curated description of human disease relevance"})

(defn sequence-name [gene]
  {:data (or (:gene/sequence-name gene) "unknown")
   :description
   "the primary corresponding sequence name of the gene, if known"})

(defn transposon [gene]
  {:data (let [data (pack-obj
                     (first (:gene/corresponding-transposon gene)))]
           data)
   :description "Corresponding transposon for this gene"})

(defn- get-structured-description [gene typ]
  (let [gene-type     (keyword "gene" typ)
        txt-key (keyword (str "gene." typ) "text")]
    (->> (gene-type gene)
         (map (fn [data]
                {:text     (txt-key data)
                 :evidence (obj/get-evidence data)}))
         (seq))))

(defn- structured-prov-desc [gene]
  (let [cds (->> (:gene/concise-description gene)
                 (map :gene.concise-description/text)
                 (set))]
    (seq (for [p (:gene/provisional-description gene)
               :let [txt (:gene.provisional-description/text p)]
               :when (not (cds txt))]
           {:text txt
            :evidence (obj/get-evidence p)}))))

(defn structured-description [gene]
  (let [gene-struct-desc (partial get-structured-description gene)
        data (pace-utils/vmap
              :Provisional_description
              (structured-prov-desc gene)

              :Other_description
              (gene-struct-desc "other-description")

              :Sequence_features
              (gene-struct-desc "sequence-features")

              :Functional_pathway
              (gene-struct-desc "functional-pathway")

              :Functional_physical_interaction
              (gene-struct-desc "functional-physical-interaction")

              :Molecular_function
              (gene-struct-desc "molecular-function")

              :Biological_process
              (gene-struct-desc "biological-process")

              :Expression
              (gene-struct-desc "expression"))]
    {:data data
     :description "structured descriptions of gene function"}))

(def widget
  {:also_refers_to           also-refers-to
   :classification           gene-classification
   :clone                    parent-clone
   :cloned_by                cloned-by
   :concise_description      concise-description
   :gene_class               gene-class
   :gene_cluster             gene-cluster
   :human_disease_relevance  disease-relevance
   :legacy_information       legacy-info
   :locus_name               locus-name
   :merged_into              merged-into
   :name                     generic/name-field
   :named_by                 named-by
   :operon                   gene-operon
   :other_names              gene-other-names
   :parent_sequence          parent-sequence
   :remarks                  curatorial-remarks
   :sequence_name            sequence-name
   :status                   gene-status
   :structured_description   structured-description
   :taxonomy                 gene-taxonomy
   :transposon               transposon
   :version                  gene-version})
