(ns rest-api.classes.strain.widgets.contains
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.variation.core :as variation-core]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn rearrangements [s]
  {:data (when-let [rs (:rearrangement/_strain s)]
           (map pack-obj rs))
   :description "rearrangements contained in the strain"})

(defn clones [s]
  {:data (when-let [clones (:clone/_in-strain s)]
           (if (> (count clones) 500)
             (str (count clones) " found")
             (map pack-obj clones)))
   :description "clones contained in the strain"})

(defn alleles [s]
  {:data (when-let [vhs (:variation.strain/_strain s)]
           (for [vh vhs
                 :let [variation (:variation/_strain vh)]]
             (variation-core/process-variation variation)))
   :description "alleles contained in the strain"})

(defn genes [s]
  {:data (when-let [genes (:gene/_strain s)]
           (map pack-obj genes))
   :description "genes contained in the strain"})

(defn transgenes [s]
  {:data (when-let [tgs (:transgene/_strain s)] 
             (map pack-obj tgs))
   :description "transgenes carried by the strain"})

(def widget
  {:name generic/name-field
   :rearrangements rearrangements
   :clones clones
;   :alleles alleles
   :genes genes
   :transgenes transgenes})
