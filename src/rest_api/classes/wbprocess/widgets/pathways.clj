(ns rest-api.classes.wbprocess.widgets.pathways
  (:require
   [pseudoace.utils :as pace-utils]
   [clojure.data.xml :refer :all]
   [clj-http.client :as client]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pathway [p]
  {:data (let [wiki-url "http://webservice.wikipathways.org/getCurationTagsByName?tagName=Curation:WormBase_Approved"]
           (when-let [pathways (some->> (:wbprocess/database p)
                                        (filter (fn [d]
                                                  (= (:database/id
                                                       (:wbprocess.database/database d))
                                                     "WikiPathways")))
                                        (map :wbprocess.database/accession))]
             (some->> (:content (parse-str (:body (client/get wiki-url))))
                      (map (fn [content]
                             (some->> content
                                      :content
                                      (filter #(= (:tag %) :pathway))
                                      (map (fn [pathway]
                                             {:pathway_id
                                              (some->> (:content pathway)
                                                       (filter #(= (:tag %) :id))
                                                       first
                                                       :content
                                                       first)
                                              :revision
                                              (some->> (:content pathway)
                                                       (filter #(= (:tag %) :revision))
                                                       first
                                                       :content
                                                       first)}))
                                      (into {}))))
                      (filter #(contains? (set pathways) (:pathway_id %))))))
   :description "Related wikipathway link"})

(def widget
  {:name generic/name-field
   :pathway pathway})
