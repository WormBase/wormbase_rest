(ns rest-api.classes.clone.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


(defn canonical-for [clone]
  {:data nil
   :description "clones that the requested clone is a canonical representative of"})

(defn maps [clone] ; example B0272
  {:data (if-let [m (:clone.map/map (first (:clone/map clone)))]
           (pack-obj m))
   :description "maps assigned to this clone"})

(defn sequence-status [clone]
  {:data (if-let [sequences (:sequence/_clone clone)]
           {:Finished (if-let [date-str (:sequence/finished (first sequences))]
            (date/format-date5 date-str))
            :Accession_number (:clone/accession-number clone)})
   :description "sequencing status of clone"})

(defn canonical-parent [clone]
  {:data nil
   :description "canonical parent for clone"})

(defn screened-negative [clone]
  {:data nil
   :description "entities shown to NOT be contained within the requested clone"})

(defn url [clone]
  {:data nil
   :description "The website for this clone"})

(defn remarks [clone]
  {:data (if-let [remarks (:clone/general-remark clone)]
           (for [remark remarks]
             {:text remark}))
   :description "Remarks"})

(defn lengths [clone]
  {:data nil
   :description "lengths relevant to this clone"})

(defn sequences [clone]; example AB070577
  {:data (if-let [sequences (:sequence/_clone clone)]
           (for [s sequences] (pack-obj s)))
   :description "sequences associated with this clone"})

(defn in-strain [clone]
  {:data nil
   :description "The current clone is found in this strain"})

(defn pcr-product [clone]
  {:data (if-let [pcrs (:pcr-product/_clone clone)]
            (let [pcr (first pcrs)]
           {:pcr_prodcut (pack-obj pcr)
            :oligo (if-let [ohs (:pcr-product/oligo pcr)]
                     (for [oh ohs :let [oligo
                       (:pcr-product.oligo/oligo oh)]]
                       {:obj {:id (:oligo/id oligo)  ; not using pack object because of custom class name
                              :label (:oligo/id oligo)
                              :class "pcr_oligo"
                              :taxonomy "all"}
                        :sequence (:oligo/sequence oligo)}))}))
   :description "PCR product associated with this clone"})

(defn gridded-on [clone]
  {:data nil
   :description "grid this clone was gridded on during fingerprinting"})

(defn taxonomy [clone]
  {:data (if-let [gspecies (:species/id (:clone/species clone))]
           (let [[genus species] (str/split gspecies #" ")]
             {:genus genus
              :species species}))
   :description "the genus and species of the current object"})

(defn genomic-position [clone]
  {:data nil ; sjj_F45D11.l has a genomic posistion
   :description "The genomic location of the sequence"})

(defn type-field [clone]
  {:data (if-let [t (:clone/type clone)]
           (let [n (name (:clone.type/value t))]
             (case n
               "cdna"
               "cDNA"
               (str/capitalize n))))
   :description "The type of this clone"})

(defn screened-position [clone]
  {:data (:db/id clone)
   :description "entities shown to be contained within this clone"})

(defn expression-patterns [clone]
  {:data (keys clone)
   :description (str "expression patterns associated with the Clone: " (:clone/id clone))})

(def widget
  {:canonical_for canonical-for
   :maps maps
   :sequence_status sequence-status
   :canonical_parent canonical-parent
   :screened_negative screened-negative
   :url url
   :remarks remarks
   :lengths lengths
   :sequences sequences
   :in_strain in-strain
   :pcr_product pcr-product
   :gridded_on gridded-on
   :name generic/name-field
   :taxonomy taxonomy
   :genomic_position genomic-position
   :type type-field
   :screened_position screened-position
   :expression_patterns expression-patterns})
