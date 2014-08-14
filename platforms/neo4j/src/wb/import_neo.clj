(ns wb.import-neo
  (:use acedb.acefile)
  (:require [clojure.core.match :refer (match)]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as ne]))

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
(def ^:private ^:dynamic id-cache)
(def ^:private ^:dynamic links)

(defn- create-cached-node [conn props]
  (let [node (nn/create conn props)]
    (swap! id-cache assoc (:_id props) (:id node))
    node))

(defn- link [from to type props]
  (swap! links conj [from to type props]))

(defmulti ace-to-neo (fn [_ obj] (:class obj)))

(defmethod ace-to-neo "Paper"
  [con obj]
  (let [authors         (select obj ["Author"])
        [[ref_title]]   (select obj ["Reference" "Title"])
        [[ref_journal]] (select obj ["Reference" "Journal"])
        [[ref_volume]]  (select obj ["Reference" "Volume"])
        [[ref_page]]    (select obj ["Reference" "Page"])
        [[brief_cite]]  (select obj ["Brief_citation"])
        [[abstract]]    (select obj ["Abstract"])]
    (create-cached-node con
      (vmap :_id         (:id obj)
            :authors     (seq (map first authors))    ; Check cases where this is empty...
            :ref_title   ref_title
            :ref_journal ref_journal
            :ref_volume  ref_volume
            :ref_page    ref_page
            :brief_citation  (unescape brief_cite)
            :abstract    (@longtext-cache abstract)))))

(defn- evidence-to-neo [evseq]
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

(defmethod ace-to-neo "LongText"
  [_ obj]
  (swap! longtext-cache assoc (:id obj) (:text obj))
  [])


(defmethod ace-to-neo "Gene"
  [con obj]
  (let [[[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        refs                (map first (select obj ["Reference"]))
        gene                (create-cached-node con
                               (vmap :_id             (:id obj)
                                     :name_cgc       cgc_name
                                     :name_sequence  seq_name
                                     :name_public    pub_name))]
    (when (seq concise)
      (let [cdn (nn/create con {:concise (unescape (ffirst concise))})]
        (ne/create con gene cdn :desc (evidence-to-neo (map rest concise)))))
    (doseq [r refs]
      (link (:id obj) r :reference {}))
    (doseq [[r & evidence] rnais]
      (link (:id obj) r :rnai (evidence-to-neo [evidence])))))

(defmethod ace-to-neo "RNAi"
  [con obj]
  (let [[[delivery]]        (select obj ["Experiment" "Delivered_by"])
        [[strain]]          (select obj ["Experiment" "Strain"])
        phenotypes          (map first (select obj ["Phenotype"]))
        not_phenotypes      (map first (select obj ["Phenotype_not_observed"]))
        refs                (map first (select obj ["Reference"]))
        rnai                (create-cached-node con
                              (vmap :_id             (:id obj)
                                    :expt_strain     strain
                                    :expt_delivery   delivery))]
    (doseq [p phenotypes]
      (link (:id obj) p :phenotype-observed {}))
    (doseq [p not_phenotypes]
      (link (:id obj) p :phenotype-not-observed {}))
    (doseq [r refs]
      (link (:id obj) r :reference {}))))

(defmethod ace-to-neo "Phenotype"
  [con obj]
  (let [id            (:id obj)
        [[desc]]      (select obj ["Description"])
        [[name]]      (select obj ["Name" "Primary_name"])]
    (create-cached-node con
       (vmap :_id          id
             :name         name
             :description  desc))))

(defmethod ace-to-neo :default
  [con obj]
  [])   ; Make no assertions about AceDB objects we don't understand

(defn import-acefiles
  "Open all elements of seq `files` as .ace files.  Store objects we understand
  in the neocons connection con"
  [con files]
  (binding [longtext-cache (atom {})
            id-cache (atom {})
            links (atom [])]
    (doseq [file files]
      (println "Importing " file)
      (->> (ace-reader file)
           (ace-seq)
           (map (partial ace-to-neo con))
           (doall)))
    (println "Creating " (count @links) " links")
    (doseq [link-block (partition-all 100 @links)]
      ; It seems to be possible to "saturate" a Neo4J server to the
      ; point where it starts refusing requests (!).  The chunking here
      ; is a) an ugly non-solution and b) probably actually more extreme
      ; thank really needed, but it does the trick.
      
      (doseq [[from to type props] link-block]
        (try
          (if-let [from-node (nn/get con (@id-cache from))]
            (if-let [to-node (nn/get con (@id-cache to))]
              (ne/create con
                         from-node
                         to-node
                         type
                         props)))
          (catch Exception e
            (println "Error linking " from " -> " to)
            (println (@id-cache from))
            (println (@id-cache to))
            (throw e))))
      (Thread/sleep 1000))))
