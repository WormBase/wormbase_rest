(ns rest-api.classes.expr-profile.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pcr-data [ep]
  {:data nil
   :desciption "pcr data of the expression profile"})

(defn expression-map [ep]
  {:data nil
   :desciption "expression map data for expr_profile"})

(defn remarks [ep]
  {:data nil
   :desciption "curatorial remarks for the Expr_profile"})

(defn method [ep]
  {:data nil
   :desciption "the method used to describe the Expr_profile"})

(defn pcr-products [ep]
  {:data nil
   :desciption "pcr_products for the expression profile"})

(defn profile [ep]
  {:data nil
   :desciption "expression profiles for set of genes"})

(defn rnai  [ep]
  {:data nil
   :desciption "RNAis associated with this expression profile"})

(def widget
  {:name generic/name-field
   :pcr_data pcr-data
   :expression_map expression-map
   :remarks remarks
   :method method
   :pcr_products pcr-products
   :profile profile
   :rnai rnai})
