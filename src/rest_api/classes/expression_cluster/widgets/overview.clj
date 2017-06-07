(ns rest-api.classes.expression-cluster.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn algorithm [ec]
  {:data (first (:expression-cluster/algorithm ec))
   :description "Algorithm used to determine cluster"})

(defn attribute-of [ec]
  {:data  (not-empty
            (into
              {}
              (remove
                nil?
                (conj
                (when-let [mes (:expression-cluster/microarray-experiment ec)]
                  {:Microarray_experiment (for [me mes] (pack-obj me))})
                (when-let [mss (:expression-cluster/mass-spectrometry ec)]
                  {:Mass_spectometry (for [ms mss] (pack-obj ms))})
                (when-let [rs (:expression-cluster/rnaseq ec)]
                  {:RNASeq (for [r rs] (pack-obj r))})
                (when-let [tas (:expression-cluster/tiling-array ec)]
                  {:Tiling_array (for [ta tas] (pack-obj ta))})
                (when-let [qs (:expression-cluster/qpcr ec)]
                  {:qPCR (for [q qs] (pack-obj q))})
                (when-let [hs (:expression-cluster/expr-pattern ec)]
                  {:Expr_pattern (for [h hs
                                       :let [ep (:expression-cluster.expr-pattern/expr-pattern h)]]
                                   (pack-obj ep))})))))
   :description "Items attributed to this expression cluster"})

(def widget
  {:name generic/name-field
   :algorithm algorithm
   :taxonomy generic/taxonomy
   :remarks generic/remarks
   :attribute_of attribute-of
   :description generic/description})
