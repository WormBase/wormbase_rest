(ns rest-api.classes.gene-class.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn laboratory [gc] ; according to the datomic schema this should return an array of results but returns only one
  {:data (when-let [laboratory (:gene-class/designating-laboratory gc)]
           {:laborotory (pack-obj laboratory)
            :representative (when-let [reps (:laboratory/representative laboratory)]
                              (for [rep reps]
                                (pack-obj rep)))})
   :description "The laboratory where the Gene_class was isolated, created, or named"})

(defn former-laboratory [gc]
  {:data (when-let [lhs (:gene-class/former-designating-laboratory gc)]
           (let [lh (last lhs)
                 lab (:gene-class.former-designating-laboratory/laboratory lh)]
             {:lab (pack-obj lab)
              :time (when-let [t (:gene-class.former-designating-laboratory/until lh)]
                      (date/format-date4 t)
                      )}))
   :description "Former designating laboratory for the gene class and the date of retirement"})

(defn remarks [gc]
  {:data (when-let [rs (:gene-class/remark gc)]
           (for [r rs]
             {:text (:gene-class.remark/text r)
              :evidence nil}))
   :description "curatorial remarks for the Gene_class"})

(defn description [gc]
  {:data (first (:gene-class/description gc))
   :description (str "description of the Gene_class " (:gene-class/id gc))})

(defn other-names [gc]
  {:data nil ; This field does not exist on this object
   :description (str "other names that have been used to refer to " (:gene-class/id gc))})

(def widget
  {:name generic/name-field
   :laboratory laboratory
   :former_laboratory former-laboratory
   :remarks remarks
   :description description
   :other_names other-names})
