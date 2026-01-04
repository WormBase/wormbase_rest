(ns rest-api.perf.runner
  "Test execution engine for response size analysis."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [datomic.api :as d]
   [mount.core :as mount]
   [rest-api.db.main :refer [datomic-conn]]
   [rest-api.main :as main]
   [rest-api.perf.config :as config]
   [rest-api.perf.metrics :as metrics]
   [ring.mock.request :as mock]))

(defn ensure-started
  "Ensure mount state is started."
  []
  (when-not (mount/running-states)
    (mount/start)))

(defn make-request
  "Make a request through the Ring app and return response."
  [url]
  (let [request (mock/request :get url)
        response (main/app request)]
    response))

(defn parse-response
  "Parse JSON response body."
  [response]
  (when (= 200 (:status response))
    (try
      (-> response :body (json/parse-string true))
      (catch Exception _
        nil))))

(defn test-endpoint
  "Test a single endpoint and collect size metrics.
   Returns a map with endpoint info and metrics."
  [entity-ns scheme endpoint-name entity-id]
  (let [url (format "/rest/%s/%s/%s/%s"
                    (name scheme)
                    (str/replace entity-ns "-" "_")
                    entity-id
                    endpoint-name)
        start-time (System/nanoTime)
        response (make-request url)
        elapsed-ms (/ (- (System/nanoTime) start-time) 1e6)
        parsed (parse-response response)]
    (merge
     {:entity-ns entity-ns
      :scheme scheme
      :endpoint-name endpoint-name
      :entity-id entity-id
      :url url
      :status (:status response)
      :elapsed-ms elapsed-ms}
     (when parsed
       (let [analysis (metrics/analyze-response parsed)
             data-analysis (metrics/analyze-data-field parsed)]
         {:total-size-kb (:size-kb analysis)
          :total-size-bytes (:size-bytes analysis)
          :data-size-kb (:size-kb data-analysis)
          :data-size-bytes (:size-bytes data-analysis)
          :nesting-depth (:nesting-depth data-analysis)
          :entity-count (:entity-count data-analysis)
          :top-classes (:top-classes data-analysis)
          :large-collections (metrics/find-large-collections
                              (:data parsed)
                              :min-size 20)})))))

(defn test-entity-endpoints
  "Test all known widgets for an entity type with given entity IDs."
  [entity-ns entity-ids widget-names]
  (for [entity-id entity-ids
        widget-name widget-names]
    (test-endpoint entity-ns :widget widget-name entity-id)))

(def gene-widgets
  "Known gene widgets to test."
  ["overview" "expression" "external_links" "feature" "genetics"
   "history" "homology" "human_diseases" "location" "mapping_data"
   "gene_ontology" "phenotype" "phenotype_graph" "reagents"
   "references" "sequences" "biocyc" "interactions"])

(def gene-fields
  "Known gene fields to test."
  ["alleles_other" "polymorphisms" "interaction_details"])

(def variation-widgets
  ["overview" "genetics" "human_diseases" "isolation"
   "molecular_details" "phenotypes" "location" "external_links"
   "references"])

(def protein-widgets
  ["overview" "location" "sequences" "motif_details" "homology"
   "blast_details" "history" "external_links"])

(def strain-widgets
  ["overview" "contains" "human_diseases" "isolation"
   "natural_isolates" "origin" "phenotypes" "references"])

(defn run-gene-tests
  "Run size analysis on gene endpoints."
  [entity-ids]
  (ensure-started)
  (concat
   (test-entity-endpoints "gene" entity-ids gene-widgets)
   (for [entity-id entity-ids
         field-name gene-fields]
     (test-endpoint "gene" :field field-name entity-id))))

(defn run-variation-tests
  "Run size analysis on variation endpoints."
  [entity-ids]
  (ensure-started)
  (test-entity-endpoints "variation" entity-ids variation-widgets))

(defn run-protein-tests
  "Run size analysis on protein endpoints."
  [entity-ids]
  (ensure-started)
  (test-entity-endpoints "protein" entity-ids protein-widgets))

(defn run-strain-tests
  "Run size analysis on strain endpoints."
  [entity-ids]
  (ensure-started)
  (test-entity-endpoints "strain" entity-ids strain-widgets))

(defn run-all-tests
  "Run size analysis on all configured entities."
  [config]
  (ensure-started)
  (let [gene-ids (config/get-entity-ids "gene" config 3)
        variation-ids (config/get-entity-ids "variation" config 3)
        protein-ids (config/get-entity-ids "protein" config 2)
        strain-ids (config/get-entity-ids "strain" config 2)]
    (concat
     (run-gene-tests gene-ids)
     (run-variation-tests variation-ids)
     (run-protein-tests protein-ids)
     (run-strain-tests strain-ids))))

(defn run-single-entity-test
  "Run size analysis on a single entity type."
  [entity-ns entity-id widgets]
  (ensure-started)
  (doall
   (for [widget widgets]
     (test-endpoint entity-ns :widget widget entity-id))))
