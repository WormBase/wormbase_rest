(ns rest-api.classes.gene.widgets.sequences
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [datomic.api :as d]
   [rest-api.db.sequence :as seqdb]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.gene.generic :as generic]
   [rest-api.classes.gene.sequence :as seqfeat]
   [rest-api.formatters.object :as obj :refer [pack-obj humanize-ident]]))

(defn get-transcript-length [sequence]
  (let [species (:transcript/species sequence)
        method (:locatable/method sequence)]
    ; get the length from db
    "test"))

(defn- get-features-from-sequence [sequence]
  (let  [species-name  (->> sequence :transcript/species :species/id)
         g-species  (seqfeat/xform-species-name species-name)
         sequence-database  (seqdb/get-default-sequence-database g-species)
         method (:method/id (:locatable/method sequence))
         transcript-id (:transcript/id sequence)]
    (if sequence-database
      (seqfeat/sequence-features-where-type sequence-database transcript-id method))))

(defn cds-id [s]
  ;s can be either a cds, transcript or pseudogene
  (if-let [cds (or
                 (and (:cds/id s))
                 (:transcript.corresponding-cds/cds (:transcript/corresponding-cds s))
                 (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds s)))]
    (:cds/id cds)))

(defn cds-to-sequence [s]
  {(cds-id s) s})

(defn distinct-seqs [seqs]
  (flatten (vals (apply merge-with set/union (group-by key (filter (fn [k v] (nil? k)) map cds-to-sequence seqs )))))
;  (->>
;                (map cds-to-sequence seqs)
;                (filter (fn [k v] (nil? k)))
;                (group-by key)
;                (apply merge-with set/union)
 ;               (vals)
;                (flatten)
;      )]
  )

(defn gene-model-data [db coding? seqs]
  (->>
    (map (partial d/entity db) seqs)
    (sort-by (some-fn :cds/id :transcript/id :pseudogene/id))
    (distinct-seqs)
    (reduce
      (fn [{:keys [table remark-map]} sequence]
        (if-let [features (get-features-from-sequence sequence)]
          (let [cds (or
                      (and (:cds/id sequence) sequence)
                      (:transcript.corresponding-cds/cds (:transcript/corresponding-cds sequence))
                      (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds sequence)))
                protein (:cds.corresponding-protein/protein (:cds/corresponding-protein cds))
                seqs (or (seq (map :transcript.corresponding-cds/_cds (:transcript/_corresponding-cds cds)))
                         [sequence])
                status (str (obj/humanize-ident (:cds/prediction-status cds))
                            (if (:cds/matching-cdna cds)
                              " by cDNA(s)"))
                {:keys [remark-map footnotes]}
                (reduce (fn [{:keys [remark-map footnotes]} r]
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
                        :model
                        (map pack-obj seqs)

                        :method (:method/id (:locatable/method sequence))

                        :features features

                        :keys (keys sequence)
                        :protein
                        (pack-obj "protein" protein)

                        :cds
                        (pace-utils/vmap
                          :text (pace-utils/vassoc (pack-obj "cds" cds) :footnotes footnotes)
                          :evidence (if (not (empty? status))
                                      {:status status}))

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
                        (seq
                          (for [s seqs]
                            (get-transcript-length s)))

                        :length_protein
                        (:protein.peptide/length (:protein/peptide protein))

                        :type (if seqs
                                (if-let [mid (:method/id
                                               (or (:transcript/method sequence)
                                                   (:pseudogene/method sequence)
                                                   (:cds/method sequence)))]
                                  (str/replace mid #"_" " ")))))};}
            )))
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
    {:data (gene-model-data db coding? seqs)
     :description "gene models for this gene"}))

;(defn gene-model-data [db coding? seqs]
;  (->>
;    (map (partial d/entity db) seqs)
;    (sort-by (some-fn :cds/id :transcript/id :pseudogene/id))
;    (reduce
;      (fn [{:keys [table remark-map]} sequence]
;        (if-let [features (get-features-from-sequence sequence)]
;          (let [cds (or
;                      (and (:cds/id sequence) sequence)
;                      (:transcript.corresponding-cds/cds (:transcript/corresponding-cds sequence))
;                      (:pseudogene.corresponding-cds/cds (:psuedogene/corresponding-cds sequence)))
;                protein (:cds.corresponding-protein/protein (:cds/corresponding-protein cds))
;                seqs (or (seq (map :transcript.corresponding-cds/_cds (:transcript/_corresponding-cds cds)))
;                         [sequence])
;                status (str (obj/humanize-ident (:cds/prediction-status cds))
;                            (if (:cds/matching-cdna cds)
;                              " by cDNA(s)"))
;                {:keys [remark-map footnotes]}
;                (reduce (fn [{:keys [remark-map footnotes]} r]
;                          (let [pr (if-let [ev (obj/get-evidence r)]
;                                     {:text (:cds.remark/text r)
;                                      :evidence ev}
;                                     (:cds.remark/text r))]
;                            (if-let [n (get remark-map pr)]
;                              {:remark-map remark-map
;                               :footnotes (pace-utils/conjv footnotes n)}
;                              (let [n (inc (count remark-map))]
;                                {:remark-map (assoc remark-map pr n)
;                                 :footnotes (pace-utils/conjv footnotes n)}))))
;                        {:remark-map remark-map}
;                        (:cds/remark cds))]
;            {:remark-map remark-map
;             :table (pace-utils/conjv
;                      table
;                      (pace-utils/vmap
;                        :model
;                        (map pack-obj seqs)
;
;                        :method (:method/id (:locatable/method sequence))
;
;                        :features features
;
;                        :keys (keys sequence)
;                        :protein
;                        (pack-obj "protein" protein)
;
;                        :cds
;                        (pace-utils/vmap
;                          :text (pace-utils/vassoc (pack-obj "cds" cds) :footnotes footnotes)
;                          :evidence (if (not (empty? status))
;                                      {:status status}))
;
;                        :length_spliced
;                        (if coding?
;                          (let [length-spliced (if-let [exons (seq (:cds/source-exons cds))]
;                                                 (->>
;                                                   (map
;                                                     (fn [ex]
;                                                       (- (:cds.source-exons/end ex)
;                                                          (:cds.source-exons/start ex) -1))
;                                                     exons)
;                                                   (reduce +)))]
;                            (if (= length-spliced 0)
;                              "-"
;                              length-spliced)))
;
;                        :length_unspliced
;                        (seq
;                          (for [s seqs]
;                            (get-transcript-length s)))
;
;                        :length_protein
;                        (:protein.peptide/length (:protein/peptide protein))
;
;                        :type (if seqs
;                                (if-let [mid (:method/id
;                                               (or (:transcript/method sequence)
;                                                   (:pseudogene/method sequence)
;                                                   (:cds/method sequence)))]
;                                  (str/replace mid #"_" " ")))))};}
;            )))
;      {})
;    ((fn [{:keys [table remark-map]}]
;       (pace-utils/vmap
;         :table table
;         :remarks (if-not (empty? remark-map)
;                    (into (sorted-map)
;                          (for [[r n] remark-map]
;                            [n r]))))))))

(def widget
  {:name        generic/name-field
   :gene_models gene-models})
