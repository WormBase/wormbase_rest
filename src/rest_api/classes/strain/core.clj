(ns rest-api.classes.strain.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :refer [pack-obj]]))

(defn get-genotype [strain]
  (when-let [genotype (:strain/genotype strain)]
    {:str genotype
     :data (when-let [genes (:gene/_strain strain)]
             (into
              {}
              (for [gene genes]
                {(or (:gene/public-name gene)
                     (:gene/id gene))
                 (pack-obj gene)})))}))
