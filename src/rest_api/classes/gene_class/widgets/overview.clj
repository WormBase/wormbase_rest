(ns rest-api.classes.gene-class.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn laboratory [gc]
  {:data (when-let [laboratory (:gene-class/designating-laboratory gc)]
           {:laboratory (pack-obj laboratory)
            :representative (some->> (:laboratory/representative laboratory)
                                     (map pack-obj))})
   :description "The laboratory where the Gene_class was isolated, created, or named"})

(defn former-laboratory [gc]
  {:data (when-let [lhs (:gene-class/former-designating-laboratory gc)]
           (let [lh (last lhs)
                 lab (:gene-class.former-designating-laboratory/laboratory lh)]
             {:lab (pack-obj lab)
              :time (when-let [t (:gene-class.former-designating-laboratory/until lh)]
                      (date/format-date4 t))}))
   :description "Former designating laboratory for the gene class and the date of retirement"})

(defn other-names [gc]
  {:data (some->> (:gene-class/_main-name gc)
                  (map :gene-class/id))
   :description (str "other names that have been used to refer to " (:gene-class/id gc))})

(defn evidence [gc]
  {:data (some->> (:gene-class/evidence gc)
                  (:evidence/paper-evidence)
                  (first)
                  (pack-obj))
   :description (str "evidence for the gene class " (:gene-class/id gc))})

(defn description [gc]
  {:data (:gene-class/description gc)
   :description "Description of the gene class"})

(def widget
  {:name generic/name-field
   :laboratory laboratory
   :former_laboratory former-laboratory
   :evidence evidence
   :remarks generic/remarks
   :description description
   :other_names other-names})
