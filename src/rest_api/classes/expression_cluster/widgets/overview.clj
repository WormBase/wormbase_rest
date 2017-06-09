(ns rest-api.classes.expression-cluster.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn algorithm [ec]
  {:data (first (:expression-cluster/algorithm ec))
   :description "Algorithm used to determine cluster"})

(defn- create-attribute-list [objects]
  (vals
    (into
      (sorted-map)
      (into {}
	    (for [object objects]
	      (let [id-kw (first (filter #(= (name %) "id") (keys object)))]
		{(id-kw object)
		 (pack-obj object)}))))))

(defn attribute-of [ec]
  {:data  (not-empty
            (into
              {}
              (remove
                nil?
                (conj
                  (when-let [mes (:expression-cluster/microarray-experiment ec)]
                    {:Microarray_experiment (create-attribute-list mes)})
                (when-let [mss (:expression-cluster/mass-spectrometry ec)]
                  {:Mass_spectometry (create-attribute-list mss)})
                (when-let [rs (:expression-cluster/rnaseq ec)]
                  {:RNASeq (create-attribute-list rs)})
                (when-let [tas (:expression-cluster/tiling-array ec)]
                  {:Tiling_array (create-attribute-list tas)})
                (when-let [qs (:expression-cluster/qpcr ec)]
                  {:qPCR (create-attribute-list qs)})
                (when-let [hs (:expression-cluster/expr-pattern ec)]
		  {:Expr_pattern (vals
				   (into
				     (sorted-map)
				     (into {}
					   (for [h hs
						 :let [ep (:expression-cluster.expr-pattern/expr-pattern h)]]
					     {(:expression-pattern/id ep)
					      (pack-obj ep)}))))})))))
   :description "Items attributed to this expression cluster"})

(def widget
  {:name generic/name-field
   :algorithm algorithm
   :taxonomy generic/taxonomy
   :remarks generic/remarks
   :attribute_of attribute-of
   :description generic/description})
