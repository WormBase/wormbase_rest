(ns rest-api.classes.phenotype.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.paper.core :as paper-core]
   [rest-api.formatters.object :as obj]
   [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn- create-pato-term [id label entity-term entity-type pato-term]
  (let [pato-id (str/join "_" [id label pato-term])]
    {pato-id
     {:pato_evidence
      {:entity_term entity-term
       :entity_type label
       :pato_term pato-term}
      :key pato-id}}))

(defn get-pato-from-holder [holder]
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
                      entity-term (pack-obj label
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

(defn get-pato-combinations [db pid phenos]
  (if-let [tp (phenos pid)]
    (let [patos (for [t tp
                      :let [holder (d/entity db t)]]
                  (get-pato-from-holder holder))]
      (apply merge patos))))

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
        (paper-core/evidence paper)))

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
             (obj/tag-obj "Person_evidence"))
           (if
             (contains? sd :evidence/curator-confirmed)
             (obj/tag-obj "Curator_confirmed"))
           (if
             (contains? sd :evidence/paper-evidence)
             (obj/tag-obj "Paper_evidence"))])))

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
      (obj/tag-obj
        (obj/humanize-ident
          (:phenotype-info.maternal/value
            (:phenotype-info/maternal holder)))))

    :Paternal
    (if
      (contains? holder :phenotype-info/paternal)
      (obj/tag-obj
        (obj/humanize-ident
          (:phenotype-info.paternal/value
            (:phenotype-info/paternal holder)))))

    :Haplo_insufficient
    (if
      (contains? holder :phenotype-info/haplo-insufficient)
      (obj/tag-obj
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
          [(obj/tag-obj
            (obj/humanize-ident
             (:phenotype-info.variation-effect/value ve)))
           (if
               (contains? ve :evidence/person-evidence)
             (obj/tag-obj "Person_evidence"))
           (if
               (contains? ve :evidence/curator-confirmed)
             (obj/tag-obj "Curator_confirmed"))
           (if
               (contains? ve :evidence/paper-evidence)
             (obj/tag-obj "Paper_evidence"))]))))

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
      (obj/tag-obj
        (obj/humanize-ident
          (:phenotype-info.ease-of-scoring/value
            (:phenotype-info/ease-of-scoring holder)))))

    :keys (keys holder)

    :Caused_by_gene
    (if
      (contains? holder :phenotype-info/caused-by-gene)
      (let [cbgs (:phenotype-info/caused-by-gene holder)]
        (for [cbg cbgs]
          (pack-obj (:phenotype-info.caused-by-gene/gene cbg)))))

    :Phenotype_assay
    (if
      (contains? holder :phenotype-info/strain)
      (obj/tag-obj "Strain"))

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
            (obj/tag-obj "Heat-sensitive"))
          (if (contains? holder :phenotype-info/cold-sensitive)
            (obj/tag-obj "Cold-sensitive"))))

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
