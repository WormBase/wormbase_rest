(ns rest-api.classes.transcript
  (:require
   [datomic.api :as d]
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
  {:entity-class "transcript"
   :widget
   {:expression expression-widget}
   :field
   {:fpkm_expression_summary_ls exp/fpkm-expression-summary-ls}})
