(ns rest-api.perf.report
  "Report generation for response size analysis."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(defn format-kb
  "Format KB value with 2 decimal places."
  [kb]
  (format "%.2f KB" (double kb)))

(defn format-ms
  "Format milliseconds with 2 decimal places."
  [ms]
  (format "%.2f ms" (double ms)))

(defn successful-results
  "Filter for successful (status 200) results."
  [results]
  (filter #(= 200 (:status %)) results))

(defn generate-summary
  "Generate summary statistics from results."
  [results]
  (let [successful (successful-results results)
        sizes (map :total-size-kb successful)
        data-sizes (map :data-size-kb successful)
        times (map :elapsed-ms successful)]
    {:total-tested (count results)
     :successful (count successful)
     :failed (- (count results) (count successful))
     :avg-total-size-kb (when (seq sizes) (/ (reduce + sizes) (count sizes)))
     :max-total-size-kb (when (seq sizes) (apply max sizes))
     :avg-data-size-kb (when (seq data-sizes) (/ (reduce + data-sizes) (count data-sizes)))
     :max-data-size-kb (when (seq data-sizes) (apply max data-sizes))
     :avg-response-ms (when (seq times) (/ (reduce + times) (count times)))
     :max-response-ms (when (seq times) (apply max times))}))

(defn find-large-responses
  "Find responses exceeding size threshold."
  [results threshold-kb]
  (->> results
       successful-results
       (filter #(> (:total-size-kb %) threshold-kb))
       (sort-by :total-size-kb >)))

(defn find-high-entity-counts
  "Find responses with many entity references."
  [results threshold]
  (->> results
       successful-results
       (filter #(> (:entity-count %) threshold))
       (sort-by :entity-count >)))

(defn find-deeply-nested
  "Find responses with deep nesting."
  [results threshold]
  (->> results
       successful-results
       (filter #(> (:nesting-depth %) threshold))
       (sort-by :nesting-depth >)))

(defn group-by-entity
  "Group results by entity type."
  [results]
  (group-by :entity-ns results))

(defn group-by-endpoint
  "Group results by endpoint name."
  [results]
  (group-by :endpoint-name results))

(defn console-report
  "Print formatted console report."
  [results thresholds]
  (let [summary (generate-summary results)
        large (find-large-responses results (:large-response-kb thresholds 500))
        warning (find-large-responses results (:warning-response-kb thresholds 100))
        high-entities (find-high-entity-counts results (:high-entity-count thresholds 100))
        deep (find-deeply-nested results (:deep-nesting-level thresholds 10))]

    (println "\n" (str/join (repeat 60 "=")) "\n")
    (println "       WORMBASE REST API - RESPONSE SIZE ANALYSIS")
    (println "\n" (str/join (repeat 60 "=")) "\n")

    (println "SUMMARY")
    (println (str "  Endpoints tested: " (:total-tested summary)))
    (println (str "  Successful: " (:successful summary)))
    (println (str "  Failed: " (:failed summary)))
    (when (:avg-total-size-kb summary)
      (println (str "  Avg response size: " (format-kb (:avg-total-size-kb summary))))
      (println (str "  Max response size: " (format-kb (:max-total-size-kb summary)))))
    (when (:avg-data-size-kb summary)
      (println (str "  Avg data size: " (format-kb (:avg-data-size-kb summary))))
      (println (str "  Max data size: " (format-kb (:max-data-size-kb summary)))))
    (when (:avg-response-ms summary)
      (println (str "  Avg response time: " (format-ms (:avg-response-ms summary))))
      (println (str "  Max response time: " (format-ms (:max-response-ms summary)))))

    (when (seq large)
      (println (str "\n" (str/join (repeat 60 "-"))))
      (println (str "LARGE RESPONSES (>" (:large-response-kb thresholds 500) " KB)"))
      (println (str (str/join (repeat 60 "-"))))
      (doseq [r (take 15 large)]
        (println (format "  %s/%s/%s"
                         (:entity-ns r) (:endpoint-name r) (:entity-id r)))
        (println (format "    Size: %s | Entities: %d | Depth: %d"
                         (format-kb (:total-size-kb r))
                         (:entity-count r)
                         (:nesting-depth r)))
        (when (seq (:top-classes r))
          (println (format "    Top classes: %s"
                           (str/join ", "
                                     (for [[cls cnt] (:top-classes r)]
                                       (str cls ":" cnt))))))
        (when (seq (:large-collections r))
          (println (format "    Large collections: %s"
                           (str/join ", "
                                     (for [{:keys [path count]} (take 3 (:large-collections r))]
                                       (str (last path) ":" count))))))))

    (when (and (seq warning) (empty? large))
      (println (str "\n" (str/join (repeat 60 "-"))))
      (println (str "WARNING SIZE RESPONSES (>" (:warning-response-kb thresholds 100) " KB)"))
      (println (str (str/join (repeat 60 "-"))))
      (doseq [r (take 10 warning)]
        (println (format "  %s/%s/%s: %s"
                         (:entity-ns r) (:endpoint-name r) (:entity-id r)
                         (format-kb (:total-size-kb r))))))

    (when (seq high-entities)
      (println (str "\n" (str/join (repeat 60 "-"))))
      (println (str "HIGH ENTITY COUNT (>" (:high-entity-count thresholds 100) ")"))
      (println (str (str/join (repeat 60 "-"))))
      (doseq [r (take 10 high-entities)]
        (println (format "  %s/%s/%s: %d entities"
                         (:entity-ns r) (:endpoint-name r) (:entity-id r)
                         (:entity-count r)))))

    (when (seq deep)
      (println (str "\n" (str/join (repeat 60 "-"))))
      (println (str "DEEPLY NESTED (depth >" (:deep-nesting-level thresholds 10) ")"))
      (println (str (str/join (repeat 60 "-"))))
      (doseq [r (take 10 deep)]
        (println (format "  %s/%s/%s: depth %d"
                         (:entity-ns r) (:endpoint-name r) (:entity-id r)
                         (:nesting-depth r)))))

    (println (str "\n" (str/join (repeat 60 "=")) "\n"))))

(defn write-edn-report
  "Write detailed report to EDN file."
  [results path]
  (io/make-parents path)
  (spit path (with-out-str (pprint/pprint
                            {:timestamp (str (java.util.Date.))
                             :summary (generate-summary results)
                             :results results}))))

(defn write-json-report
  "Write detailed report to JSON file."
  [results path]
  (io/make-parents path)
  (spit path (json/generate-string
              {:timestamp (str (java.util.Date.))
               :summary (generate-summary results)
               :results results}
              {:pretty true})))
