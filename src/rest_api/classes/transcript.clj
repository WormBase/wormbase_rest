(ns rest-api.classes.transcript
  (:require
   [datomic.api :as d]
   [rest-api.routing :as routing]
   [rest-api.classes.gene.expression :as exp]))

(defn- provide-gene [gene-field-function]
  (fn [transcript]
    (let [db (d/entity-db transcript)]
      (if-let [gene-dbid (d/q exp/q-corresponding-transcript
                              db
                              (:db/id transcript))]
        (gene-field-function (d/entity db gene-dbid))))))

(def expression-widget
  {:name generic/name-field
   :expressed_in (provide-gene exp/expressed-in)
   :expressed_during (provide-gene exp/expressed-during)
   :subcellular_localization (provide-gene exp/subcellular-localization)
   :expression_profiling_graphs (provide-gene exp/expression-profiling-graphs)
   :expression_cluster (provide-gene exp/expression-cluster)
   :anatomy_function (provide-gene exp/anatomy-functions)
   :fourd_expression_movies (provide-gene exp/expression-movies)
   :epic_expr_patterns (provide-gene exp/epic-expr-patterns)})

(routing/defroutes
  {:entity-class "transcript"
   :widget
   {:expression expression-widget}
   :field
   {:fpkm_expression_summary_ls exp/fpkm-expression-summary-ls}})
