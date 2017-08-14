(ns rest-api.classes.blast-hit
  (:require
   [compojure.api.sweet :as sweet]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [ring.util.http-response :as response]
   [schema.core :as schema]))

(def id-types [:transcript/id :cds/id :sequence/id :protein/id])

(defmulti convert-id
  (fn [id]
    (let [db (d/db datomic-conn)]
      (->> id-types
           (filter #(d/entity db [% id]))
           (first)))))

(defmethod convert-id :transcript/id [id]
  (let [db (d/db datomic-conn)
        transcript (d/entity db [:transcript/id id])
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

(defmethod convert-id :cds/id [id]
  (let [db (d/db datomic-conn)
        cds (d/entity db [:cds/id id])
        protein (some->> cds
                         (:cds/corresponding-protein)
                         (:cds.corresponding-protein/protein))
        gene (some->> cds
                      (:gene.corresponding-cds/_cds)
                      (first)
                      (:gene/_corresponding-cds))]
    (prn gene)
    {:gene (pack-obj "gene" gene :label "[Gene Summary]")
     :protein (pack-obj "protein" protein :label "[Protein Summary]")
     :sequence (pack-obj cds)}))

(defmethod convert-id :sequence/id [id]
  (let [db (d/db datomic-conn)
        sequence (d/entity db [:sequence/id id])
        gene (some->> sequence
                      (:locatable/_parent)
                      (filter :gene/id)
                      (first))]
    {:corresponding_gene (pack-obj "gene" gene :label "[Corr. Gene]")
     :sequence (pack-obj sequence)}))

(defmethod convert-id nil [id]
  {:sequence {:label id}})

(defmethod convert-id :default [id]
  (let [db (d/db datomic-conn)
        sequence (some #(d/entity db [% id]) id-types)]
    {:sequence (pack-obj sequence)}))



(def routes
  [(sweet/GET "/convert/:id" []
              :path-params [id :- (sweet/describe schema/Str "WB ID of a BLAST hit")]
;;              :no-doc true
              (response/ok (convert-id id)))])
