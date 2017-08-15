(ns rest-api.classes.blast-hit
  (:require
   [compojure.api.sweet :as sweet]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [ring.util.http-response :as response]
   [schema.core :as schema]))

(def id-types [:transcript/id :cds/id :sequence/id :protein/id])

(defmulti convert-entity
  (fn [entity]
    (first (filter (partial contains? entity) id-types))))

;; wormpep
(defmethod convert-entity :transcript/id [entity]
  (let [transcript entity
        protein (some->> transcript
                         (:transcript/corresponding-cds)
                         (:transcript.corresponding-cds/cds)
                         (:cds/corresponding-protein)
                         (:cds.corresponding-protein/protein))
        gene (some->> transcript
                      (:gene.corresponding-transcript/_transcript)
                      (first)
                      (:gene/_corresponding-transcript))]
    {:gene (pack-obj "gene" gene :label "[Gene Summary]")
     :protein (pack-obj "protein" protein :label "[Protein Summary]")
     :sequence (pack-obj transcript)}))

;; wormpep
(defmethod convert-entity :cds/id [entity]
  (let [cds entity
        protein (some->> cds
                         (:cds/corresponding-protein)
                         (:cds.corresponding-protein/protein))
        gene (some->> cds
                      (:gene.corresponding-cds/_cds)
                      (first)
                      (:gene/_corresponding-cds))]
    {:gene (pack-obj "gene" gene :label "[Gene Summary]")
     :protein (pack-obj "protein" protein :label "[Protein Summary]")
     :sequence (pack-obj cds)}))

;; est
(defmethod convert-entity :sequence/id [entity]
  (let [sequence entity
        gene (some->> sequence
                      (:locatable/_parent)
                      (filter :gene/id)
                      (first))]
    {:corresponding_gene (pack-obj "gene" gene :label "[Corr. Gene]")
     :sequence (pack-obj sequence)}))

(defmethod convert-entity :default [entity]
  {:sequence (pack-obj entity)})

(def routes
  [(sweet/GET "/convert/:id" []
              :path-params [id :- (sweet/describe schema/Str "WB ID of a BLAST hit")]
              :no-doc true
              (let [db (d/db datomic-conn)]
                (if-let [entity (some #(d/entity db [% id]) id-types)]
                  (response/ok (convert-entity entity))
                  (response/not-found {:reason (format "No match found in %s" (clojure.string/join ", " id-types))}))))])
