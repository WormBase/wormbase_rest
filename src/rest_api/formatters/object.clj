(ns rest-api.formatters.object
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.date :as dates])
  (:import
   (java.text SimpleDateFormat)))

;;
;; General purpose functions for working with Wormbase-ish entity-maps.
;;

(declare pack-obj)

(defn obj-get
  "Retrieve an entity-map for object `id` of class `class`
  as of database-value `db`."
  [class db id]
  (d/entity db [(keyword class "id") id]))

(defn obj-tax
  "If entity-map `obj` has a species attribute,
  return the short name of the species, otherwise \"all\"."
  [class obj]
  (let [species-ident (keyword class "species")]
    (if-let [species-many (species-ident obj)]
      (let [species (if (contains? species-many :species/id)
                      species-many
                      (first species-many))]
        (if-let [species-obj (let [species-kw (keyword (str class ".species") "species")]
                               (if (contains? species species-kw)
                                 (species-kw species)
                                 species))]
          (if-let [species-id (:species/id species-obj)]
            (if-let [[_ g species-str]
                     (re-matches #"(.).*[ _](.+)" species-id)]
              (.toLowerCase (str g "_" species-str))
              "unknown")
            "unknown")
          "unknown"))
      "all")))

(defmulti obj-label
  "Build a human-readable label for `obj`"
  (fn [class obj] class))

(defmethod obj-label "gene" [_ obj]
  (or (:gene/public-name obj)
      (:gene/id obj)))

(defmethod obj-label "wbprocess" [_ obj]
  (or (:wbprocess/public-name obj)
      (:wbprocess/id obj)))

(defmethod obj-label "laboratory" [_ obj]
  (or (first (:laboratory/mail obj))
      (:laboratory/id obj)))

(defmethod obj-label "protein" [_ prot]
  (or (first (:protein/gene-name prot))
      (:protein/id prot)))

(defmethod obj-label "phenotype" [_ obj]
  (or (->> (:phenotype/primary-name obj)
           (:phenotype.primary-name/text))
      (:phenotype/id obj)))

(defmethod obj-label "molecule" [_ obj]
   (or (first (:molecule/public-name obj))
      (:molecule/id obj)))

(defmethod obj-label "variation" [_ obj]
  (or (:variation/public-name obj)
      (:variation/id obj)))

;; Helpers for paper labels.
(defn- author-lastname [author-holder]
  (or
   (->> (:affiliation/person author-holder)
        (first)
        (:person/last-name))
   (-> (:paper.author/author author-holder)
       (:author/id)
       (.trim)
       (str/split #"\s+")
       (first))))

(defn author-list [paper]
  (let [authors (sort-by :ordered/index (:paper/author paper))]
    (cond
     (= (count authors) 1)
     (author-lastname (first authors))

     (< (count authors) 6)
     (let [last-names (map author-lastname authors)]
       (str (str/join ", " (butlast last-names))
            " & "
            (last last-names)))

     :default
     (str (author-lastname (first authors)) " et al."))))

(defmethod obj-label "paper" [_ paper]
  (if (contains? paper :paper/author)
    (if-let [year (when (seq (:paper/publication-date paper))
                    (first
                      (str/split
                        (:paper/publication-date paper)
                        #"-")))]
      (str (author-list paper) ", " year)
      (author-list paper))
    (:paper/id paper)))

(defmethod obj-label "feature" [_ feature]
  (or (:feature/public-name feature)
      (if (nil? (:feature/other-name feature))
        (:feature/id feature)
        (first (:feature/other-name feature)))))

(defmethod obj-label "anatomy-term" [_ term]
  (or (:anatomy-term.term/text (:anatomy-term/term term))
      (:anatomy-term/id term)))

(defmethod obj-label "do-term" [_ term]
  (:do-term/name term))

(defmethod obj-label "person" [_ person]
 (or (:person/standard-name person)
     (or (:author/id (first (:person/possibly-publishes-as person)))
         (:person/id person))))

(defmethod obj-label "construct" [_ cons]
  (or (first (:construct/public-name cons))
      (or (first (:construct/other-name cons))
          (:construct/id cons))))

(defmethod obj-label "transgene" [_ tg]
  (or (:transgene/public-name tg)
      (:transgene/id tg)))

(defmethod obj-label "go-term" [_ go]
  (first (:go-term/name go))) ;; Not clear why multiples allowed here!

(defmethod obj-label "life-stage" [_ ls]
  (:life-stage/public-name ls))

(defmethod obj-label "molecule-affected" [_ ls]
  (:moluecule/public-name ls))

(defmethod obj-label "protein" [_ prot]
  (or (first (:protein/gene-name prot))
      (:protein/id prot)))

(defmethod obj-label "pcr_oligo" [_ pcr]
  (or (:pcr-product/id pcr)
      (or (:oligo/id pcr)
          (:oligo-set/id pcr))))


(def q-interactor
  '[:find [?interactor ...]
    :in $ ?int
    :where
    (or-join
     [?int ?interactor]
     (and
      [?int
       :interaction/interactor-overlapping-gene ?gi]
      [?gi
       :interaction.interactor-overlapping-gene/gene
       ?interactor])
     (and
      [?int :interaction/molecule-interactor ?mi]
      [?mi
       :interaction.molecule-interactor/molecule
       ?interactor])
     (and
      [?int :interaction/other-interactor ?orint]
      [?orint
       :interaction.other-interactor/text
       ?interactor])
     (and
      [?int :interaction/rearrangement ?ri]
      [?ri
       :interaction.rearrangement/rearrangement
       ?interactor])
     (and
      [?int :interaction/feature-interactor ?fi]
      [?fi
       :interaction.feature-interactor/feature
       ?interactor]))])

(defmethod obj-label "interaction" [_ int]
  ;; Note that only certain types of interactor are considered when
  ;; computing the display name.
  (let [db (d/entity-db int)]
    (if-let [il (seq (d/q q-interactor db (:db/id int)))]
      (->> il
           (map
            (fn [interactor]
              (cond
                (string? interactor)
                interactor

                :default
                (:label (pack-obj (d/entity db interactor))))))
           (sort)
           (str/join " : "))
      (:interaction/id int))))

(defmethod obj-label "motif" [_ motif]
  (or (first (:motif/title motif))
      (:motif/id motif)))

(defmethod obj-label :default [class obj]
  ((keyword class "id") obj))

;; This should be obsolete now, use pack-obj instead.

(defmulti obj-name (fn [class db id] class))

(defmethod obj-name "gene" [class db id]
  (let [obj (obj-get class db id)]
    {:data
     {:id (:gene/id obj)
       :label (or (:gene/public-name obj)
                (:gene/id obj))
       :class "gene"
       :taxonomy (obj-tax class obj)}
     :description (str "The name and WormBase internal ID of "
                       (:gene/id obj))}))

(defn obj-class
  "Attempt to determine the class of a WormBase-ish entity-map."
  [obj]
  (cond
   (:gene/id obj)
   "gene"

   (:clone/id obj)
   "clone"

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

   (:molecule/id obj)
   "molecule"

   (:life-stage/id obj)
   "life-stage"

   (:go-term/id obj)
   "go-term"

   (:pcr-product/id obj)
   "pcr_oligo"

   (:oligo-set/id obj)
   "pcr_oligo"

   (:oligo/id obj)
   "pcr_oligo"

   :default
   (if-let [k (first (filter #(= (name %) "id") (keys obj)))]
     (namespace k))))

(defn pack-obj
  "Retrieve a 'packed' (web-API) representation of entity-map `obj`."
  ([obj]
   (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
   (if obj
     {:id (or ((keyword class "id") obj)
              (or (:oligo-set/id obj)
                  (or (:pcr-product/id obj)
                      (:oligo/id obj))))
      :label (or label (obj-label class obj))
      :class (if class
               (if (= class "author")
                 "person"
                 (str/replace class "-" "_")))
      :taxonomy (obj-tax class obj)})))

(defn get-evidence [holder]
  ;; Some of these need further checking to ensure that handling of
  ;; multiple values matches Perl.
  (pace-utils/vmap-if
   :Inferred_automatically
   (seq (:evidence/inferred-automatically holder))

   :Curator
   (seq (for [person (:evidence/curator-confirmed holder)]
          (pack-obj "person" person)))

   :Person_evidence
   (seq (for [person (:evidence/person-evidence holder)]
          (pack-obj "person" person)))

   :Paper_evidence
   (seq (for [paper (:evidence/paper-evidence holder)]
          (pack-obj "paper" paper)))

   :Date_last_updated
   (when-let [last-updated (:evidence/date-last-updated holder)]
     (let [ds (-> last-updated dates/format-date4 str)]
       [{:id ds
         :label ds
         :class "text"}]))

   :Published_as
   (seq (for [pa (:evidence/published-as holder)]
          {:evidence pa
           :label pa}))

   :Author_evidence
   (seq (for [ah (:evidence/author-evidence holder)
              :let [author (:evidence.author-evidence/author ah)]]
          {:evidence (pack-obj "author" author)}))

   :Accession_evidence
   (when-let [accs (:evidence/accession-evidence holder)]
     (for [{acc :evidence.accession-evidence/accession
            db  :evidence.accession-evidence/database} accs]
       {:id acc
        :label (format "%s:%s" (:database/id db) acc)
        :class (:database/id db)}))


   :Protein_id_evidence
   (seq (for [p (:evidence/protein-id-evidence holder)]
          {:id p
           :label p
           :class "Entrezp"}))

   :GO_term_evidence
   (seq (map (partial pack-obj "go-term")
             (:evidence/go-term-evidence holder)))

   :Expr_pattern_evidence
   (when-let [epe (:evidence/expr-pattern-evidence holder)]
     (map (partial pack-obj "expr-pattern") epe))

   :Microarray_results_evidence
   (when-let [e (:evidence/microarray-results-evidence holder)]
     (map (partial pack-obj "microarray-results") e))

   :RNAi_evidence   ;; could be multiples?
   (when-let [rnai (first (:evidence/rnai-evidence holder))]
     {:id (:rnai/id rnai)
      :label (if-let [hn (:rnai/history-name rnai)]
               (format "%s (%s)" (:rnai/id rnai) hn)
               (:rnai/id rnai))})

   :Feature_evidence
   (when-let [features (:evidence/feature-evidence holder)]
     (map (partial pack-obj "feature") features))

   :Laboratory_evidence
   (when-let [labs (:evidence/laboratory-evidence holder)]
     (map (partial pack-obj "laboratory") labs))

   :From_analysis
   (when-let [anas (:evidence/from-analysis holder)]
     (map (partial pack-obj "analysis") anas))

   :Variation_evidence
   (when-let [vars (:evidence/variation-evidence holder)]
     (map (partial pack-obj "variation") vars))

   :Mass_spec_evidence
   (when-let [msps (:evidence/mass-spec-evidence holder)]
     (map (partial pack-obj "mass-spec-peptide") msps))

   :Sequence_evidence
   (when-let [seqs (:evidence/sequence-evidence holder)]
     (map (partial pack-obj "sequence") seqs))

   :genotype
   (when-let [c (:anatomy-function.assay/condition holder)]
     (str/join "<br /> " (:condition/genotype c)))

   :Autonomous
   (when-let [ss (:anatomy-function-info/autonomous holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Autonomous"
        :id "Autonomous"})

   :Necessary
   (when-let [ss (:anatomy-function-info/necessary holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Necessary"
        :id "Necessary"})

   :Unnecessary
   (when-let [ss (:anatomy-function-info/unnecessary holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Unnecessary"
        :id "Unnecessary"})

   :Nonautonomous
   (when-let [ss (:anatomy-function-info/nonautonomous holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Nonautonomous"
        :id "Nonautonomous"})

   :Sufficient
   (when-let [ss (:anatomy-function-info/sufficient holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Sufficient"
        :id "Sufficient"})

   :Insufficient
   (when-let [iss (:anatomy-function-info/insufficient holder)]
       {:taxonomy "all"
        :class "txt"
        :label "Insuffiecient"
        :id "Insufficient"})))

(defn pack-text
  "Normalize text to behave like a pack object."
  [text]
  {:id text
   :label (str/replace text #"_" " ")
   :class text
   :taxonomy nil})

(defn humanize-ident
  "Reconstruct a more human-readable representation of a
  Datomic enum key."
  [ident]
  (if ident
    (-> (name ident)
        (str/split #":")
        (last)
        (str/replace #"_" " ")
        (str/replace #"-" " ")
        (str/capitalize))))

(defn name-field [object]
  (let [data (pack-obj object)]
    {:data (not-empty data)
     :description (format "The name and WormBase internal ID of %s"
                          (:id data))}))

(defn tag-obj [label]
  {:taxonomy "all"
   :class "tag"
   :label label
   :id label})
