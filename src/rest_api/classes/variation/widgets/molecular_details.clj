(ns rest-api.classes.variation.widgets.molecular-details
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.db.sequence :as seqdb]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn- parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(defn- str-insert
  "Insert c in string s at index i."
  [s c i]
  (str (subs s 0 i) c (subs s i)))

(defn- get-deletion-str [variation]
  (or
    (when-let [deletion (:variation.deletion/text
                          (:variation/deletion variation))]
      (str/lower-case deletion))
    (if-let [refseqobj  (sequence-fns/genomic-obj variation)] ;WBVar00145789
      (sequence-fns/get-sequence refseqobj))))

(defn- fetch-coords-in-feature [varrefseqobj object]
  (when-let [refseqobj (sequence-fns/genomic-obj object)]
    (if (and
          (or (contains? object :cds/id)
              (contains? object :pseudogene/id))
          (= :locatable.strand/negative
             (:locatable/strand object)))
      {:fstart (:start refseqobj)
       :fstop (:stop refseqobj)
       :start (+ 1
                 (- (:stop refseqobj)
                    (:stop varrefseqobj)))
       :stop (+ 1
                (- (:stop refseqobj)
                   (:start varrefseqobj)))
       :abs_start (:start varrefseqobj)
       :abs_stop (:stop varrefseqobj)
       :item (pack-obj object)}
      {:fstart (:start refseqobj)
       :fstop (:stop refseqobj)
       :start (+ 1
                 (- (:start varrefseqobj)
                    (:start refseqobj)))
       :stop (+ 1
                (- (:stop varrefseqobj)
                   (:start refseqobj)))
       :abs_start (:start varrefseqobj)
       :abs_stop (:stop varrefseqobj)
       :item (pack-obj object)})))

(defn- get-cgh-deleted-probes [variation]
   (when-let [dp (:variation/cgh-deleted-probes variation)]
           {:left_flank (:variation.cgh-deleted-probes/text-a dp)
            :right_flank (:variation.cgh-deleted-probes/text-b dp)}))

(defn- get-cgh-flanking-probes [variation]
   (when-let [fp (:variation/cgh-flanking-probes variation)]
           {:left_flank (:variation.cgh-flanking-probes/text-a fp)
            :right_flank (:variation.cgh-flanking-probes/text-b fp)}))

(defn- get-feature-affected-evidence [feature]
  (if-let [rt (some (fn [[k v]] (if (= (name k) "text") v)) feature)]
    (pace-utils/vmap
      :evidence_type
      (when-let [evidence-str (str/replace rt "*" "STOP")] evidence-str)

      :evidence
      (when (contains? feature :evidence/inferred-automatically)
        "Inferred Automatically"))
    (let [ev (obj/get-evidence feature)]
      (not-empty
        (pace-utils/vmap
          :evidence_type
          (some->> ev vals flatten first)

          :evidence
          (some->> ev keys first))))))

(defn- mutant-conceptual-translation [pseq position from to]
  (str
    (subs
      pseq
      0
      (- position 1))
    to
    (subs
      pseq
      (- (+
          position
          (count from))
         1))))

(defn- get-silent-obj [predicted-cds-holder cds]
  (when-let [holder (first (:molecular-change/silent predicted-cds-holder))]
     (let [description (:molecular-change.silent/text holder)
           [full added-aa location-str] (re-matches #"(.*)\ \((.*)\)" description)
           location (Integer/parseInt location-str)
           protein (:cds.corresponding-protein/protein
                     (:cds/corresponding-protein cds))
           peptide (:protein.peptide/peptide
                     (:protein/peptide protein))
           pseq (:peptide/sequence peptide)]
      (conj
        {:aa_change description
         :mutant_start (str (count pseq))
         :mutant_stop (str (+ (count pseq)
                       (count added-aa)))
         :wildtype_start (str (count pseq))
         :wildtype_stop (str (count pseq))
         :description description
         :protein (pack-obj protein)
         :peptide (pack-obj peptide)
         :wildtype_conceptual_translation pseq ;eg. WBVar00466445
         :mutant_conceptual_translation pseq}
        (get-feature-affected-evidence holder)))))

(defn- remove-from-end [s end]
  (if (and (and (some? s) (some? end))
           (.endsWith s end))
    (.substring s 0 (- (count s)
                       (count end)))
    s))


(defn- get-readthrough-obj [predicted-cds-holder cds]
  (when-let [holder (first (:molecular-change/readthrough predicted-cds-holder))]
     (let [description (when-let[t (:molecular-change.readthrough/text holder)]
                        (str/replace t "*" "STOP"))
           [tmp-txt removed-aa added-aa] (re-matches #"(.*)\* to (.*)"(:molecular-change.readthrough/text holder))
           protein (:cds.corresponding-protein/protein
                     (:cds/corresponding-protein cds))
           peptide (:protein.peptide/peptide
                     (:protein/peptide protein))
           pseq (:peptide/sequence peptide)]
       (conj
         {:aa_change description
          :mutant_start (str (- (count pseq)
                                (count removed-aa)))
          :mutant_stop (str (- (+ (count pseq)
                                  (count added-aa))
                               (count removed-aa)))
          :wildtype_start (str (- (count pseq) (count removed-aa)))
         :wildtype_stop (str (count pseq))
         :description description
         :protein (pack-obj protein)
         :peptide (pack-obj peptide)
         :wildtype_conceptual_translation pseq ;eg. WBVar00466445
         :mutant_conceptual_translation (str (remove-from-end pseq removed-aa) added-aa)}
        (get-feature-affected-evidence holder)))))

(defn- get-nonsense-obj [predicted-cds-holder cds]
  (when-let [holder (:molecular-change/nonsense predicted-cds-holder)]
     (let [description (:molecular-change.nonsense/text holder)
           mutant-stop (last (re-matches #".*\((\d+)\).*" description))
           position (re-matches #"\(.*\)" description)
           protein (:cds.corresponding-protein/protein
                     (:cds/corresponding-protein cds))
           peptide (:protein.peptide/peptide
                     (:protein/peptide protein))
           pseq (:peptide/sequence peptide)]
      (conj
        {:aa_change (str (first description) " to STOP")
         :mutant_start mutant-stop
         :mutant_stop mutant-stop
         :wildtype_start mutant-stop
         :wildtype_stop (str (count pseq))
         :description description
         :protein (pack-obj protein)
         :peptide (pack-obj peptide)
         :wildtype_conceptual_translation pseq ;eg. WBVar00466445
         :mutant_conceptual_translation (->> (parse-int mutant-stop)
                                             (dec)
                                             (subs pseq 0)
                                             (format "%s*"))}
        (get-feature-affected-evidence holder)))))

(defn- get-missense-obj [predicted-cds-holder cds]
  (when-let [m (first (:molecular-change/missense predicted-cds-holder))]
    (let [description (:molecular-change.missense/text m)
          position (:molecular-change.missense/int m)
          [full from to] (re-matches #"(.*)\sto\s(.*)" description)
          protein (:cds.corresponding-protein/protein
                    (:cds/corresponding-protein cds))
          peptide (:protein.peptide/peptide
                    (:protein/peptide protein))
          pseq (:peptide/sequence peptide)]
      (conj
        {:aa_change (str from position to)
         :position position
         :from from
         :to to
         :mutant_start position
         :mutant_stop position
         :wildtype_start position
         :wildtype_stop position
         :description description
         :protein (pack-obj protein)
         :peptide (pack-obj peptide)
         :wildtype_conceptual_translation pseq ; eg. WBVar01684110
         :mutant_conceptual_translation (mutant-conceptual-translation pseq position from to)}
        (get-feature-affected-evidence m)))))

(defn polymorphism-type [variation]
  {:data (if (contains? variation :variation/snp)
           (if (contains? variation :variation/reference-strain-digest)
             "SNP and RFLP"
             "SNP")
           (when-let [insertion-str (:transposon-family/id
                                      (first
                                        (:variation/transposon-insertion variation)))]
             (str insertion-str " transposon insertion" )))
   :description "the general class of this polymorphism"})


(defn amino-acid-change [variation] ; e.g. WBVar00271007
 {:data (some->> (:variation/transcript variation)
                 (map (fn [h]
                       (when-let [cds  (some->> (:variation.transcript/transcript h)
                                                (:transcript/corresponding-cds)
                                                (:transcript.corresponding-cds/cds)
                                                (pack-obj))]
                        {(:id cds)
                         {:amino_acid_change
                          (some->> (:molecular-change/amino-acid-change h)
                                   (first)
                                   (:molecular-change.amino-acid-change/text))

                          :transcript
                          cds}})))
                   (into {})
                   (vals))
 :description "amino acid changes for this variation, if appropriate"})


(defn detection-method [variation] ; WBVar00601206
  {:data (first (:variation/detection-method variation))
   :description "detection method for polymorphism, typically via sequencing or restriction digest."})

(defn deletion-verification [variation]; e.g. WBVar00278357
  {:data (some->> (:variation/deletion-verification variation)
                  (map (fn [h]
                         {:text (:variation.deletion-verification/text h)
                          :evidence (obj/get-evidence h)})))
   :description "the method used to verify deletion alleles"})

;tested with: WBVar00750883,
(defn sequence-context [variation]
  {:data (when-let [refseqobj (sequence-fns/genomic-obj variation)]
           (let [max-seqlen 1000000
                 padding 500
                 flank-length 25

                 strand (if (= (:locatable/strand variation) :locatable.strand/negative) "-" "+")

                 seq-length (+ 1
                               (- (:stop refseqobj)
                                  (:start refseqobj)))

                 refseqobj (if (and (= seq-length 1)
                                   (and (contains? variation :variation/insertion)
                                        (not (contains? variation :variation/deletion))))
                             (conj
                               refseqobj
                               {:start (+ 1 (:start refseqobj))
                                :stop (:stop refseqobj)})
                             refseqobj)

                 placeholder (when (> seq-length 1000000)
                               seq-length)

                 wildtype-sequence (when (nil? placeholder)
                                     (sequence-fns/get-sequence
                                       (conj
                                         refseqobj
                                         {:start (- (:start refseqobj) padding)
                                          :stop (+ padding (:stop refseqobj))})))

                 wildtype-full-length (+ (* 2 padding)
                                         seq-length)

                 length-change (if-let [insertion (:variation/insertion variation)]
                                 (or (if-let [insertion-str (let [insertion (or (:variation.insertion/text insertion)
                                                                                (or
                                                                                  (:transposon-family/id
                                                                                    (first
                                                                                      (:variation/transposon-insertion variation)))
                                                                                  "-"))]
                                                              (if (= insertion "Mos") "<Mos>" insertion))]
                                       (if (contains? variation :variation/deletion)
                                         (- (count insertion-str)
                                            (count (get-deletion-str variation)))
                                         (count insertion-str))
                                       (if (contains? variation :variation/deletion)
                                         (- 1 (count (get-deletion-str variation)))
                                         (when (contains? variation :variation/transposon-insertion)
                                           (- (count (:transposon-family/id
                                                       (first (:variation.transpon-insertion variation))))
                                              1)))))
                                 (if (contains? variation :variation/deletion)
                                   (- 1 (count (get-deletion-str variation)))
                                   (if-let [substitution (:variation/substitution variation)]
                                     (- (count (:variation.substitution/alt substitution))
                                        (count (:variation.substitution/ref substitution)))
                                     (when (contains? variation :variation/tandem-duplication)
                                       (- 1 seq-length)))))

                 cgh-deleted-probes (get-cgh-deleted-probes variation)

                 wildtype-positive (when (nil? placeholder)
                                     {:sequence (if (and
                                                      (not (contains? variation :variation/deletion))
                                                      (or
                                                        (contains? variation :variation/insertion)
                                                        (contains? variation :variation/transpson-insertion)))
                                                  (str-insert wildtype-sequence
                                                              "-"
                                                              padding)
                                                  (apply str (map-indexed (fn [index val]
                                                                            (if (and (>= index padding)
                                                                                     (<= index (- (+ padding
                                                                                                     seq-length)
                                                                                                  1)))
                                                                              (str/upper-case val)
                                                                              val))
                                                                          wildtype-sequence)))
                                      :features
                                      (pace-utils/vmap
                                        :variation {:type "variation"
                                                    :start (+ padding 1)
                                                    :stop (+ padding
                                                             seq-length)}
                                        :left_cgh_deleted_flank (when cgh-deleted-probes
                                                                  {:type "cgh_deleted_probe"
                                                                   :start (+ padding 1)
                                                                   :stop (+ padding
                                                                            (count (:left_flank cgh-deleted-probes)))})
                                        :left_flank {:type "flank"
                                                     :start (- (+ padding 1) flank-length)
                                                     :stop padding}
                                        :right_flank {:type "flank"
                                                      :start (+ padding 1 seq-length)
                                                      :stop (+
                                                             padding
                                                             seq-length
                                                             flank-length)}
                                        :right_cgh_deleted_flank (when cgh-deleted-probes
                                                                   {:type "cgh_deleted_probe"
                                                                    :start (- (+ padding 1 seq-length)
                                                                              (count (:right_flank cgh-deleted-probes)))
                                                                    :stop (+ padding seq-length)}))})

                 wildtype-negative (when (nil? placeholder)
                                     {:sequence
                                      (generic-functions/dna-reverse-complement (:sequence wildtype-positive))

                                      :features
                                      (:features wildtype-positive)})

                 mutant-positive (when (nil? placeholder)
                                   {:sequence
                                    (cond
                                      (contains? variation :variation/substitution)
                                      (let [substitution (:variation/substitution variation)
                                            varseq (str/upper-case
                                                     (sequence-fns/get-sequence
                                                       (conj
                                                         refseqobj
                                                         {:start (:start refseqobj)
                                                          :stop (:stop refseqobj)})))
                                            refseq (str/upper-case
                                                     (:variation.substitution/ref substitution))
                                            altseq (str/upper-case
                                                     (:variation.substitution/alt substitution))
                                            wildtype-seq (:sequence wildtype-positive)]
                                        (str
                                          (subs wildtype-seq 0 padding)
                                          (cond
                                            (= varseq refseq)
                                            (str/upper-case altseq)

                                            (= varseq (generic-functions/dna-reverse-complement refseq))
                                            (str/upper-case (generic-functions/dna-reverse-complement altseq))

                                            :else
                                            (throw (Exception. "substitution/ref does not match either + or - strand")))
                                          (subs wildtype-seq (+ padding (count varseq)))))

                                      (and (contains? variation :variation/insertion)
                                            (contains? variation :variation/deletion))
                                         (let [insertion (:variation/insertion variation)
                                               insert-str (or
                                                            (:variation.insertion/text insertion)
                                                            (or (:transposon-family/id
                                                                  (first
                                                                    (:variation/transposon-insertion variation))))
                                                            "-")]
                                           (str/replace
                                             (:sequence wildtype-positive)
                                             #"[A-Z]+"
                                             (if (str/includes? (:sequence wildtype-positive)
                                                                (str/upper-case (get-deletion-str variation)))
                                               (str/upper-case insert-str)
                                               (generic-functions/dna-reverse-complement
                                                 (str/upper-case
                                                   insert-str)))))

                                      (contains? variation :variation/insertion)
                                      (let [insertion (:variation/insertion variation)
                                            insert-str (or (if-let [insert-value (:variation.insertion/text insertion)]
                                                             (if (= strand "+")
                                                               insert-value
                                                               (generic-functions/dna-reverse-complement insert-value)))
                                                           (or
                                                             (:transposon-family/id
                                                               (first
                                                                 (:variation/transposon-insertion variation)))
                                                             "-"))]
                                        (str/replace
                                          (:sequence wildtype-positive)
                                          #"\-+"
                                          (if (= insert-str "Mos") "<Mos>" (str/upper-case insert-str))))

                                      (or
                                        (contains? variation :variation/deletion)
                                        (contains? variation :variation/tandem-duplication))
                                      (str/replace
                                        (:sequence wildtype-positive)
                                        #"[A-Z]+"
                                        "-"))

                                    :features
                                    {:variation
                                     {:type "variation"
                                      :start (:start (:variation (:features wildtype-positive)))
                                      :stop (if (and (contains? variation :variation/insertion)
                                                     (not (contains? variation :variation/deletion)))
                                              (- (+ (:stop (:variation (:features wildtype-positive)))
                                                 length-change) 1)
                                              (+ (:stop (:variation (:features wildtype-positive)))
                                                 length-change))}

                                     :left_flank
                                     (:left_flank (:features wildtype-positive))

                                     :right_flank
                                     {:type "flank"
                                      :start (if (and (contains? variation :variation/insertion)
                                                      (not (contains? variation :variation/deletion)))
                                               (- (+ (:start (:right_flank (:features wildtype-positive)))
                                                length-change) 1)
                                               (+ (:start (:right_flank (:features wildtype-positive)))
                                                length-change))
                                      :stop (if (and (contains? variation :variation/insertion)
                                                     (not (contains? variation :variation/deletion)))
                                              (+ 1 (+ (:stop (:right_flank (:features wildtype-positive)))
                                                      length-change))
                                              (+ (:stop (:right_flank (:features wildtype-positive)))
                                                 length-change))}}})
                 mutant-negative (when (nil? placeholder)
                                   {:sequence
                                    (if-let [transposon-family-str (let [transposon-family (:transposon-family/id
                                                                                             (first
                                                                                               (:variation/transposon-insertion variation)))]
                                                                     (if (= transposon-family "Mos") "<Mos>" transposon-family))]
                                      (str/replace
                                        (generic-functions/dna-reverse-complement (:sequence mutant-positive))
                                        (re-pattern (generic-functions/dna-reverse-complement transposon-family-str))
                                        transposon-family-str)
                                      (generic-functions/dna-reverse-complement (:sequence mutant-positive)))

                                    :features
                                    (:features mutant-positive)})

                 wildtype-positive-flattened (when (nil? placeholder)
                                               {:sequence (:sequence wildtype-positive)
                                                :features (sort-by
                                                            :start
                                                            (vals (:features wildtype-positive)))})
                 wildtype-negative-flattened (when (nil? placeholder)
                                               {:sequence (:sequence wildtype-negative)
                                                :features (sort-by
                                                            :start
                                                            (vals (:features wildtype-negative)))})
                 mutant-positive-flattened (when (nil? placeholder)
                                             {:sequence (:sequence mutant-positive)
                                              :features (sort-by
                                                          :start
                                                          (vals (:features mutant-positive)))})
                 mutant-negative-flattened (when (nil? placeholder)
                                             {:sequence (:sequence mutant-negative)
                                              :features (sort-by
                                                          :start
                                                          (vals (:features mutant-negative)))})

                 species (some->> (:species/assembly (:variation/species variation))
                                  (map (fn [assembly]
                                         (:strain/id (first (:sequence-collection/strain assembly))))))]
              (when-not (every? nil? [wildtype-positive placeholder])
                (pace-utils/vmap
                  :wildtype (not-empty
                              (pace-utils/vmap
                                :positive_strand wildtype-positive-flattened
                                :negative_strand wildtype-negative-flattened))
                  :mutant (not-empty
                            (pace-utils/vmap
                              :positive_strand mutant-positive-flattened
                              :negative_strand mutant-negative-flattened))
                  :public_name (:variation/public-name variation)
                  :placeholder placeholder))))
   :description "wild type and variant sequences in genomic context"})

(defn flanking-pcr-products [variation] ; tested with WBVar00145789
  {:data (some->> (:variation/pcr-product variation)
                  (map (fn [p]
                        {(:pcr-product/id p) (pack-obj p)}))
                  (apply merge))
   :description "PCR products that flank the variation"})

;test WBVar01112111
(defn variation-type [variation]
  (let [variation-type-map {:variation/engineeered-allele "Engineered allele"
                            :variation/allele"Allele"
                            :variation/snp "SNP"
                            :variation/confirmed-snp "Confirmed SNP"
                            :variation/reference-strain-digest "RFLP"
                            :variation/predicted-snp "Predicted SNP"
                            :variation/transposon-insertion "Transposon Insertion"
                            :variation/natural-variant "Natural Variant"}
        type-of-mutation-map {:variation/substitution "Substitution"
                              :variation/insertion "Insertion"
                              :variation/deletion "Deletion"
                              :variation/tandem-duplication "Tandem Duplication"}]
    {:data {:physical_class (cond
                              (contains? variation :variation/transposon-insertion)
                              "Transposon insertion"

                              (contains? variation :variation/transposon-excision)
                              "Transposon excision"

                              :else
                              (->> type-of-mutation-map
                                   (map
                                     #(str (if
                                             (contains? variation (first %))
                                             (second %))))
                                   (filter #(not= % ""))
                                   (str/join "/")))
            :general_class (->> variation-type-map
                                (map
                                  #(str (if
                                          (contains? variation (first %))
                                          (second %))))
                                (filter #(not= % "")))}
     :description "the general type of the variation"}))

;test WBVar01112111 WBVar00601206
(defn features-affected [variation]
  {:data (let [varrefseqobj (sequence-fns/genomic-obj variation)
               d (println varrefseqobj)]
           (pace-utils/vmap
             "Clone" ;checked with WBVar00274017
             (when-let [s (:variation/mapping-target variation)]
               [(conj
                  (pack-obj s)
                  (when (and (some? varrefseqobj)
                             (not= "MTCE" (:sequence/id s)))
                    (fetch-coords-in-feature varrefseqobj s)))])

             "Chromosome"
             (some->> (:variation/gene variation)
                      (map :variation.gene/gene)
                      (map (fn [g]
                             (or (-> g ; WBVar00274017
                                     :gene/interpolated-map-position
                                     :gene.interpolated-map-position/map)
                                 (-> g ; WBVar00145789
                                     :gene/map
                                     :gene.map/map))))
                      (remove nil?)
                      (distinct)
                      (map (fn [chromosome-map]
                             (conj
                               (pack-obj chromosome-map)
                               {:item (pack-obj chromosome-map)})))
                      (not-empty))

             "Gene" ;tested with WBVar00274017
             (some->> (:variation/gene variation)
                      (map-indexed
                        (fn [idx gh]
                          (let [gene (:variation.gene/gene gh)
                                obj (pack-obj gene)]
                            {:entry (+ idx 1)
                             :item obj
                             :id (:gene/id gene)
                             :label (:label obj)
                             :class "gene"
                             :taxonomy (:taxonomy obj)}))))

             "Predicted_CDS" ;tested with WBVar01112111
             (some->> (:variation/transcript variation)
                      (map (fn [predicted-cds-holder]
                            (when-let [cds (some->> (:variation.transcript/transcript predicted-cds-holder)
                                                (:transcript/corresponding-cds)
                                                (:transcript.corresponding-cds/cds))]
                             (let [missense-obj (get-missense-obj predicted-cds-holder cds)
                                   nonsense-obj (get-nonsense-obj predicted-cds-holder cds)
                                   readthrough-obj (get-readthrough-obj predicted-cds-holder cds)
                                   cds-obj (or missense-obj
                                            (or nonsense-obj
                                                (select-keys readthrough-obj
                                                             [:from :to])))]
                            (conj
                              (pack-obj cds)
                              (when (some? varrefseqobj)
                                (fetch-coords-in-feature varrefseqobj cds)) ; appears to a discreptency. This code gives 2945
                              (select-keys cds-obj [:wildtype_conceptual_translation
                                                    :mutant_conceptual_translation
                                                    :from
                                                    :to])
                              (pace-utils/vmap
                                :protein_effects
                                (not-empty
                                  (pace-utils/vmap
                                    "Silent" ;tested with WBVar01112111
                                    (some->> (:molecular-change/silent predicted-cds-holder)
                                             (map (fn [mc]
                                                    (conj
                                                      {:description (:molecular-change.silent/text mc)}
                                                      (get-feature-affected-evidence mc)))))

                                    "Missense" ; eg. WBVar01684110
                                    (when (some? missense-obj)
                                      (select-keys missense-obj [:aa_change :evidence :evidence_type :from :to :wildtype_start :wildtype_stop :mutant_start :mutant_stop :position :description]))

                                    "Nonsense" ; eg. WBVar00466445
                                    (when (some? nonsense-obj)
                                      (select-keys nonsense-obj [:aa_change :evidence :evidence_type :wildtype_start :wildtype_stop :mutant_start :mutant_stop :description]))

                                    "Readthrough" ; eg. WBVar00215920
                                    (when (some? readthrough-obj)
                                      (select-keys readthrough-obj [:aa_change :evidence :evidence_type :wildtype_start :wildtype_stop :mutant_start :mutant_stop :description]))

                                    "Frameshift" ; e.g. WBVar00273213
                                    (when-let [fs (first (:molecular-change/frameshift predicted-cds-holder))]
                                      (conj
                                        {:description (:molecular-change.frameshift/text fs)}
                                        (get-feature-affected-evidence fs)))))

                                :location_effects
                                (not-empty
                                  (pace-utils/vmap
                                    "Coding_exon" ;tested with WBVar01112111
                                    (when-let [ce (:molecular-change/coding-exon predicted-cds-holder)]
                                      (get-feature-affected-evidence ce))

                                    "Intron" ;tested with WBVar00271172
                                    (when-let [i (:molecular-change/intron predicted-cds-holder)]
                                      (get-feature-affected-evidence i)))))))))))

             "Transcript"
             (some->> (:variation/transcript variation)
                      (map (fn [h]
                             (let [t (:variation.transcript/transcript h)]
                               (conj
                                   (pack-obj t)
                                   (when (some? varrefseqobj)
                                     (fetch-coords-in-feature varrefseqobj t))
                                   {:item
                                    (pack-obj t)

                                    :vep_consequence
                                    (some-> (:molecular-change/vep-consequence h)
                                            (first)
                                            (str/replace "_" " "))

                                    :vep_impact
                                    (some->> (:molecular-change/vep-impact h)
                                             (first)
                                             (:molecular-change.vep-impact/text)
                                             (str/lower-case))

                                    :polyphen
                                    (some->> (:molecular-change/polyphen h)
                                             (first)
                                             ((fn [polyphen]
                                                {:text (some-> (:molecular-change.polyphen/text polyphen)
                                                               (str/replace "_" " "))
                                                 :evidence {:PolyPhen_score (:molecular-change.polyphen/float polyphen)}})))
                                    :sift
                                    (some->> (:molecular-change/sift h)
                                             (first)
                                             ((fn [sift]
                                                {:text (some-> (:molecular-change.sift/text sift)
                                                               (str/replace "_" " "))
                                                 :evidence {:SIFT_score (:molecular-change.sift/float sift)}})))

                                    :cds
                                    (when (:molecular-change/cds-position h)
                                      (some->> (:variation.transcript/transcript h)
                                               (:transcript/corresponding-cds)
                                               (:transcript.corresponding-cds/cds)
                                               (pack-obj)))

                                    :cds_position
                                    (some->> (:molecular-change/cds-position h)
                                             (first)
                                             (:molecular-change.cds-position/text))

                                    :cdna_position
                                    (some->> (:molecular-change/cdna-position h)
                                             (first)
                                             (:molecular-change.cdna-position/text))

                                    :protein_position
                                    (some->> (:molecular-change/protein-position h)
                                             (first)
                                             (:molecular-change.protein-position/text))

                                    :codon_change
                                    (some->> (:molecular-change/codon-change h)
                                             (first)
                                             (:molecular-change.codon-change/text))

                                    :amino_acid_change
                                    (some->> (:molecular-change/amino-acid-change h)
                                             (first)
                                             (:molecular-change.amino-acid-change/text))

                                    :exon_number
                                    (some->> (:molecular-change/exon-number h)
                                             (first)
                                             (:molecular-change.exon-number/text))

                                    :intron_number
                                    (some->> (:molecular-change/intron-number h)
                                             (first)
                                             (:molecular-change.intron-number/text))

                                    :hgvsc
                                    (some->> (:molecular-change/hgvsc h)
                                             (first)
                                             (:molecular-change.hgvsc/text))

                                    :hgvsp
                                    (some->> (:molecular-change/hgvsp h)
                                             (first)
                                             (:molecular-change.hgvsp/text))


                                    :location_effects
                                    (not-empty
                                      (pace-utils/vmap
                                        :UTR_5
                                        (get-feature-affected-evidence
                                          (:molecular-change/five-prime-utr h))

                                        :UTR_3
                                        (get-feature-affected-evidence
                                          (:molecular-change/three-prime-utr h))))})))))

             "Pseudogene" ;tested with WBVar00601206
             (some->> (:variation/pseudogene variation)
                      (map :variation.pseudogene/pseudogene)
                      (map (fn [pseudogene]
                             (when-let [refseqobj (sequence-fns/genomic-obj pseudogene)]
                               (conj
                                 (pack-obj pseudogene)
                                 (when (some? varrefseqobj)
                                   (fetch-coords-in-feature varrefseqobj pseudogene))
                                 {:item (pack-obj pseudogene)})))))

             "Interactor"
             (some->> (:interaction.variation-interactor/_variation variation)
                      (map :interaction/_variation-interactor)
                      (map (fn [i]
                             (let [obj (pack-obj i)]
                               (conj
                                 obj
                                 {:item obj})))))))
   :description "genomic features affected by this variation"})

(defn cgh-deleted-probes [variation] ; tested with WBVar00601206
  {:data (get-cgh-deleted-probes variation)
   :description "probes used for CGH of deletion alleles"})

(defn cgh-flanking-probes [variation] ; WBVar00601206
  {:data (get-cgh-flanking-probes variation)
   :desciption "probes used for CGH of deletion alleles"})

(defn polymorphism-assays [variation]; WBVar00597552
  {:data (some->> (:variation/pcr-product variation)
                  (map (fn [pcr]
                         {(:pcr-product/id pcr)
                          (let [ohs (:pcr-product/oligo pcr)
                                first-ohs-id (:id (pack-obj (:pcr-product.oligo/oligo (first ohs))))
                                [left-oh right-oh] (if (nil? first-ohs-id)
                                                     [nil nil]
                                                     (if (str/ends-with? first-ohs-id "_b")
                                                       [(second ohs) (first ohs)]
                                                       [(first ohs) (second ohs)]))]
                            {:pcr_product
                             (conj
                               (pack-obj pcr)
                               {:pcr_conditions nil ; from pcr-product/assay-conditions non found
                                :dna nil ;non found
                                :left_oligo (-> left-oh :pcr-product.oligo/oligo :oligo/sequence)
                                :right_oligo (-> right-oh :pcr-product.oligo/oligo :oligo/sequence)})
                             :assay_type (if (contains? (-> ohs first :pcr-product.oligo/oligo) :oligo/sequence)
                                           "sequence")})}))
                  (apply merge))
   :description "experimental assays for detecting this polymorphism"})


(defn affects-splice-site [variation] ; made from WBVar00750883
 {:data (some->> (:variation/transcript variation)
                 (map (fn [h]
                       (let [t (:variation.transcript/transcript h)
                             consequence (some-> (:molecular-change/vep-consequence h)
                                                 (first)
                                                 (str/replace "_" " "))
                             hgvsc (some->> (:molecular-change/hgvsc h)
                                            (first)
                                            (:molecular-change.hgvsc/text))]
                                            (not-empty
                                             (pace-utils/vmap
                                              :donor
                                              (when (= consequence "splice donor variant") hgvsc)

                                              :acceptor
                                              (when (= consequence "splice acceptor variant")
                                               hgvsc))))))
                  (remove nil?)
                  (first)
                  (not-empty))
   :description "Affects splice site"})


(defn polymorphism-status [variation]; WBVar00116162 is predicted
  {:data (if (contains? variation :variation/confirmed-snp)
           "confirmed"
           (if (or (contains? variation :variation/snp)
                   (or (contains? variation :variation/reference-strain-digest)
                       (contains? variation :variation/transposon-insertion)))
             "predicted"))
   :description "experimental status of this polymorphism"})

(defn- pack-nulcleotide-change-obj [type-str wildtype mutant]
  {:type type-str
   :wildtype wildtype
   :mutant mutant
   :wildtype-label "wildtype"
   :mutant-label "variant"})

(defn- variation-features [variation]
  (if-let  [species-name (->> variation :variation/species :species/id)]
    (let  [g-species (generic-functions/xform-species-name species-name)
           sequence-database (seqdb/get-default-sequence-database g-species)
           db-spec ((keyword sequence-database) seqdb/sequence-dbs)
           variation-id (:variation/id variation)]
      (if sequence-database
        (seqdb/variation-features db-spec variation-id)))))

(defn- compile-nucleotide-changes [variation] ;WBVar00116162 substitution
  (remove nil?
          [(when-let [insertion (or (:variation/insertion variation)
                                    (:variation/transposon-insertion variation))] ; tested with WBVar00269113
             {:mutant (or (:variation.insertion/text
                            (:variation/insertion variation))
                          (:transposon-family/id
                            (first
                              (:variation/transposon-insertion variation))))
              :mutant_label "variant"
              :wildtype_label "wild type"
              :type "Insertion"
              :wildtype ""})
           (when-let [deletion (:variation/deletion variation)] ;eg WBVar00601206
             (if (contains? variation :variation/cgh-deleted-probes)
               {:type "Definition Deletion" ; eg WBVar00601206
                :mutant ""
                :wildtype (if-let [refseqobj  (sequence-fns/genomic-obj variation)]
                            (sequence-fns/get-sequence refseqobj))}
               {:type "Deletion" ; eg WBVar00274723
                :mutant_label "variant"
                :mutant ""
                :wildtype_label "wild type"
                :wildtype (get-deletion-str variation)}))
           (when-let [substitution (:variation/substitution variation)] ; e.g. tested with WBVar00274017
             (if-let [refseqobj (sequence-fns/genomic-obj variation)]
               (let [varseq (str/lower-case (sequence-fns/get-sequence
                                              (conj
                                                refseqobj
                                                {:start (:start refseqobj)
                                                 :stop (:stop refseqobj)})))]
                 (cond
                   (= varseq (str/lower-case
                               (:variation.substitution/ref substitution)))
                   {:mutant (:variation.substitution/alt substitution)
                    :wildtype (:variation.substitution/ref substitution)
                    :mutant_label "variant"
                    :wildtype_label "wild type"
                    :type "Substitution"}

                   (= varseq (str/lower-case
                               (generic-functions/dna-reverse-complement
                                 (:variation.substitution/ref substitution))))
                   {:mutant (generic-functions/dna-reverse-complement (:variation.substitution/alt substitution))
                    :wildtype (generic-functions/dna-reverse-complement (:variation.substitution/ref substitution))
                    :mutant_label "variant"
                    :wildtype_label "wild type"
                    :type "Substitution"}

                   :else
                   (throw (Exception. "substution/ref does not match either + or - strand"))))))]))

(defn nucleotide-change [variation]
  {:data (compile-nucleotide-changes variation)
   :description "raw nucleotide changes for this variation"})

;tested with WBVar00101112
(defn reference-strain [variation]
  {:data (some->> (:variation/strain variation)
                  (map :variation.strain/strain)
                  (map pack-obj)
                  (sort-by :label))
   :description "strains that this variant has been observed in"})

(defn causes-frameshift [variation]; e.g. WBVar01943248 this does not work on Ace version
  {:data (some->> (:variation/transcript variation)
                  (map (fn [h]
                        (let [consequence (some-> (:molecular-change/vep-consequence h)
                                                  (first)
                                                  (str/replace "_" " "))]
                        (not-empty
                         (when (= consequence "frameshift variant")
                         (some->> (:molecular-change/amino-acid-change h)
                                  (first)
                                  (:molecular-change.amino-acid-change/text)))))))
                  (remove nil?)
                  (first)
                  (not-empty))
   :description "A variation that alters the reading frame"})

(defn sequencing-status [variation]
  {:data (when-let [seqstatus (:variation/seqstatus variation)]
           (obj/humanize-ident (name seqstatus)))
   :description "sequencing status of the variation"})

(def widget
  {:name generic/name-field
   :polymorphism_type polymorphism-type
   :amino_acid_change amino-acid-change
   :detection_method detection-method
   :deletion_verification deletion-verification
   :sequence_context sequence-context
   :flanking_pcr_products flanking-pcr-products
   :variation_type variation-type
   :features_affected features-affected
   :cgh_deleted_probes cgh-deleted-probes
   :cgh_flanking_probes cgh-flanking-probes
   :polymorphism_assays polymorphism-assays
   :affects_splice_site affects-splice-site
   :polymorphism_status polymorphism-status
   :nucleotide_change nucleotide-change
   :reference_strain reference-strain
   :causes_frameshift causes-frameshift
   :sequencing_status sequencing-status})
