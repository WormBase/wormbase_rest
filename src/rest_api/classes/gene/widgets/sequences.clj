(ns rest-api.classes.gene.widgets.sequences
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [rest-api.db.sequence :as seqdb]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj humanize-ident]]))

(defn- get-features-from-sequence [sequence]
  (if-let [species-name (->> sequence :transcript/species :species/id)]
    (let [g-species (generic-functions/xform-species-name species-name)
         sequence-database (seqdb/get-default-sequence-database g-species)
         db-spec ((keyword sequence-database) seqdb/sequence-dbs)
         method (:method/id (:locatable/method sequence))
         transcript-id (:transcript/id sequence)]
    (if sequence-database
      (seqdb/sequence-features-where-type db-spec transcript-id method)))))

(defn cds-id [s]
  ;Can be either a cds, transcript or pseudogene
  (if-let [cds (or
                 (and (:cds/id s) s)
                 (:transcript.corresponding-cds/cds (:transcript/corresponding-cds s))
                 (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds s)))]
    (:cds/id cds)))

(defn get-corresponding-cds [sequence]
  (or
    (and (:cds/id sequence) sequence)
    (:transcript.corresponding-cds/cds (:transcript/corresponding-cds sequence))
    (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds sequence))))

(defn cds-to-sequence [s]
  {(cds-id s) s})

(defn distinct-seqs [seqs]
  (->>
    (map cds-to-sequence seqs)
    (reverse)
    (into {})
    (vals)))

(defn gene-model-data [db coding? seqs gene]
  (->>
    (map (partial d/entity db) seqs)
    (sort-by (some-fn :cds/id :transcript/id :pseudogene/id))
    (distinct-seqs)
    (reduce
      (fn [{:keys [table remark-map]} sequence]
        (if-let [features (get-features-from-sequence sequence)]
          (let [cds (get-corresponding-cds sequence)
                protein (:cds.corresponding-protein/protein (:cds/corresponding-protein cds))
                seqs (or (seq (map :transcript/_corresponding-cds (:transcript.corresponding-cds/_cds cds)))
                         [sequence])
                status (str (obj/humanize-ident (:cds/prediction-status cds))
                            (if (:cds/matching-cdna cds)
                              " by cDNA(s)"))
                {:keys [remark-map footnotes]} (reduce (fn [{:keys [remark-map footnotes]} r]
                                                         (let [pr (if-let [ev (obj/get-evidence r)]
                                                                    {:text (:cds.remark/text r)
                                                                     :evidence ev}
                                                                    (:cds.remark/text r))]
                                                           (if-let [n (get remark-map pr)]
                                                             {:remark-map remark-map
                                                              :footnotes (pace-utils/conjv footnotes n)}
                                                             (let [n (inc (count remark-map))]
                                                               {:remark-map (assoc remark-map pr n)
                                                                :footnotes (pace-utils/conjv footnotes n)}))))
                                                       {:remark-map remark-map}
                                                       (:cds/remark cds))]
            {:remark-map remark-map
             :table (pace-utils/conjv
                      table
                      (pace-utils/vmap
                        :model (for [s seqs]
                                 (pack-obj s))

                        :protein
                        (pack-obj "protein" protein)

                        :cds
                        (if (nil? cds)
                          "(no CDS)"
                          (pace-utils/vmap
                            :text (pace-utils/vassoc (pack-obj "cds" cds) :footnotes footnotes)
                            :evidence (if (not (empty? status))
                                        {:status status})))

                        :length_spliced
                        (if coding?
                          (let [length-spliced (if-let [exons (seq (:cds/source-exons cds))]
                                                 (->>
                                                   (map
                                                     (fn [ex]
                                                       (- (:cds.source-exons/end ex)
                                                          (:cds.source-exons/start ex) -1))
                                                     exons)
                                                   (reduce +)))]
                            (if (= length-spliced 0)
                              "-"
                              length-spliced)))

                        :length_unspliced
                        (if coding?
                          (for [s seqs]
                            (let [length-spliced (if-let [exons (seq (:transcript/source-exons s))]
                                                   (->>
                                                     (map
                                                       (fn [ex]
                                                         (- (:transcript.source-exons/max ex)
                                                            (:transcript.source-exons/min ex) -1))
                                                       exons)
                                                     (reduce +)))]
                              (if (= length-spliced 0)
                                "-"
                                length-spliced))))

                        :length_protein
                        (:protein.peptide/length (:protein/peptide protein))

                        :type
                        (if seqs
                          (if-let [mid (:method/id
                                         (or
                                           (:locatable/method sequence)
                                           (:transcript/method sequence)
                                           (:pseudogene/method sequence)
                                           (:cds/method sequence)))]
                            (str/replace mid #"_" " ")))))})))
      {})
    ((fn [{:keys [table remark-map]}]
       (pace-utils/vmap
         :table table
         :remarks (if-not (empty? remark-map)
                    (into (sorted-map)
                          (for [[r n] remark-map]
                            [n r]))))))))

(defn gene-models [gene]
  (let [db (d/entity-db gene)
        coding? (:gene/corresponding-cds gene)
        seqs (d/q '[:find [?seq ...]
                    :in $ % ?gene
                    :where (or
                             (gene-transcript ?gene ?seq)
                             (gene-cdst-or-cds ?gene ?seq)
                             (gene-pseudogene ?gene ?seq))]
                  db
                  '[[(gene-transcript ?gene ?seq) [?gene :gene/corresponding-transcript ?ct]
                     [?ct :gene.corresponding-transcript/transcript ?seq]]
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
    {:data (gene-model-data db coding? seqs gene)
     :description "gene models for this gene"}))

(def widget
  {:name        generic/name-field
   :gene_models gene-models})
