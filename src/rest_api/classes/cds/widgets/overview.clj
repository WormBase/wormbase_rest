(ns rest-api.classes.cds.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn taxonomy [cds]
  {:data (if-let [species (:species/id (:cds/species cds))]
           (let [[genus species] (str/split species #" ")]
             {:genus genus
              :species species}))
   :description "the genus and species of the current object"})

(defn sequence-type [cds]
  {:data {:data nil ; for C10F3.5a this is "Wormbase CDS" - I can't figure out how to get this string
          :description "the general type of the sequence"}
   :description "the general type of the sequence"})

(defn description [cds] ; I can't find any in datomic
  {:data (:cds.detailed-description/text (first (:cds/detailed-description cds)))
   :description (str "description of the CDS " (:cds/id cds))})

(defn partial-field [cds]
  {:data (cond
           (and
             (contains? cds :cds/start-not-found)
             (contains? cds :cds/end-not-found))
           "start and end not found"

           (contains? cds :cds/start-not-found)
           "start not found"

           (contains? cds :cds/end-not-found)
           "end not found"

           :else
           nil)
   :description "Whether the start or end of the CDS is found"})

(defn identity-field [cds]
  {:data (if-let [ident (:cds/brief-identification cds)]
           {:text (:cds.brief-identification/text ident)
            :evidence (obj/get-evidence ident)})
   :description "Brief description of the WormBase CDS"})

(defn remarks [cds]
  {:data (if-let [remarks (concat (:cds/db-remark cds) (:cds/remark cds))]
           (filter
             some?
             (for [remark remarks]
               {:text (or (:cds.db-remark/text remark)
                          (:cds.remark/text remark))
                :evidence (obj/get-evidence remark)})))
   :description "curatorial remarks for the CDS"})

(defn method [cds]
  {:data (:method/id (:locatable/method cds))
   :description "the method used to describe the CDS"})

(defn corresponding-all [cds] ; still trying to figure out
  {:data (if-let [holders  (:transcript.corresponding-cds/_cds cds)]
           (for [holder holders
                 :let [transcript (:transcript/_corresponding-cds holder)]]
             (let [length-unspliced (if (contains? transcript :transcript/corresponding-cds)
                                    (let [ccds (:transcript.corresponding-cds/cds
                                                 (:transcript/corresponding-cds transcript))]
                                      (- (:locatable/max ccds)
                                         (:locatable/min ccds)))
                                    "-")
                   length (let [hs (:transcript/source-exons transcript)]
                            (reduce +
                                    (for [h hs]
                                      (- (:transcript.source-exons/max h)
                                         (:transcript.source-exons/min h)))))
                   length-spliced (if (nil? length)
                                     "-</br>"
                                     (str length "</br>"))]
               {:length_unspliced length-unspliced
                :model {:style 0
                        :id (:transcript/id transcript)
                        :label (:transcript/id transcript)
                        :class "transcript"
                        :taxonomy (if-let [id (:species/id (:transcript/species transcript))]
                                    (generic/xform-species-name id))}
                :cds (if (contains? transcript :transcript/corresponding-cds)
                       (let [ccds (:transcript.corresponding-cds/cds
                                    (:transcript/corresponding-cds transcript))]
                         {:text
                          {:style "font-weight:bold"
                           :id (:cds/id ccds)
                           :label (:cds/id ccds)
                           :class "cds"
                           :taxonomy (generic/xform-species-name (:species/id (:transcript/species transcript)))}
                          :evidence {:status (name (:cds/prediction-status ccds))}})
                       "(no CDS)")
                :gene (if-let [gh (:gene.corresponding-transcript/_transcript transcript)]
                        (pack-obj (:gene/_corresponding-transcript (first gh))))
;                :dbid (:db/id transcript)
                :length_protein (if-let [ph (:transcript/corresponding-protein transcript)]
                                  (:protein.peptide/length
                                    (:protein/peptide
                                      (:cds.corresponding-protein/protein ph))))
                :protein (if-let [ph (:transcript/corresponding-protein transcript)]
                           (pack-obj (:transcript.corresponding-protein/protein ph)))
                :length_spliced length-spliced
                :keys (keys transcript)
                :dbid (:db/id transcript)
                :cdsdbid (:db/id cds)
                :type (if-let [type-field (:method/id (:locatable/method transcript))]
                        (str/replace type-field #"_" " "))
    })))
   :description "corresponding cds, transcripts, gene for this protein"})


(def widget
  {:name generic/name-field
   :taxonomy taxonomy
   :sequence_type sequence-type
   :description description
   :partial partial-field
   :identity identity-field
   :remarks remarks
   :method method
   :corresponding_all corresponding-all})
