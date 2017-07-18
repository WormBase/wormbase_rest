(ns rest-api.classes.paper.core
  (:require
   [clojure.string :as str]
   [rest-api.formatters.object :as obj]))

(defn evidence [paper]
  {:class "paper"
   :id (:paper/id paper)
   :taxonomy "all"
   :label (str
            (obj/author-list paper)
            ", "
            (if (= nil (:paper/publication-date paper))
              ""
              (first (str/split (:paper/publication-date paper)
                                #"-"))))})
