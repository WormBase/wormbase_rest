(ns rest-api.classes.laboratory.widgets.all-labs
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.db.main :refer [datomic-conn]]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-fns]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn all-labs [s]
  {:data (let [db (d/db datomic-conn)]
           (->> (d/q '[:find [?s ...]
                        :in $
                        :where
                        [?s :laboratory/id]]
                      db)
                (map (fn [id]
                       (let [obj (d/entity db id)]
                         {:email (first (:laboratory/e-mail obj))
                          :allele_designation (:laboratory/allele-designation obj)
                          :lab (let [lab-obj (pack-obj obj)]
                                 (conj
                                   lab-obj
                                   {:label (:id lab-obj)}))
                          :url (when-let [url-raw (first (:laboratory/url obj))]
                                 (let [lc-url (str/lower-case url-raw)]
                                   (if (not  (or (str/starts-with? lc-url "http://")
                                                 (str/starts-with? lc-url "https://")))
                                     (str "http://" lc-url)
                                     lc-url)))
                          :affiliation (first (:laboratory/mail obj))
                          :represenative (when-let [rep (:laboratory/representative obj)]
                                           (pack-obj (first rep)))})))
                (remove nil?)))
   :description "the natural isolates of the strain"})

(def widget
  {:name generic/name-field
   :all_labs all-labs})
