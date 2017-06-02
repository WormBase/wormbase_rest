(ns rest-api.classes.cds.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-type [cds]
  {:data {:data nil ; for C10F3.5a this is "Wormbase CDS" - I can't figure out how to get this string
          :description "the general type of the sequence"}
   :description "the general type of the sequence"})

(defn description [cds]
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
                                    (generic-functions/xform-species-name id))}
                :cds (if (contains? transcript :transcript/corresponding-cds)
                       (let [ccds (:transcript.corresponding-cds/cds
                                    (:transcript/corresponding-cds transcript))]
                         {:text
                          {:style "font-weight:bold"
                           :id (:cds/id ccds)
                           :label (:cds/id ccds)
                           :class "cds"
                           :taxonomy (generic-functions/xform-species-name (:species/id (:transcript/species transcript)))}
                          :evidence {:status (name (:cds/prediction-status ccds))}})
                       "(no CDS)")
                :gene (if-let [gh (:gene.corresponding-transcript/_transcript transcript)]
                        (pack-obj (:gene/_corresponding-transcript (first gh))))
                :length_protein (if-let [ph (:transcript/corresponding-protein transcript)]
                                  (:protein.peptide/length
                                    (:protein/peptide
                                      (:cds.corresponding-protein/protein ph))))
                :protein (if-let [ph (:transcript/corresponding-protein transcript)]
                           (pack-obj (:transcript.corresponding-protein/protein ph)))
                :length_spliced length-spliced
                :type (if-let [type-field (:method/id (:locatable/method transcript))]
                        (str/replace type-field #"_" " "))})))
   :description "corresponding cds, transcripts, gene for this protein"})


(def widget
  {:name generic/name-field
   :taxonomy generic/taxonomy
   :sequence_type sequence-type
   :description description
   :partial partial-field
   :identity generic/identity-field
   :remarks generic/remarks
   :method generic/method
   :corresponding_all corresponding-all})
