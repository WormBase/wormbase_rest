(ns rest-api.classes.interaction.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn libraries-screened [i]
  {:data nil
   :description "Libraries screened for the interaction"})

(defn regualtion-result [i]
  {:data nil
   :description "Regulation results for this interaction"})

(defn interaction-type [i]
  {:data nil
   :description "Type of the interaction"})

(defn interaction-phenotype [i]
  {:data nil
   :description "Phenotype details for the interaction"})

(defn interaction-summary [i]
  {:data nil
   :description "Interaction data was extracted by a curator from sentences enriched by Textpresso.  The interaction was attributed to the paper(s) from which it was extracted."})

(defn process [i]
  {:data nil
   :description "WBProcess for the interaction"})

(defn detection-method [i]
  {:data nil
   :description "Method(s) by which the interaction was detected"})

(defn regulation-level [i]
  {:data nil
   :description "Regulation level for this interaction"})

(defn confidence [i]
  {:data nil
   :description "Confidence details for the interaction"})

(defn rnai [i]
  {:data nil
   :description "RNAi details for the interaction"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :libraries_screened libraries-screened
   :regulation_result regulation-result
   :interaction_type interaction-type
   :interaction_phenotype interaction-phenotype
   :interaction_summary interaction-summary
   :process process
   :detection_method detection-method
   :regulation_level regulation-level
   :remarks generic/remarks
   :confidence confidence
   :hisotrical_gene generic/historical-gene
   :rnai rnai
   :interactor interactor})
