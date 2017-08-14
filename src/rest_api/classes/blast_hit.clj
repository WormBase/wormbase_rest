(ns rest-api.classes.blast-hit
  (:require
   [compojure.api.sweet :as sweet]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [ring.util.http-response :as response]
   [schema.core :as schema]))


(defn- convert-id [id]
  (let [db (d/db datomic-conn)]
    (or (if-let [transcript (d/entity db [:transcript/id id])]
          (let [protein (some->> transcript
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
        )))

(def routes
  [(sweet/GET "/convert/:id" []
              :path-params [id :- (sweet/describe schema/Str "WB ID of a BLAST hit")]
;;              :no-doc true
              (response/ok (convert-id id)))])
