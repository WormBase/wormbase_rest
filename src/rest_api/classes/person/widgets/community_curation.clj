(ns rest-api.classes.person.widgets.community-curation
  (:require
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn author-first-pass-curation [person]
  {:data (->> person
              (:person/author-first-pass-curation)
              (map pack-obj)
              (seq))
   :description "Papers curated through Author First Pass"})

(def widget
  {:name generic/name-field
   :author_first_pass_curation author-first-pass-curation})
