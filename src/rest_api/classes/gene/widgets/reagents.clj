(ns rest-api.classes.gene.widgets.reagents
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [pseudoace.binning]
    [pseudoace.utils :as pace-utils]
    [pseudoace.locatables :as locatables]))

(defn- construct-labs [construct]
  (seq
    (map
      #(pack-obj "laboratory" (:construct.laboratory/laboratory %))
      (:construct/laboratory construct))))

(defn- transgene-labs [tg]
  (seq (map #(pack-obj "laboratory" (:transgene.laboratory/laboratory %))
            (:transgene/laboratory tg))))


(defn- transgene-record [construct]
  (let [base {:construct (pack-obj "construct" construct)
              :used_in   (pack-obj "transgene" (first (:construct/transgene-construct construct)))
              :use_summary (:construct/summary construct)}]
    (pace-utils/cond-let [use]
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

(defn transgenes  [gene]
  (let  [db  (d/entity-db gene)]
    {:data
     (->>  (d/q '[:find  [?cons ...]
                :in $ ?gene
                :where  [?cbg :construct.driven-by-gene/gene ?gene]
                [?cons :construct/driven-by-gene ?cbg]]
              db  (:db/id gene))
          (map  (partial d/entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes expressed by this gene"}))

(defn transgene-products [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?cons ...]
                :in $ ?gene
                :where [?cg :construct.gene/gene ?gene]
                [?cons :construct/gene ?cg]]
              db (:db/id gene))
          (map (partial d/entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes that express this gene"}))

(def ^:private probe-types
  {:oligo-set.type/affymetrix-microarray-probe "Affymetrix"
   :oligo-set.type/washu-gsc-microarray-probe  "GSC"
   :oligo-set.type/agilent-microarray-probe    "Agilent"})

(defn microarray-probes [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?oligo ...]
               :in $ ?gene [?type ...]
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?ocds :oligo-set.overlaps-cds/cds ?cds]
                      [?oligo :oligo-set/overlaps-cds ?ocds]
                      [?oligo :oligo-set/type ?type]]
             db (:db/id gene) (keys probe-types))
          (map (fn [oid]
           (let [oligo (d/entity db oid)]
       (assoc (pack-obj "oligo-set" oligo)
              :class "pcr_oligo"
              :label (format
                 "%s [%s]"
                 (:oligo-set/id oligo)
                 (some probe-types (:oligo-set/type oligo)))))))
          (seq))
     :description "microarray probes"}))

(defn matching-cdnas [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?cdna ...]
               :in $ ?gene
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?cds :cds/matching-cdna ?mcdna]
                      [?mcdna :cds.matching-cdna/sequence ?cdna]]
             db (:db/id gene))
          (map #(pack-obj "sequence" (d/entity db %)))
          (seq))
     :description "cDNAs matching this gene"}))

(defn antibodies [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?ab ...]
               :in $ ?gene
               :where [?gab :antibody.gene/gene ?gene]
                      [?ab :antibody/gene ?gab]]
             db (:db/id gene))
          (map
            (fn [abid]
              (let [ab (d/entity db abid)]
                {:antibody (pack-obj "antibody" ab)
                 :summary (:antibody.summary/text (:antibody/summary ab))
                 :laboratory (map (partial pack-obj "laboratory") (:antibody/location ab))})))
          (seq))
     :description "antibodies generated against protein products or gene fusions"}))

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

(defn orfeome-primers [gene]
  (let [db (d/entity-db gene)
        [parent start end] (locatables/root-segment gene)]
    {:data
    (if parent
       (->> (d/q '[:find [?p ...]
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
                (let [pp (d/entity db ppid)]
                  {:id    (:pcr-product/id pp)
                   :class "pcr_oligo"
                   :label (:pcr-product/id pp)})))
            (seq)))
     :description "ORFeome Project primers and sequences"}))

(defn primer-pairs [gene]
  (let [db                 (d/entity-db gene)
        [parent start end] (locatables/root-segment gene)]
    {:data
     (if parent
       (->> (d/q '[:find [?p ...]
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
		      (let [pp (d/entity db ppid)]
			{:id    (:pcr-product/id pp)
			 :class "pcr_oligo"
			 :label (:pcr-product/id pp)})))
		  (seq)))
     :description "Primer pairs"}))

(defn sage-tags [gene]
  {:data (seq
            (map
              #(pack-obj "sage-tag" (:sage-tag/_gene %))
              (:sage-tag.gene/_gene gene)))
   :description "SAGE tags identified"})

(def widget
  {:name               generic/name-field
   :transgenes         transgenes
   :transgene_products transgene-products
   :microarray_probes  microarray-probes
   :matching_cdnas     matching-cdnas
   :antibodies         antibodies
   :orfeome_primers    orfeome-primers
   :primer_pairs       primer-pairs
   :sage_tags          sage-tags})
