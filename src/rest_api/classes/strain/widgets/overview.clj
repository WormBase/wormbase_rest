(ns rest-api.classes.strain.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mutagen [s]
  {:data (first (:strain/mutagen s))
   :description "the mutagen used to generate this stain"})

(defn outcrossed [s]
  {:data (first (:strain/outcrossed s))
   :description "extent to which the strain has been outcrossed"})

(defn genotype [s]
  {:data (when-let [genotype (:strain/genotype s)]
           {:str genotype
            :data (when-let [genes (:gene/_strain s)]
                    (into
                      {}
                      (for [gene genes]
                        {(or (:gene/public-name gene)
                             (:gene/id gene))
                         (pack-obj gene)})))})
   :description "the genotype of the strain"})

(def widget
  {:name generic/name-field
   :mutagen mutagen
   :outcrossed outcrossed
   :taxonomy generic/taxonomy
   :genotype genotype
   :remarks generic/remarks
   :other_names generic/other-names})
