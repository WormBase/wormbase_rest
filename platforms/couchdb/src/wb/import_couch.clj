(ns wb.import-couch
  (:use acedb.acefile)
  (:require [clojure.core.match :refer (match)]))

(defn vmap
  "Construct a map from alternating key-value pairs, discarding any keys
  associated with nil values."
  [& args] 
  (into {} (for [[k v] (partition 2 args) 
                 :when (not (nil? v))] 
             [k v])))

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

(def ^:private ^:dynamic longtext-cache)

(defmulti ace-to-couch :class)

(defmethod ace-to-couch "Paper"
  [obj]
  (let [authors         (select obj ["Author"])
        [[ref_title]]   (select obj ["Reference" "Title"])
        [[ref_journal]] (select obj ["Reference" "Journal"])
        [[ref_volume]]  (select obj ["Reference" "Volume"])
        [[ref_page]]    (select obj ["Reference" "Page"])
        [[brief_cite]]  (select obj ["Brief_citation"])
        [[abstract]]    (select obj ["Abstract"])
        trans (vmap :_id         (:id obj)
                    :authors     (map first authors)
                    :ref_title   ref_title
                    :ref_journal ref_journal
                    :ref_volume  ref_volume
                    :ref_page    ref_page
                    :brief_citation  (unescape brief_cite)
                    :abstract    (@longtext-cache abstract))]
    [trans]))

(defn- evidence-to-couch [evseq]
  (conj-into {}
    (for [e evseq]
      (match (vec e)
        ["Inferred_automatically" s]   [:evidence_automatic s]
        ["Paper_evidence" p]           [:evidence_paper p]
        ["Person_evidence" s]          [:evidence_person s]
        ["Curator_confirmed" s]        [:evidence_curator s]
        ["RNAi_evidence" s]            [:evidence_rnai s]
        ["Date_last_updated" s]        [:evidence_date_updated s]
        :else (throw (Exception. (str "Don't understand evidence " e)))))))

(defn- gene-desc-to-couch [concise]
  (when (seq concise)
    (assoc (evidence-to-couch (map rest concise))
      :concise (unescape (ffirst concise)))))

(defmethod ace-to-couch "LongText"
  [obj]
  (swap! longtext-cache assoc (:id obj) (:text obj))
  [])


(defn- gene-rnai-to-couch [[rnai & evidence]]
  (assoc (evidence-to-couch [evidence])
         :rnai rnai))

(defmethod ace-to-couch "Gene"
  [obj]
  (let [[[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        refs                (map first (select obj ["Reference"]))]
    [(vmap :_id             (:id obj)
           :name_cgc       cgc_name
           :name_sequence  seq_name
           :name_public    pub_name
           :desc           (gene-desc-to-couch concise)
           :rnai           (seq (map gene-rnai-to-couch rnais)) ; `seq` replaces empty collection with `nil`
           :reference      refs)]))

(defmethod ace-to-couch "RNAi"
(co  [obj]
  (let [[[delivery]]        (select obj ["Experiment" "Delivered_by"])
        [[strain]]          (select obj ["Experiment" "Strain"])
        phenotypes          (map first (select obj ["Phenotype"]))
        not_phenotypes      (map first (select obj ["Phenotype_not_observed"]))
        refs                (map first (select obj ["Reference"]))]
    [(vmap :_id              (:id obj)
           :expt_strain     strain
           :expt_delivery   delivery
           :phenotype       (seq phenotypes)
           :not_phenotype   (seq not_phenotypes)
           :reference       (seq refs))]))

(defmethod ace-to-couch "Phenotype"
  [obj]
  (let [[[desc]]      (select obj ["Description"])
        [[name]]      (select obj ["Name" "Primary_name"])]
    [(vmap :_id           (:id obj)
           :name         name
           :description  desc)]))

(defmethod ace-to-couch :default
  [obj]
  [])   ; Make no assertions about AceDB objects we don't understand

(defn import-acefiles
  "Open all elements of seq `files` as .ace files.  Convert objects we understand into
  documents suitable for storage in CouchDB"
  [files]
  (binding [longtext-cache (atom {})]
    (doall
        (mapcat
         (fn [file]
           (->> (ace-reader file)
                (ace-seq)
                (mapcat ace-to-couch)))
         files))))
