(ns rest-api.classes.expr-profile.widgets.overview
  (:require
   [rest-api.db.sequence :as seqdb]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

; testing with 0C24D10.5, 0W09C5.2 (has sequence)

(defn pcr-data [ep]
  {:data (if-let [m (first (:expr-profile/expr-map ep))]
           (let [primer (-> ep :locatable/parent :pcr-product/id)
                 feature (when-let [species-name (->> ep
                                                      :locatable/parent
                                                      :pcr-product/species
                                                      :species/id)]
                          (let [g-species (generic-functions/xform-species-name species-name)
                                sequence-database (seqdb/get-default-sequence-database g-species)
                                db-spec ((keyword sequence-database) seqdb/sequence-dbs)]
                           (first (seqdb/get-features db-spec primer))))]
           {:seq (when feature (str (:seqname feature) ":" (:start feature) ".." (:end feature)))
            :chromosome (:seqname feature)
            :radius 4
            :x_coord (:sk-map/x-coord m)
            :y_coord (:sk-map/y-coord m)
            :start (:start feature)
            :stop (:end feature)
            :primer primer
            :mountain (:sk-map/mountain m)}))
   :desciption "pcr data of the expression profile"})

(defn expression-map [ep]
  {:data (:sk-map/id (first (:expr-profile/expr-map ep)))
   :desciption "expression map data for expr_profile"})

(defn pcr-products [ep]
  {:data (when-let [p (:locatable/parent ep)]
          {(:pcr-product/id ep) (pack-obj p)})
   :desciption "pcr_products for the expression profile"})

(defn profile [ep]
  {:data nil ;can't find this popuated in the Ace Version
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
