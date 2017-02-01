(ns rest-api.classes.gene.widgets.phenotypes
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.gene.generic :as generic]
   [rest-api.formatters.object :as obj]))

(defn parse-int
  "Format a string as a number."
  [s]
  (Integer. (re-find #"\d+" s)))

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

(defn- create-tag [label]
  {:taxonomy "all"
   :class "tag"
   :label label
   :id  label})

(defn- evidence-paper [paper]
  {:class "paper"
   :id (:paper/id paper)
   :taxonomy "all"
   :label (str (obj/author-list paper)
               ", "
               (if (= nil (:paper/publication-date paper))
                 ""
                 (first (str/split (:paper/publication-date paper)
                                   #"-"))))})

(defn var-evidence [holder variation pheno]
  (pace-utils/vmap
    :Person_evidence
    (seq
      (for [person (:phenotype-info/person-evidence holder)]
        {:class "person"
         :id (:person/id person)
         :label (:person/standard-name person)
         :taxonomy "all"}))

    :Curator
    (seq (for [person (:phenotype-info/curator-confirmed holder)]
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
    (if (contains? holder :phenotype-info/recessive)
      "")

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
     (remove
      nil?
      (flatten
       (conj
        (if (contains? holder :phenotype-info/low)
          (for [low-holder (:phenotype-info/low holder)
                :let [text (:phenotype-info.low/text low-holder)]]
            (if (not= text "")
              text)))
        (if
            (contains? holder :phenotype-info/high)
          (for [high-holder (:phenotype-info/high holder)
                :let [text (:phenotype-info.high/text high-holder)]]
            (if (not= text "")
              text)))
        (if
            (contains? holder :phenotype-info/complete)
          (for [complete-holder (:phenotype-info/complete holder)
                :let [text (:phenotype-info.complete/text
                            complete-holder)]]
            (if (not= text "")
              text)))))))

    :Penetrance-range
    (if (pace-utils/not-nil? (:phenotype-info/range holder))
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
        (obj/humanize-ident
          (:phenotype-info.maternal/value
            (:phenotype-info/maternal holder)))))

    :Paternal
    (if
      (contains? holder :phenotype-info/paternal)
      (create-tag
        (obj/humanize-ident
          (:phenotype-info.paternal/value
            (:phenotype-info/paternal holder)))))

    :Haplo_insufficient
    (if
      (contains? holder :phenotype-info/haplo-insufficient)
      (create-tag
        (obj/humanize-ident
          (:phenotype-info.paternal/value
            (:phenotype-info/haplo-insufficient holder)))))

    :Variation_effect
    (if (contains? holder :phenotype-info/variation-effect)
      (first
       ;; we should actually display all of them but catalyst template
       ;; not displaying nested array
       (for [ve (:phenotype-info/variation-effect holder)]
         (remove
          nil?
          [(create-tag
            (obj/humanize-ident
             (:phenotype-info.variation-effect/value ve)))
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
        (obj/pack-obj (:phenotype-info.molecule/molecule m))))

    :Affected_by_pathogen
    (if
      (contains? holder :phenotype-info/pathogen)
      (for [m (:phenotype-info/pathogen holder)]
        (obj/pack-obj (:phenotype-info.molecule/species m))))

    :Ease_of_scoring
    (if
      (contains? holder :phenotype-info/ease-of-scoring)
      (create-tag
        (obj/humanize-ident
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
      (obj/humanize-ident
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
    nil))

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
                (let [make-pi-kw (partial keyword
                                       (str "phenotype-info." eq-key))
                      pato-term-kw (make-pi-kw "pato-term")
                      label-kw (make-pi-kw label)
                      eq-kw (make-pi-kw eq-key)
                      pato-names (:pato-term/name (-> eq-term
                                                      (pato-term-kw)))
                      pato-name (first pato-names)
                      id ((keyword eq-key "id") (-> eq-term (eq-kw)))
                      entity-term (obj/pack-obj label
                                                (-> eq-term (label-kw)))
                      pato-term (if (nil? pato-name)
                                  "abnormal"
                                  pato-name)]
                  (if (pace-utils/not-nil? id)
                    (create-pato-term id
                                      label
                                      entity-term
                                      (str/capitalize
                                       (str/replace eq-key #"-" "_"))
                                      pato-term)))))
        var-combo (into {} (for [x sot]
                             (apply merge x)))]
    {(str/join "_" (sort (keys var-combo))) (vals var-combo)}))

(defn- get-pato-combinations [db pid rnai-phenos var-phenos not?]
  (if-let [vp (distinct (concat (rnai-phenos pid) (var-phenos pid)))]
    (let [patos (for [v vp
                      :let [holder (d/entity db v)]]
                     (get-pato-from-holder holder))]
      (apply merge patos))))

(def ^{:private true} pt-info-kw (partial keyword "phenotype-info"))

(defn get-transgene-evidence
  [holders phenotypeid transgene]
  (for [h holders
        :let [pid (:phenotype/id (:transgene.phenotype/phenotype h))]]
    (if (= pid phenotypeid)
      (let [remark (map :phenotype-info.remark/text
                        (:phenotype-info/remark h))
            transgeneobj (obj/pack-obj "transgene" transgene)
            causedbygenes (:phenotype-info/caused-by-gene h)
            paperevidences (:phenotype-info/paper-evidence h)
            curators (:phenotype-info/curator-confirmed h)]
        {:text
         [transgeneobj
          (str "<em>"
               (:transgene.summary/text (:transgene/summary transgene))
               "</em>")
          remark]

         :evidence
         {:Phenotype_assay
          (remove
            nil?
            (flatten
              (for [term ["treatment" "temperature" "genotype"]]
                (let [label (str/capitalize term)]
                  (if (contains? h (pt-info-kw term))
                    {:taxonomy "all"
                     :class "tag"
                     :label label
                     :id label})))))

          :Curator
          (for [curator curators]
            (obj/pack-obj "person" curator))

          :EQ_annotations
          (remove
            nil?
            (flatten
              (for [term ["anatomy-term"
                          "life-stage"
                          "go-term"
                          "molecule-affected"]]
                (let [label (str/capitalize term)]
                  (if (contains? h (pt-info-kw term))
                    {:taxonomy "all"
                     :class "tag"
                     :label label
                     :id label})))))

          :Caused_by_gene
          (for [cbgs causedbygenes
                :let [cbg (:phenotype-info.caused-by-gene/gene cbgs)]]
            (obj/pack-obj "gene" cbg))

          :Transgene transgeneobj

          :Paper_evidence
          (for [pe paperevidences]
            (obj/pack-obj "paper" pe))

          :remark remark}}))))

(defn- transgene-evidence-for-phenotype
  "Return the transgene evidence given a set of transgene ids
  `tg-ids` and a phenotype id `pt-id`."
  [db tg-ids pid]
  (let [pheno (d/entity db pid)
        pt-id (:phenotype/id pheno)]
    {pt-id {:object (obj/pack-obj "phenotype" pheno)
            :evidence
            (flatten
             (for [tg-id tg-ids]
               (let [tg (d/entity db tg-id)
                     holders (:transgene/phenotype tg)
                     evidence (get-transgene-evidence holders
                                                      pt-id
                                                      tg-id)]
                 evidence)))}}))

(defn drives-overexpression [gene]
  (let [db (d/entity-db gene)
        tg-ids (d/q '[:find [?tg ...]
                      :in $ ?gene
                      :where
                      [?cbg :construct.driven-by-gene/gene ?gene]
                      [?cons :construct/driven-by-gene ?cbg]
                      [?cons :construct/transgene-construct ?tg]]
                    db (:db/id gene))
        tg-evidence (partial transgene-evidence-for-phenotype db tg-ids)
        phenotype
        (->> tg-ids
             (map (fn [tg]
                    (->> (d/q '[:find [?pheno ...]
                                :in $ ?tg
                                :where
                                [?tg :transgene/phenotype ?ph]
                                [?ph
                                 :transgene.phenotype/phenotype
                                 ?pheno]]
                              db tg)
                         (map tg-evidence)
                         (into {}))))
             (into {}))]
    {:data (if ((comp not empty?) phenotype)
             {:Phenotype phenotype})
     :description (str "phenotypes due to overexpression under "
                       "the promoter of this gene")}))

(defn- phenotype-table-entity
  [db pheno pato-key entity pid var-phenos rnai-phenos not-observed?]
  {:entity entity
   :phenotype {:class "phenotype"
               :id (:phenotype/id pheno)
               :label (:phenotype.primary-name/text
                       (:phenotype/primary-name pheno))
               :taxonomy "all"}
   :evidence
   (pace-utils/vmap
     "Allele:"
     (if-let [vp (seq (var-phenos pid))]
       (for [v vp
             :let [holder (d/entity db v)
                   var ((if not-observed?
                          :variation/_phenotype-not-observed
                          :variation/_phenotype)
                        holder)
                   pato-keys (keys (get-pato-from-holder holder))
                   var-pato-key (first pato-keys)]]
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
            :evidence (var-evidence holder var pheno)})))
     "RNAi:"
     (if-let [rp (seq (rnai-phenos pid))]
       (for [r rp]
         (let [holder (d/entity db r)
               pato-keys (keys (get-pato-from-holder holder))
               rnai ((if not-observed?
                       :rnai/_phenotype-not-observed
                       :rnai/_phenotype) holder)
               rnai-pato-key (first pato-keys)]
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
                (let [paper-ref (:rnai/reference rnai)]
                  (if-let [paper (:rnai.reference/paper paper-ref)]
                    (evidence-paper paper)))}
               (var-evidence holder rnai pheno))})))))})

(defn- phenotype-table [db gene not-observed?]
  (let [var-phenos (into {} (d/q (if not-observed?
                                   q-gene-var-not-pheno
                                   q-gene-var-pheno)
                                 db gene))
        rnai-phenos (into {} (d/q (if not-observed?
                                    q-gene-rnai-not-pheno
                                    q-gene-rnai-pheno)
                                  db gene))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->> (flatten
          (for [pid phenos
                :let [pheno (d/entity db pid)]]
            (let [pcs (get-pato-combinations db
                                             pid
                                             rnai-phenos
                                             var-phenos
                                             not-observed?)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      var-phenos
                                      rnai-phenos
                                      not-observed?)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        var-phenos
                                        rnai-phenos
                                        not-observed?))))))
         (into []))))

(defn phenotype-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) false)]
    {:data data
     :description "The Phenotype summary of the gene"}))

(defn phenotype-by-interaction [gene]
  (let [db (d/entity-db gene)
        gid (:db/id gene)
        table (d/q '[:find ?pheno (distinct ?int) ?int-type
                     :in $ ?gene
                     :where
                     [?ig
                      :interaction.interactor-overlapping-gene/gene
                      ?gene]
                     [?int :interaction/interactor-overlapping-gene ?ig]
                     [?int :interaction/interaction-phenotype ?pheno]
                     [?int :interaction/type ?type-id]
                     [?type-id :db/ident ?int-type]]
                   db gid)
        phenos (->> (map first table)
                    (set)
                    (map (fn [pid]
                           (let [pheno (d/entity db pid)]
                             [pid (obj/pack-obj "phenotype" pheno)])))
                    (into {}))
        inters (->> (mapcat second table)
                    (set)
                    (map
                     (fn [iid]
                       (let [int (d/entity db iid)]
                         [iid
                          {:interaction (obj/pack-obj "interaction" int)
                           :citations (map (partial obj/pack-obj "paper")
                                           (:interaction/paper int))}])))
                    (into {}))
        data (map (fn [[pheno pints int-type]]
                    {:interaction_type
                     (obj/humanize-ident int-type)
                     :phenotype
                     (phenos pheno)
                     :interactions
                     (map #(:interaction (inters %)) pints)
                     :citations
                     (map #(:citations (inters %)) pints)})
                  table)]
    {:data (if (empty? data) nil data)
     :description
     "phenotype based on interaction"}))

(defn phenotype-not-observed-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) true)]
    {:data data
     :description "The Phenotype not observed summary of the gene"}))

(def widget
  {:drives_overexpression    drives-overexpression
   :name                     generic/name-field
   :phenotype                phenotype-field
   :phenotype_by_interaction phenotype-by-interaction
   :phenotype_not_observed   phenotype-not-observed-field})
