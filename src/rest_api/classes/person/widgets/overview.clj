(ns rest-api.classes.person.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

;; (def-rest-widget overview [person]
;;   {:name                     (person-fields/name-field person)
;;    :email                    (person-fields/email person)
;;    :orcid                    (person-fields/orcid person)
;;    :institution              (person-fields/institution person)
;;    :web_page                 (person-fields/web-page person)
;;    :street_address           (person-fields/street-address person)
;;    :also_known_as            (person-fields/also-known-as person)
;;    :previous_addresses       (person-fields/previous-addresses person)
;; })

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
  {
;;  :also_refers_to           also-refers-to
;;  :name                     name-field
;;  :email                    email
;;  :orcid                    orcid
;;  :institution              institution
;;  :web_page                 web-page
  :street_address           street-address
;;  :also_known_as            also-known-as
;;  :previous_addresses       previous-addresses
  })

(defn street-address [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find [?street-address ...]
                         :in $ ?person
                         :where [?person :person/address ?address]
                                [?address :address/street-address ?street-address]]
                       db (:db/id person))
                  (seq))]
    {:data (if (empty? data) nil data)
     :description "street address of this person"}))


;; (defn name-field [person]
;;   (let [data (pack-obj "person" person)]
;;     {:data (if (empty? data) nil data)
;;      :description (format "The name and WormBase internal ID of %s" (:person/id person))}))

