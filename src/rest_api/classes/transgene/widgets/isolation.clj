(ns rest-api.classes.transgene.widgets.isolation
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn integration-method [tg]
  {:data (obj/humanize-ident (:transgene/integration-method tg))
   :description "how the transgene was integrated (if it has been)"})

(defn author [tg]
  {:data nil ; currently there are no authors attached to transgene
   :description "the person who created the transgene"})

(defn construct [tg]
  {:data (when-let [constructs (:construct/_transgene-construct tg)]
           (flatten
             (for [construct constructs]
               [(pack-obj construct)
                (:construct.summary/text
                  (:construct/summary construct))])))
   :description "gene that drives the transgene"})

(defn coinjection-marker [tg]
  {:data (when-let [constructs (:construct/_transgene-coinjection tg)]
           (flatten
             (for [construct constructs]
               [(pack-obj construct)
                (:construct.summary/text
                  (:construct/summary construct))])))
   :description "Coinjection marker for this transgene"})

(defn recombination-site [tg]
  {:data (when-let [constructs (:construct/_transgene-construct tg)]
           (for [construct constructs]
             (keys construct)))
   :description "map position of the integrated transgene"})

(defn construction-summary [tg]
  {:data (when-let [cs (:transgene/construction-summary tg)]
          (apply str cs))
   :description "Construction details for the transgene"})

(defn coinjection-marker-other [tg]
  {:data (when-let [cs (:transgene/coinjection-other tg)]
           (for [c cs]
             {:id c
              :label c
              :class "text"
              :taxonomy "all"}))
   :description "Coinjection marker for this transgene"})

(defn integrated-from [tg]
  {:data (when-let [t (:transgene/integrated-from tg)]
           (map pack-obj t))
   :description "integrated from"})

(defn laboratory [tg]
  (if-let [construct (first (:construct/_transgene-construct tg))]
    (generic/laboratory construct)
    (generic/laboratory tg)))


(def widget
  {:name generic/name-field
   :laboratory laboratory
   :integration_method integration-method
   :author author
   :construct construct
   :coinjection_marker coinjection-marker
   :recombination_site recombination-site
   :construction_summary construction-summary
   :coinjection_marker_other coinjection-marker-other
   :integrated_from integrated-from
})
