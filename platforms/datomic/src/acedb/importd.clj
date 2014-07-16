(ns acedb.importd
  (:use acedb.acefile)
  (:require [clojure.core.match :refer (match)]
            [clojure.string :as str]
            [datomic.api :as d]))

(defn vmap
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values."
  [& args] 
  (into {} (for [[k v] (partition 2 args) 
                 :when (not (nil? v))] 
             [k v])))

(defmulti ace-to-datomic :class)

(defmethod ace-to-datomic "LongText"
  [obj]
  (let [{:keys [id text]} obj]
    [{:db/id           (d/tempid :db.part/user)
      :longtext/id     id
      :longtext/text   (unescape text)}]))

(defmethod ace-to-datomic "Paper"
  [obj]
  (let [authors         (select obj ["Author"])
        [[ref_title]]   (select obj ["Reference" "Title"])
        [[ref_journal]] (select obj ["Reference" "Journal"])
        [[ref_volume]]  (select obj ["Reference" "Volume"])
        [[ref_page]]    (select obj ["Reference" "Page"])
        [[brief_cite]]  (select obj ["Brief_citation"])
        [[abstract]]    (select obj ["Abstract"])
        trans (vmap :db/id            (d/tempid :db.part/user)
                    :paper/id         (:id obj)
                    :paper/author     (map-indexed
                                       (fn [index [author & _]]
                                         {:db/id     (d/tempid :db.part/user)
                                          :paper.author/ordinal (inc index)
                                          :paper.author/name author})
                                       authors)
                    :paper/ref.title   ref_title
                    :paper/ref.journal ref_journal
                    :paper/ref.volume  ref_volume
                    :paper/ref.page    ref_page
                    :paper/brief.citation  (unescape brief_cite))]
    [(if abstract
       (assoc trans :paper/abstract {:db/id        (d/tempid :db.part/user)
                                     :longtext/id  abstract})
       trans)]))

(defn conj-into 
  "Update a map with key-value pairs, conj-ing new values onto sequences"
  [m kvs]
  (reduce (fn [m [k v]]
            (if k
              (if-let [old (m k)]
                (assoc m k (conj old v))
                (assoc m k [v]))
              m))
          m kvs))

(defn- evidence-to-datomic [evseq]
  (conj-into {}
    (for [e evseq]
      (match (vec e)
        ["Inferred_automatically" s]   [:evidence/automatic s]
        ["Paper_evidence" p]           [:evidence/paper {:paper/id p}]
        ["Person_evidence" s]          [:evidence/person s]
        ["Curator_confirmed" s]        [:evidence/curator s]
        ["RNAi_evidence" s]            [:evidence/rnai {:rnai/id s}]
        ["Date_last_updated" _]        nil    ; This should be handled by transactions....
        :else (throw (Exception. (str "Don't understand evidence " e)))))))
  

(defn- gene-rnai-to-datomic [[rnai & evidence]]
  (assoc (evidence-to-datomic [evidence])
         :gene.rnai/rnai {:rnai/id rnai}))

(defn- gene-desc-to-datomic [concise]
  (when (seq concise)
    (assoc (evidence-to-datomic (map rest concise))
      :gene.desc/concise (unescape (ffirst concise)))))

(defmethod ace-to-datomic "Gene"
  [obj]
  (let [[[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        refs                (map first (select obj ["Reference"]))]
    [(vmap :db/id               (d/tempid :db.part/user)
           :gene/id             (:id obj)
           :gene/name.cgc       cgc_name
           :gene/name.sequence  seq_name
           :gene/name.public    pub_name
           :gene/desc           (gene-desc-to-datomic concise)
           :gene/rnai           (seq (map gene-rnai-to-datomic rnais)) ; `seq` replaces empty collection with `nil`
           :gene/reference      (seq (for [r refs]
                                       {:db/id        (d/tempid :db.part/user)
                                        :paper/id     r})))]))  

(def ^:private rnai-delivery-to-datomic
  {"Bacterial_feeding"       :rnai.delivery/feeding
   "Injection"               :rnai.delivery/injection
   "Soaking"                 :rnai.delivery/soaking
   "Transgene_expression"    :rnai.delivery/transgene})

(defmethod ace-to-datomic "RNAi"
  [obj]
  (let [[[delivery]]        (select obj ["Experiment" "Delivered_by"])
        [[strain]]          (select obj ["Experiment" "Strain"])
        phenotypes          (map first (select obj ["Phenotype"]))
        not_phenotypes      (map first (select obj ["Phenotype_not_observed"]))
        refs                (map first (select obj ["Reference"]))]
    [(vmap :db/id                (d/tempid :db.part/user)
           :rnai/id              (:id obj)
           :rnai/expt.strain     strain
           :rnai/expt.delivery   (rnai-delivery-to-datomic delivery)
           :rnai/phenotype       (for [p phenotypes]
                                   {:db/id        (d/tempid :db.part/user)
                                    :phenotype/id p})
           :rnai/not.phenotype   (for [p not_phenotypes]
                                   {:db/id        (d/tempid :db.part/user)
                                    :phenotype/id p})
           :rnai/reference       (for [r refs]
                                   {:db/id        (d/tempid :db.part/user)
                                    :paper/id     r}))]))

(defmethod ace-to-datomic "Phenotype"
  [obj]
  (let [[[desc]]      (select obj ["Description"])
        [[name]]      (select obj ["Name" "Primary_name"])]
    [(vmap :db/id                  (d/tempid :db.part/user)
           :phenotype/id           (:id obj)
           :phenotype/name         name
           :phenotype/description  desc)]))

(defmethod ace-to-datomic :default
  [obj]
  [])   ; Make no assertions about AceDB objects we don't understand

(defn import-acefile 
  "Open `file` as a .ace file.  Convert objects we understand into
  Datomic transactions"
  [file]
  (->> (ace-reader file)
       (ace-seq)
       (partition-by :class)
       (map #(mapcat ace-to-datomic %))
       (filter seq)))    ; Discard any empty transactions
           
