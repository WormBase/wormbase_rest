(ns datomic-rest-api.rest.gene.widgets.external-links
  (:require
   [datomic-rest-api.rest.gene.generic :as generic]
   [pseudoace.utils :as pace-utils]))

(defn xrefs [gene]
  {:data
   (reduce
    (fn [refs db]
      (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")]
        (update-in refs
                   [(:database/id (:gene.database/database db))
                    (:database-field/id (:gene.database/field db))
                    :ids]
                   pace-utils/conjv
                   (let [acc (:gene.database/accession db)]
                     (if-let [[_ rest] (match-accession acc)]
                       rest
                       acc)))))
     {}
     (:gene/database gene))
   :description (str "external databases and IDs containing "
                     "additional information on the object")})

(def widget
  {:name generic/name-field
   :xrefs xrefs})
