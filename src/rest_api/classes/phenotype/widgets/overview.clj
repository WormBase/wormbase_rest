(ns rest-api.classes.phenotype.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn is-dead [p]
 {:data (if (contains? :phenotype/dead p)
          (:db/id p) ; next have to get the Alternate phentoype but don't know how : WBPhenotype_0001976
          )
  :description "The Note of the phenotype when it's retired and replaced by another."})

(defn synonyms [p]
  {:data (when-let [shs (:phenotype/synonym p)]
           (for [sh shs]
             (:phenotype.synonym/text sh)))
   :description "Synonymous name of the phenotype"})

(defn description [p]
  {:data (when-let [d (:phenotype/description p)]
           [{:text (:phenotype.description/text d)
            :evidence (obj/get-evidence d)}])
   :description (str "description of the Phenotype" (:phenotype/id p))})

(defn related-phenotypes [p]
  {:data (pace-utils/vmap
           "Generalisation of"
           (when-let [phenotypes (:phenotype/_specialisation-of p)]
             (for [phenotype phenotypes]
               (pack-obj phenotype)))

           "Specialisation of"
           (when-let [phenotypes (:phenotype/specialisation-of p)]
             (for [phenotype phenotypes]
               (pack-obj phenotype))))
   :description "The generalized and specialized terms in the ontology for this phenotype."})

(def widget
  {:name generic/name-field
   :is_dead is-dead
   :synonyms synonyms
   :remarks generic/remarks
   :description description
   :related_phenotypes related-phenotypes})
