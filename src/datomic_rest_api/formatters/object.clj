(ns datomic-rest-api.formatters.object
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [datomic-rest-api.formatters.date :as dates]
   [pseudoace.utils :as pace-utils])
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
    (if-let [species (species-ident obj)]
      (if-let [[_ g species] (re-matches #"(.).*[ _](.+)"
                                         (:species/id species))]
        (.toLowerCase (str g "_" species))
        "unknown")
      "all")))

(defmulti obj-label
  "Build a human-readable label for `obj`"
  (fn [class obj] class))

(defmethod obj-label "gene" [_ obj]
  (or (:gene/public-name obj)
      (:gene/id obj)))

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
  (let [authors (->> (:paper/author paper)
                     (sort-by :ordered/index))]
    (cond
     (= (count authors) 1)
     (author-lastname (first authors))

     (< (count authors) 6)
     (let [names (map author-lastname authors)]
       (str (str/join ", " (butlast names)) ", & " (last names)))

     :default
     (str (author-lastname (first authors)) " et al."))))

(defmethod obj-label "paper" [_ paper]
  (if (seq (:paper/publication-date paper))
    (str (author-list paper)
         ", "
         (first (str/split (:paper/publication-date paper)
                           #"-")))))

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
  (:person/standard-name person))

(defmethod obj-label "construct" [_ cons]
  (or (first (:construct/public-name cons))
      (:construct/id cons)))

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
      (->>
       (map
        (fn [interactor]
          (cond
            (string? interactor)
            interactor

            :default
            (:label (pack-obj (d/entity db interactor)))))
        il)
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
     {:id    (:gene/id obj)
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

   :default
   (if-let [k (first (filter #(= (name %) "id") (keys obj)))]
     (namespace k))))

(defn pack-obj
  "Retrieve a 'packed' (web-API) representation of entity-map `obj`."
  ([obj]
   (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
   (if obj
     {:id ((keyword class "id") obj)
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
   (if-let [last-updated (:evidence/date-last-updated holder)]
     (let [ds (-> last-updated dates/format-date str)]
       [{:id ds
         :label ds
         :class "text"}]))

   :Remark
   (seq (:evidence/remark holder))

   :Published_as
   (seq (for [pa (:evidence/published-as holder)]
          {:evidence pa
           :label pa}))

   :Author_evidence
   (seq (for [a (:evidence/author-evidence holder)
              :let [author (:evidence.author-evidence/author holder)]]
          {:evidence
           ;; Notes seem to be ignored here.
           (pack-obj "author" author)})) 

   :Accession_evidence
   (if-let [accs (:evidence/accession-evidence holder)]
     (for [{acc :evidence.accession-evidence/accession
            db  :evidence.accession-evidence/database} accs]
       {:id acc
        :label (format "%s:%s" (:database/id db) acc)
        :class (:database/id acc)}))


   :Protein_id_evidence
   (seq (for [p (:evidence/protein-id-evidence holder)]
          {:id p
           :label p
           :class "Entrezp"}))

   :GO_term_evidence
   (seq (map (partial pack-obj "go-term")
             (:evidence/go-term-evidence holder)))

   :Expr_pattern_evidence
   (if-let [epe (:evidence/expr-pattern-evidence holder)]
     (map (partial pack-obj "expr-pattern") epe))

   :Microarray_results_evidence
   (if-let [e (:evidence/microarray-results-evidence holder)]
     (map (partial pack-obj "microarray-results") e))

   :RNAi_evidence   ;; could be multiples?
   (if-let [rnai (first (:evidence/rnai-evidence holder))]
     {:id (:rnai/id rnai)
      :label (if-let [hn (:rnai/history-name rnai)]
               (format "%s (%s)" (:rnai/id rnai) hn)
               (:rnai/id rnai))})

   :Feature_evidence
   (if-let [features (:evidence/feature-evidence holder)]
     (map (partial pack-obj "feature") features))

   :Laboratory_evidence
   (if-let [labs (:evidence/laboratory-evidence holder)]
     (map (partial pack-obj "laboratory") labs))

   :From_analysis
   (if-let [anas (:evidence/from-analysis holder)]
     (map (partial pack-obj "analysis") anas))

   :Variation_evidence
   (if-let [vars (:evidence/variation-evidence holder)]
     (map (partial pack-obj "variation") vars))

   :Mass_spec_evidence
   (if-let [msps (:evidence/mass-spec-evidence holder)]
     (map (partial pack-obj "mass-spec-peptide") msps))

   :Sequence_evidence
   (if-let [seqs (:evidence/sequence-evidence holder)]
     (map (partial pack-obj "sequence" seqs)))))

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
        (str/replace #"-" " ")
        (str/capitalize))))
