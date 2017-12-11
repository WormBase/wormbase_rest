(ns rest-api.classes.gene-class.widgets.current-genes
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn current-genes [g]
  {:data (some->> (:gene/_gene-class g)
                  (map (fn [o]
                         (let [species-name (:species/id  (:gene/species o))]
                           {:sequence (when-let [sn (:gene/sequence-name o)]
                                        {:id sn
                                         :label sn
                                         :class "gene_name"
                                         :taxonomy "all"})
                            :species_name species-name
                            :species (when-let [[genus species] (str/split species-name #" ")]
                                       {:genus genus
                                        :species species})
                            :locus (pack-obj o)})))
                  (group-by :species_name))
   :description "genes assigned to the gene class, organized by species"})

(def widget
  {:name generic/name-field
   :current_genes current-genes})
