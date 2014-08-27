(ns wb.import-orient-tinker
  (:use acetyl.parser)
  (:require [clojure.core.match :refer (match)]
            [archimedes.core :as g]
            [archimedes.vertex :as v]
            [archimedes.io :as io]
            [archimedes.edge :as e]))

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

; from old contrib.seq
(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

(def ^:private ^:dynamic longtext-cache)
(def ^:private ^:dynamic id-cache)
(def ^:private ^:dynamic links)

(defn- create-cached! [props]
  (let [node (v/create! props)]
    (swap! id-cache assoc (:_id props) node)
    node))

(defn- link [from to type props]
  (swap! links conj [from to type props]))

(defmulti import-obj :class)

(defn- import-evidence [evseq]
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

(defmethod import-obj "LongText"
  [obj]
  (swap! longtext-cache assoc (:id obj) (:text obj))
  [])

(defmethod import-obj "Paper" [obj]
  (let [authors         (select obj ["Author"])
        [[ref_title]]   (select obj ["Reference" "Title"])
        [[ref_journal]] (select obj ["Reference" "Journal"])
        [[ref_volume]]  (select obj ["Reference" "Volume"])
        [[ref_page]]    (select obj ["Reference" "Page"])
        [[brief_cite]]  (select obj ["Brief_citation"])
        [[abstract]]    (select obj ["Abstract"])
    
        p (create-cached!
            (vmap :_id         (:id obj)
                  :ref_title   ref_title
                  :ref_journal ref_journal
                  :ref_volume  ref_volume
                  :ref_page    ref_page
                  :brief_citation  (unescape brief_cite)
                  :abstract    (@longtext-cache abstract)))]
    (doseq [[i [a]] (indexed authors)]
      (let [anode (v/create! {:index i
                              :name  a})]
        (e/connect-with-id! nil p :author anode)))))

(defmethod import-obj "Gene" [obj]
  (let [[[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        refs                (map first (select obj ["Reference"]))
        gene                (create-cached!
                               (vmap :_id             (:id obj)
                                     :name_cgc       cgc_name
                                     :name_sequence  seq_name
                                     :name_public    pub_name))]
    (when (seq concise)
      (let [cdn (v/create! {:concise (unescape (ffirst concise))})]
        (e/connect-with-id! nil gene :desc cdn (import-evidence (map rest concise)))))
    (doseq [r refs]
      (link (:id obj) r :reference {}))
    (doseq [[r & evidence] rnais]
      (link (:id obj) r :rnai (import-evidence [evidence])))))

(defmethod import-obj "RNAi" [obj]
  (let [[[delivery]]        (select obj ["Experiment" "Delivered_by"])
        [[strain]]          (select obj ["Experiment" "Strain"])
        phenotypes          (map first (select obj ["Phenotype"]))
        not_phenotypes      (map first (select obj ["Phenotype_not_observed"]))
        refs                (map first (select obj ["Reference"]))
        rnai                (create-cached!
                              (vmap :_id             (:id obj)
                                    :expt_strain     strain
                                    :expt_delivery   delivery))]
    (doseq [p phenotypes]
      (link (:id obj) p :phenotype-observed {}))
    (doseq [p not_phenotypes]
      (link (:id obj) p :phenotype-not-observed {}))
    (doseq [r refs]
      (link (:id obj) r :reference {}))))

(defmethod import-obj "Phenotype" [obj]
  (let [id            (:id obj)
        [[desc]]      (select obj ["Description"])
        [[name]]      (select obj ["Name" "Primary_name"])]
    (create-cached!
       (vmap :_id          id
             :name         name
             :description  desc))))

(defmethod import-obj :default
  [con])   ; Make no assertions about AceDB objects we don't understand

(defn import-acefiles
  "Open all elements of seq `files` as .ace files.  Store objects we understand
  in the open Archimedes transaction"
  [files]
  (binding [longtext-cache (atom {})
            id-cache (atom {})
            links (atom [])]
    (g/transact!
     (doseq [file files]
       (println "Importing " file)
       (->> (ace-reader file)
            (ace-seq)
            (map import-obj)
            (doall))))
    (println "Creating " (count @links) " deferred links")
    (doseq [link-block (partition-all 100 @links)]
      (g/transact!
       (doseq [[from to type props] link-block]
         (try
           (if-let [from-node (@id-cache from)]
             (if-let [to-node (@id-cache to)]
               (e/connect-with-id! nil
                                   from-node
                                   type
                                   to-node
                                   props)))))))))
