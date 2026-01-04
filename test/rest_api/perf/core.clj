(ns rest-api.perf.core
  "Main entry point for response size analysis."
  (:require
   [clojure.string :as str]
   [mount.core :as mount]
   [rest-api.perf.config :as config]
   [rest-api.perf.report :as report]
   [rest-api.perf.runner :as runner])
  (:gen-class))

(defn run-analysis
  "Run response size analysis with given options."
  [{:keys [config-path entity-type entity-id output-dir]
    :or {config-path "resources/perf-config.edn"
         output-dir "target/perf-reports"}}]
  (println "Starting response size analysis...")
  (println "Loading configuration from:" config-path)

  (mount/start)

  (let [config (or (config/load-config config-path) {})
        thresholds (merge config/default-thresholds (:thresholds config))
        results (cond
                  ;; Single entity test
                  (and entity-type entity-id)
                  (do
                    (println (format "Testing single entity: %s/%s" entity-type entity-id))
                    (case entity-type
                      "gene" (runner/run-single-entity-test
                              "gene" entity-id runner/gene-widgets)
                      "variation" (runner/run-single-entity-test
                                   "variation" entity-id runner/variation-widgets)
                      "protein" (runner/run-single-entity-test
                                 "protein" entity-id runner/protein-widgets)
                      "strain" (runner/run-single-entity-test
                                "strain" entity-id runner/strain-widgets)
                      (do
                        (println "Unknown entity type:" entity-type)
                        [])))

                  ;; Entity type only - use config IDs
                  entity-type
                  (let [entity-ids (config/get-entity-ids entity-type config 3)]
                    (println (format "Testing entity type: %s with IDs: %s"
                                     entity-type (str/join ", " entity-ids)))
                    (case entity-type
                      "gene" (runner/run-gene-tests entity-ids)
                      "variation" (runner/run-variation-tests entity-ids)
                      "protein" (runner/run-protein-tests entity-ids)
                      "strain" (runner/run-strain-tests entity-ids)
                      (do
                        (println "Unknown entity type:" entity-type)
                        [])))

                  ;; Full analysis
                  :else
                  (do
                    (println "Running full analysis...")
                    (runner/run-all-tests config)))]

    (let [results-vec (vec results)]
      ;; Console report
      (report/console-report results-vec thresholds)

      ;; File reports
      (let [timestamp (System/currentTimeMillis)
            edn-path (str output-dir "/size-analysis-" timestamp ".edn")
            json-path (str output-dir "/size-analysis-" timestamp ".json")]
        (report/write-edn-report results-vec edn-path)
        (report/write-json-report results-vec json-path)
        (println "\nReports written to:")
        (println "  EDN:" edn-path)
        (println "  JSON:" json-path))

      results-vec)))

(defn -main
  "CLI entry point."
  [& args]
  (let [opts (apply hash-map args)]
    (run-analysis
     {:config-path (get opts "--config" "resources/perf-config.edn")
      :entity-type (get opts "--entity")
      :entity-id (get opts "--id")
      :output-dir (get opts "--output" "target/perf-reports")}))
  (shutdown-agents))

;; REPL helpers
(defn analyze-gene
  "Quick helper to analyze a gene."
  [gene-id]
  (run-analysis {:entity-type "gene" :entity-id gene-id}))

(defn analyze-variation
  "Quick helper to analyze a variation."
  [var-id]
  (run-analysis {:entity-type "variation" :entity-id var-id}))

(defn analyze-all
  "Run full analysis with default config."
  []
  (run-analysis {}))

(comment
  ;; REPL usage examples:

  ;; Analyze a specific gene known to have large responses
  (analyze-gene "WBGene00006759")

  ;; Analyze variations
  (analyze-variation "WBVar00248722")

  ;; Run full analysis
  (analyze-all)

  ;; Custom analysis
  (run-analysis {:entity-type "gene"
                 :entity-id "WBGene00000001"})
  )
