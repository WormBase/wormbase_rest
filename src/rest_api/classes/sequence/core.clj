(ns rest-api.classes.sequence.core
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.db.sequence :as seqdb]
    [pseudoace.utils :as pace-utils]
    [rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name id role]
  (let [db ((keyword db-name) wb-seq/sequence-dbs)]
    (case role
      "variation" (wb-seq/variation-features db id)
      "cds" (wb-seq/sequence-features-where-type db id "CDS%")
      "pcr-product" (wb-seq/sequence-features-where-type db id "PCR_product%")
      (wb-seq/get-features db id))))

(defn get-g-species [object role]
  (when-let [species-name (:species/id
                             (or
                               ((keyword role "species") object)
                               (or
                                 (:clone/species
                                   (first
                                     (:pcr-product/clone object)))
                                 (:transcript/species
                                   (first
                                     (:transcript/_corresponding-pcr-product object))))))]
    (generic-functions/xform-species-name species-name)))

(defn get-segments [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    (let [g-species (get-g-species object role)
          sequence-database (seqdb/get-default-sequence-database g-species)]
      (when sequence-database
        (sequence-features sequence-database (id-kw object) role)))))

(defn get-transcript-segments [object]
  (let [g-species (get-g-species object "transcript")
        sequence-database (seqdb/get-default-sequence-database g-species)]
    (when sequence-database
      (when-let [db ((keyword sequence-database) wb-seq/sequence-dbs)]
        (wb-seq/get-seq-features db (:transcript/id object))))))

(defn longest-segment [segments]
  (first
    (sort-by #(- (:start %) (:end %)) segments)))

(defn get-longest-segment [object]
  (let [segments (get-segments object)]
    (if (seq segments)
      (longest-segment segments))))

(defn create-genomic-location-obj [start stop object segment tracks gbrowse img]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        calc-browser-pos (fn [x-op x y mult-offset]
                            (if gbrowse
                              (->> (reduce - (sort-by - [x y]))
                                   (double)
                                   (* mult-offset)
                                   (int)
                                   (x-op x))
                              y))
        browser-start (calc-browser-pos - start stop 0.2)
        browser-stop (calc-browser-pos + stop start 0.5)
        id (str (:seqname segment) ":" browser-start ".." browser-stop)
        label (if (= img true)
                   id
                 (str (:seqname segment) ":" start ".." stop))]
    (pace-utils/vmap
      :class "genomic_location"
      :id id
      :label label
      :pos_string id
      :seqname (:seqname segment)
      :start start
      :stop stop
      :taxonomy (get-g-species object role)
      :tracks tracks)))

(defn genomic-obj [object]
  (when-let [segment (get-longest-segment object)]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true true))))

(defn genomic-obj-position [object]
  (when-let [segment (get-longest-segment object)]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true false))))

(defn genomic-obj-child-positions [object]
  (some->> (get-transcript-segments object)
           (map (fn [feature]
                  (conj
                    feature
                    {:type
                     (if-let [tag (:tag feature)]
                       (if-let [so-term (first (str/split tag #":"))]
                         so-term
                         "unknown")
                       "unknown")})))))

(defn get-sequence [seqfeature-obj]
  (let [g-species (:taxonomy seqfeature-obj)
        sequence-database (seqdb/get-default-sequence-database g-species)]
    (when sequence-database
      (let [db ((keyword sequence-database) wb-seq/sequence-dbs)
            start (:start seqfeature-obj)
            stop  (:stop seqfeature-obj)
            location (:seqname seqfeature-obj)]
        (wb-seq/get-sequence db location start stop)))))

(defn- replace-in-str [transformation in from len]
  (let [before (subs in 0 from)
        after (subs in (+ from len))
        being-replaced (subs in from (+ from len))
        replaced (case transformation
                   "uppercase" (str/upper-case being-replaced)
                   "lowercase" (str/lower-case being-replaced)
                   "remove" "")]
    (str before replaced after)))

(defn- feature-complement [features feature seq-length]
  (conj features
        {:start (+ 1 (- seq-length (:stop feature)))
         :stop (+ 1 (- seq-length (:start feature)))
         :type (:type feature)}))

(defn- add-feature [features feature next-feature-start feature-end]
  (conj
    features
    {:start next-feature-start
     :stop feature-end
     :type (:type feature)}))

(defn transcript-sequence-features [transcript padding status] ; status can be spliced or unspliced
  (when-let [refseq-obj (genomic-obj transcript)]
    (let [sequence-strand (if (> (:start refseq-obj) (:stop refseq-obj))
                            "negative"
                            "positive")
          seq-features (genomic-obj-child-positions transcript)
          three-prime-utr (filter (comp #{"three_prime_UTR"} :type) seq-features)
          five-prime-utr (filter (comp #{"five_prime_UTR"} :type) seq-features)
          features-raw (when-let [features seq-features]
                         (some->> features
                                  (map (fn [feature]
                                         (when (not= "CDS" (:type feature))
                                           {:start (+ 1
                                                      (+ padding
                                                         (if (= sequence-strand "positive")
                                                           (- (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                                three-prime-utr-start
                                                                (:start feature))
                                                              (:start refseq-obj))
                                                           (- (:stop feature)
                                                              (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                                three-prime-utr-start
                                                                (:stop refseq-obj))))))
                                            :stop (+ 1
                                                     (+ padding
                                                        (if (= sequence-strand "positive")
                                                          (- (:stop feature)
                                                             (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                               three-prime-utr-start
                                                               (:start refseq-obj)))
                                                          (- (:start feature)
                                                             (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                               three-prime-utr-start
                                                               (:stop refseq-obj))))))
                                            :type (:type feature)})))
                                  (remove nil?)))
          sequence-positive-raw (get-sequence
                                  (conj
                                    refseq-obj
                                    (if (= sequence-strand "positive")
                                      {:start (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                (- three-prime-utr-start padding)
                                                (- (:start refseq-obj) padding))
                                       :stop (if-let [five-prime-utr-stop (:stop five-prime-utr)]
                                               (+ five-prime-utr-stop padding)
                                               (+ (:stop refseq-obj) padding))}
                                      {:start (if-let [five-prime-utr-start (:start five-prime-utr)]
                                                (- five-prime-utr-start padding)
                                                (- (:stop refseq-obj) padding))
                                       :stop  (if-let [three-prime-utr-start (:start three-prime-utr)]
                                                (+ three-prime-utr-start padding)
                                                (+ (:start refseq-obj) padding))})))
          sequence-positive (let [dna-sequence (atom {:seq sequence-positive-raw})]
                              (do
                                (doseq [feature features-raw
                                        :when (= "exon" (:type feature))]
                                  (swap! dna-sequence
                                         assoc
                                         :seq
                                         (replace-in-str
                                           "uppercase"
                                           (:seq @dna-sequence)
                                           (- (:start feature) 1)
                                           (+ 1
                                              (- (:stop feature)
                                                 (:start feature))))))
                                (doseq [feature features-raw
                                        :when (or (= "three_prime_UTR" (:type feature))
                                                  (= "five_prime_UTR" (:type feature)))]
                                  (swap! dna-sequence
                                         assoc
                                         :seq
                                         (replace-in-str
                                           "lowercase"
                                           (:seq @dna-sequence)
                                           (- (:start feature) 1)
                                           (+ 1
                                              (- (:stop feature)
                                                 (:start feature))))))
                                (if (= status "spliced")
                                  (doseq [feature (reverse (sort-by :start features-raw))
                                          :when (contains? (set `("intron" "three_prime_UTR" "five_prime_UTR")) (:type feature))]
                                    (swap! dna-sequence
                                           assoc
                                           :seq
                                           (replace-in-str
                                             "remove"
                                             (:seq @dna-sequence)
                                             (- (:start feature) 1)
                                             (+ 1
                                                (- (:stop feature)
                                                   (:start feature)))))))
                                (:seq @dna-sequence)))

          positive-features (if (= status "unspliced")
                              features-raw
                              (let [pos-features (atom {:features ()})
                                    next-feature-start (atom 1)]
                                (do
                                  (doseq [feature (sort-by :start features-raw)
                                          :when (and
                                                  (not= "three_prime_UTR" (:type feature))
                                                  (not= "five_prime_UTR" (:type feature))
                                                  (not= "intron" (:type feature)))
                                          :let [feature-end (+ (deref next-feature-start) (- (:stop feature) (:start feature)))]]
                                    (swap! pos-features
                                           assoc
                                           :features
                                           (add-feature (:features @pos-features) feature (deref next-feature-start) feature-end))
                                    (reset! next-feature-start (+ 1 (+ (deref next-feature-start) (- (:stop feature) (:start feature))))))
                                  (:features @pos-features))))]
      {:positive-strand
       {:features positive-features
        :sequence sequence-positive}
       :negative-strand
       {:features (when-let [seq-length (count sequence-positive)]
                    (let [neg-features (atom {:features ()})]
                      (do
                        (doseq [feature positive-features]
                          (swap! neg-features
                                 assoc
                                 :features
                                 (feature-complement (:features @neg-features) feature seq-length)))
                        (:features @neg-features))))
        :sequence (generic-functions/dna-reverse-complement sequence-positive)}
       :sequence_strand sequence-strand})))
