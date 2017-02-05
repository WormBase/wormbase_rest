(ns rest-api.classes.person.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

;; (defn also-refers-to [gene]
;;   (let [db (d/entity-db gene)]
;;     {:data
;;      (if-let [data
;;               (->> (d/q '[:find [?other-gene ...]
;;                           :in $ ?gene
;;                           :where
;;                           [?gene :gene/cgc-name ?cgc]
;;                           [?cgc :gene.cgc-name/text ?cgc-name]
;;                           [?other-name :gene.other-name/text ?cgc-name]
;;                           [?other-gene :gene/other-name ?other-name]]
;;                         db (:db/id gene))
;;                    (map #(pack-obj "gene" (d/entity db %)))
;;                    (seq))]
;;        data)
;;      :description
;;      "other genes that this locus name may refer to"}))

(def widget
  {:also_refers_to           also-refers-to
   :name                     name-field
  })


