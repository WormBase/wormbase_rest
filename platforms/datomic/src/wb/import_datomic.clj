(ns wb.import-datomic
  (:use acedb.acefile wb.utils)
  (:require [clojure.core.match :refer (match)]
            [clojure.string :as str]
            [datomic.api :as d]))

;
; "version 2" Datomic importer, for more credible ACeDB models.
;

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

(defn- evidence-to-datomic [evseq]
  (conj-into {}
    (for [e evseq]
      (match (vec e)
        []                             nil   ; For now, no complains about missing evidence.
        ["Inferred_automatically" s]   [:evidence/automatic s]
        ["Paper_evidence" p]           [:evidence/paper {:paper/id p}]
        ["Person_evidence" s]          [:evidence/person {:person/id s}]
        ["Curator_confirmed" s]        [:evidence/curator {:person/id s}]
        ["Author_evidence" s text]     [:evidence/author
                                        {:evidence.author/author {:thing/id s}
                                         :evidence.author/note   text}]
        ["Accession_evidence" db id]   [:evidence/accession
                                        {:evidence.accession/database {:thing/id db}
                                         :evidence.accession/accession id}]
        ["Protein_id_evidence" s]      [:evidence/protein-id]
        ["GO_term_evidence" g]         [:evidence/go-term {:go/id g}]
        ["Expr_pattern_evidence" e]    [:evidence/expr-patern {:thing/id e}]
        ["Microarray_results_evidence"
         m]                            [:evidence/microarray-results {:thing/id m}]
        ["RNAi_evidence" s]            [:evidence/rnai {:rnai/id s}]
        ["CGC_data_submission"]        [:evidence/cgc-submission true]
        ["Curator_confirmed" p]        [:evidence/curator {:person/id p}]
        ["Feature_evidence" f]         [:evidence/feature {:feature/id f}]
        ["Laboratory_evidence" l]      [:evidence/laboratory {:laboratory/id l}]
        ["From_analysis" a]            [:evidence/analysis {:analysis/id a}]
        ["Variation_evidence" v]       [:evidence/variation {:variation/id v}]
        ["Mass_spec_evidence" m]       [:evidence/mass-spec {:thing/id m}]
        ["Sequence_evidence" s]        [:evidence/sequence {:sequence/id s}]
        ["Remark" r]                   [:evidence/remark r]
        ["Date_last_updated" _]        nil    ; This should be handled by transactions...
        :else (throw (Exception. (str "Don't understand evidence " e)))))))
  

(defn- gene-rnai-to-datomic [[rnai & evidence]]
  (assoc (evidence-to-datomic [evidence])
         :gene.rnai/rnai {:rnai/id rnai}))

(defn- gene-desc-to-datomic [concise]
  (when (seq concise)
    (assoc (evidence-to-datomic (map rest concise))
      :gene.desc/concise (unescape (ffirst concise)))))

(defn- gene-status-to-datomic [status]
  (assoc (evidence-to-datomic (map rest status))
    :gene.status/status ({"Live"        :gene.status.status/live
                          "Suppressed"  :gene.status.status/suppressed
                          "Dead"        :gene.status.status/dead} (ffirst status))))

(defn- laboratory-to-datomic [lab]
  (when lab
    {:db/id           (d/tempid :db.part/user)
     :laboratory/id   lab}))


(defn- holder [key value]
  (when value
    {:db/id   (d/tempid :db.part/user)
     key      value}))

(defmethod ace-to-datomic "Gene"
  [obj]
  (let [[evidence]          (select obj ["Evidence"])
        [[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        [[mol_name]]        (select obj ["Identity" "Name" "Molecular_name"])
        other-names         (select obj ["Identity" "Name" "Other_name"])
        status              (select obj ["Identity" "Status"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        [[gene-class]]      (select obj ["Gene_info" "Gene_class"])
        [[laboratory]]      (select obj ["Gene_info" "Laboratory"])
        refs                (map first (select obj ["Reference"]))]
    [(vmap :db/id                   (d/tempid :db.part/user)
           :gene/id                 (:id obj)
           :gene/name.cgc           cgc_name
           :gene/name.sequence      seq_name
           :gene/name.public        pub_name
           :gene/name.molecular     mol_name
           :gene/name.other         (seq (map first other-names))

           :gene/status             (gene-status-to-datomic status)

           :gene/class              (holder :thing/id gene-class)
           :gene/laboratory         (laboratory-to-datomic laboratory)
           
           :gene/desc               (gene-desc-to-datomic concise)
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
