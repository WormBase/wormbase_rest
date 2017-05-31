(ns rest-api.classes.expr-profile.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


; testing with 0C24D10.5

(defn pcr-data [ep]
  {:data nil ; need sequence database
   :desciption "pcr data of the expression profile"})

(defn expression-map [ep]
  {:data (:sk-map/id (first (:expr-profile/expr-map ep)))
   :desciption "expression map data for expr_profile"})

(defn pcr-products [ep]
  {:data nil ; can't find field
   :keys (keys ep)
   :dbid (:db/id ep)
   :desciption "pcr_products for the expression profile"})

(defn profile [ep]
  {:data nil ;need sequence database
   :desciption "expression profiles for set of genes"})

(defn rnai  [ep]
  {:data (when-let [rs (:expr-profile/rnai-result ep)]
           (for [r rs]
             {:strain (when-let [s (:rnai/strain r)]
                        (pack-obj s))
              :genotype (:rnai/genotype r)
              :rnai (pack-obj r)}))
  :desciption "RNAis associated with this expression profile"})

(def widget
  {:name generic/name-field
   :pcr_data pcr-data
   :expression_map expression-map
   :remarks generic/remarks
   :method generic/method
   :pcr_products pcr-products
   :profile profile
   :rnai rnai})
