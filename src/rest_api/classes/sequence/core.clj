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

(defn get-transcript-segments [object feature-id]
  (let [g-species (get-g-species object "transcript")
        sequence-database (seqdb/get-default-sequence-database g-species)]
    (when sequence-database
      (when-let [db ((keyword sequence-database) wb-seq/sequence-dbs)]
        (wb-seq/get-seq-features db feature-id)))))

(defn longest-segment [segments]
  (first
    (sort-by #(- (:start %) (:end %)) segments)))

(defn get-longest-segment [object]
  (let [segments (get-segments object)]
    (if (seq segments)
      (longest-segment segments))))

(defn get-transcript-segment [object]
  (some->> (get-segments object)
           (filter #(not (str/starts-with? (:type %) "Gene")))
           (first)))

(defn create-genomic-location-obj [start stop object segment tracks gbrowse]
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
        browser-start (calc-browser-pos - start stop 0.15)
        browser-stop (calc-browser-pos + stop start 0.25)
        id (str (:seqname segment) ":" browser-start ".." browser-stop)
        label (str (:seqname segment) ":" start ".." stop)]
    (pace-utils/vmap
      :class "genomic_location"
      :id id
      :feature_id (id-kw object)
      :label label
      :pos_string id
      :seqname (:seqname segment)
      :start start
      :stop stop
      :taxonomy (get-g-species object role)
      :tracks tracks)))

(defn genomic-obj [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
  (when-let [segment (if (= "transcript" role)
                       (get-transcript-segment object)
                       (get-longest-segment object))]
    (let [[start stop] (->> segment
                             ((juxt :start :end))
                             (sort-by +))]
      (create-genomic-location-obj start stop object segment nil true)))))

(defn genomic-obj-child-positions [object feature-id]
  (some->> (get-transcript-segments object feature-id)
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

(defn- get-spliced-exon-positions [positive-features]
  (let [last-stop (atom 0)]
    (for [feature (sort-by :start positive-features)
          :when (= (:type feature) :exon)
          :let [last-stop-position @last-stop
                new-stop-position (+ last-stop-position
                                     (+ 1
                                        (- (:stop feature) (:start feature))))
                new-start-position (+ 1 last-stop-position)]]
      (do (reset! last-stop new-stop-position)
          {:start new-start-position
           :stop new-stop-position
           :type (:type feature)}))))

(defn- add-padding-to-feature-list [features padding length]
  (when (> padding 0)
    ((comp vec flatten conj) features
     [{:type :padding
       :start 1
       :stop  padding}
      {:type :padding
       :start (+ (- length padding) 1)
       :stop length}])))

(defn transcript-sequence-features [transcript padding status]
  (when-let [refseq-obj (genomic-obj transcript)]
    (let [seq-features (genomic-obj-child-positions transcript (:feature_id refseq-obj))]
        (let [status-parts  (case status
                              :spliced
                              #{:exon :three_prime_UTR :five_prime_UTR}

                              :cds
                              #{:exon}

                              #{:intron :exon :three_prime_UTR :five_prime_UTR})
              three-prime-utr (first (filter (comp #{"three_prime_UTR"} :type) seq-features))
              five-prime-utr (first (filter (comp #{"five_prime_UTR"} :type) seq-features))
              cds (first (filter (comp #{"CDS"} :type) seq-features))
              sequence-strand (if (some nil? [three-prime-utr  five-prime-utr])
                                (when-let [strand (:locatable/strand transcript)]
                                  (cond
                                    (= strand :locatable.strand/negative) "-"
                                    (= strand :locatable.strand/positive) "+"))
                                (if (< (:start five-prime-utr) (:stop three-prime-utr)) "+" "-"))
              context-obj (if (and (= status :cds) (some? cds))cds refseq-obj)
              [context-left context-right] (if (neg? (- (:start context-obj) (:stop context-obj)))
                                             [(- (:start context-obj) padding) (+ (:stop context-obj) padding)]
                                             [(- (:stop context-obj) padding) (+ (:start context-obj) padding)])
              positive-features (some->> seq-features
                                         (map (fn [feature]
                                                (let [feature-type (keyword (:type feature))
                                                      [left-position right-position]
                                                      (if (neg? (- (:start feature) (:stop feature)))
                                                        [(:start feature) (:stop feature)]
                                                        [(:stop feature) (:start feature)])]
                                                  (when (and (not= feature-type :CDS)
                                                             (not
                                                               (and (= status :cds)
                                                                    (or (= feature-type :five_prime_UTR)
                                                                        (= feature-type :three_prime_UTR)))))
                                                    {:start (let [start (+ 1 (- left-position context-left))]
                                                              (if (neg? start) 1 start))
                                                     :stop (let [stop (+ 1 (- right-position context-left))]
                                                             (let [length (+ 1 (- context-right context-left))]
                                                               (if (> stop length) length stop)))
                                                     :type feature-type}))))
                                         (remove nil?))
              sequence-positive-raw (get-sequence
                                      (conj
                                        refseq-obj
                                        {:start context-left
                                         :stop context-right}))
              sequence-positive (let [dna-sequence (atom {:seq sequence-positive-raw})]
                                  (do
                                    (doseq [feature positive-features
                                            :when (= :exon (:type feature))]
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
                                    (doseq [feature positive-features
                                            :when (or (= :three_prime_UTR (:type feature))
                                                      (= :five_prime_UTR (:type feature)))]
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
                                    (if (contains? #{:cds :spliced} status)
                                      (doseq [feature (reverse (sort-by :start positive-features))
                                              :when (not (some #(= (:type feature) %) status-parts))]
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
              modified-positive-features (case status
                                           :unspliced
                                           positive-features

                                           :cds
                                           (get-spliced-exon-positions positive-features)

                                           :spliced
                                           (remove nil?
                                                   (flatten
                                                     (conj
                                                       (get-spliced-exon-positions positive-features)
                                                       (if (= sequence-strand "+")
                                                         (first (filter #(= (:type %) :five_primeUTR) positive-features))
                                                         (first (filter #(= (:type %) :three_prime_UTR) positive-features)))
                                                       (let [feature (if (= sequence-strand "+")
                                                                       (first (filter #(= (:type %) :three_prime_UTR) positive-features))
                                                                       (first (filter #(= (:type %) :five_prime_UTR) positive-features)))
                                                             end (count sequence-positive)]
                                                         (if (some? feature)
                                                           (conj
                                                             feature
                                                             {:start (- end
                                                                        (+ 1 (- (:stop feature) (:start feature))))
                                                              :stop (count sequence-positive)})))))))
              modified-positive-features-with-padding (if (> padding 0)
                                                        (add-padding-to-feature-list
                                                          modified-positive-features
                                                          padding
                                                          (count sequence-positive))
                                                        modified-positive-features)]
                   {:positive_strand
                    {:features modified-positive-features-with-padding
                    :sequence sequence-positive}
                    :negative_strand
                    {:features (when-let [seq-length (count sequence-positive)]
                                 (let [neg-features (atom {:features ()})]
                                   (do
                                     (doseq [feature modified-positive-features-with-padding]
                                       (swap! neg-features
                                              assoc
                                              :features
                                              (feature-complement (:features @neg-features) feature seq-length)))
                                     (:features @neg-features))))
                     :sequence (generic-functions/dna-reverse-complement sequence-positive)}
                    :strand sequence-strand}))))
