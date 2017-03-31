(ns rest-api.classes.gene.widgets.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [rest-api.classes.generic :as generic]
   [rest-api.classes.gene.expression :as expression]))

(def widget
  {:name generic/name-field
   :expressed_in expression/expressed-in
   :expressed_during expression/expressed-during
   :subcellular_localization expression/subcellular-localization
   :epic_expr_patterns expression/epic-expr-patterns
   :expression_profiling_graphs expression/expression-profiling-graphs
   :expr_pattern_images expression/expr-pattern-images
   :expression_cluster expression/expression-cluster
   :anatomy_function expression/anatomy-functions
   :fourd_expression_movies expression/expression-movies
   :microarray_topology_map_position expression/microarray-topology-map-position})
