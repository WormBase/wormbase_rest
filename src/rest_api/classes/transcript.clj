(ns rest-api.classes.transcript
  (:require
   [rest-api.classes.gene.widgets.external-links :as external-links]
   ;[rest-api.classes.transcript.widgets.overview :as overview]
   [rest-api.classes.transcript.widgets.location :as location]
   [rest-api.classes.transcript.widgets.reagents :as reagents]
   [rest-api.classes.transcript.widgets.feature :as feature]
   [rest-api.classes.transcript.widgets.references :as references]
   [rest-api.classes.gene.expression :as exp]
   [rest-api.formatters.object :as obj]
   [rest-api.routing :as routing]))

(defn- delegate-to-gene [gene-field-function]
  (fn [transcript]
    (if-let [gene (->> transcript
                       (:gene.corresponding-transcript/_transcript )
                       (map :gene/_corresponding-transcript)
                       (first))]
      (gene-field-function gene))))

(def expression-widget
  {:name obj/name-field
   :gene_name (delegate-to-gene obj/name-field)
   :expressed_in (delegate-to-gene exp/expressed-in)
   :expressed_during (delegate-to-gene exp/expressed-during)
   :subcellular_localization (delegate-to-gene exp/subcellular-localization)
   :epic_expr_patterns (delegate-to-gene exp/epic-expr-patterns)
   :expression_profiling_graphs (delegate-to-gene exp/expression-profiling-graphs)
   :expr_pattern_images (delegate-to-gene exp/expr-pattern-images)
   :expression_cluster (delegate-to-gene exp/expression-cluster)
   :anatomy_function (delegate-to-gene exp/anatomy-functions)
   :fourd_expression_movies (delegate-to-gene exp/expression-movies)
   :microarray_topology_map_position (delegate-to-gene exp/microarray-topology-map-position)})

(routing/defroutes
  {:entity-ns "transcript"
   :widget
   {;:overview overview/widget
    :reagents reagents/widget
    :expression expression-widget
    :external_links external-links/widget
    :location location/widget
    :feature feature/widget
    :references references/widget}
   :field
   {:fpkm_expression_summary_ls exp/fpkm-expression-summary-ls}})
