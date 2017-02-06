(ns rest-api.classes.person.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


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

 (defn name-field [person]
   (let [data (pack-obj "person" person)]
     {:data (if (empty? data) nil data)
      :description (format "The name and WormBase internal ID of %s" (:person/id person))}))

 (defn web-page [person]
   (let [db (d/entity-db person)
         data (->> (d/q '[:find [?web-page ...]
                        :in $ ?person
                        :where [?person :person/address ?address]
                               [?address :address/web-page ?web-page]]
                      db (:db/id person))
                   (seq))]
     {:data (if (empty? data) nil data)
      :description "web-page of this person"}))
 
 (defn orcid [person]
   (let [db (d/entity-db person)
         orcids (->> (d/q '[:find [?orcid ...]
                          :in $ ?person
                          :where [?person :person/database ?dbent]
                                 [?orc :database/id "ORCID"]
                                 [?dbent :person.database/database ?orc]
                                 [?dbent :person.database/accession ?orcid]]
                        db (:db/id person))
                     (seq))
         data (for [orcid orcids]
                 {:class "ORCID"
                  :id orcid
                  :label orcid})]
     {:data (if (empty? data) nil data)
      :description "ORCID of this person"}))
 
 (defn institution [person]
   (let [db (d/entity-db person)
         data (->> (d/q '[:find [?institution ...]
                        :in $ ?person
                        :where [?person :person/address ?address]
                               [?address :address/institution ?institution]]
                      db (:db/id person))
                   (seq))]
     {:data (if (empty? data) nil data)
      :description "institution of this person"}))
 
 (defn email [person]
   (let [db (d/entity-db person)
         data (->> (d/q '[:find [?email ...]
                        :in $ ?person
                        :where [?person :person/address ?address]
                               [?address :address/email ?email]]
                      db (:db/id person))
                   (seq))]
     {:data (if (empty? data) nil data)
      :description "email addresses of this person"}))
 
 (defn also-known-as [person]
   {:data
    (or (:person/also-known-as person)
        "unknown")
    :description
   "other names person is also known as."})

;; (defn previous-addresses [person]
;;   (let [db (d/entity-db person)
;;         data
;;         (->> (d/q '[:find [?old-address ...]
;;                   :in $ ?person
;;                   :where [?person :person/old-address ?old-address]
;;                          ]
;;                 db (:db/id person))
;;              (map (fn [oid]
;;                     (let [old-address (entity db oid)]
;;                       (vmap
;;                        :date-modified (date-helper/format-date (:person.old-address/datetype old-address))
;;                        :email (:address/email old-address)
;;                        :institution (:address/institution old-address)
;;                        :street-address (:address/street-address old-address)
;;                        :country (:address/country old-address)
;;                        :main-phone (:address/main-phone old-address)
;;                        :lab-phone (:address/lab-phone old-address)
;;                        :office-phone (:address/office-phone old-address)
;;                        :other-phone (:address/other-phone old-address)
;;                        :fax (:address/fax old-address)
;;                        :web-page (:address/web-page old-address)
;;                       )
;;                     )))
;;              (seq))]
;;     {:data (if (empty? data) nil data)
;;      :description
;;      "previous addresses of this person."}))


(def widget
  {
    :name                     name-field
    :email                    email
    :orcid                    orcid
    :institution              institution
    :web_page                 web-page
    :street_address           street-address
    :also_known_as            also-known-as
;;    :previous_addresses       previous-addresses
  })

