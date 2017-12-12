(ns rest-api.classes.wbprocess.widgets.pathways
  (:require
   [pseudoace.utils :as pace-utils]
   [clojure.data.xml :refer :all]
   [clj-http.client :as client]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pathway [p]
  {:data (let [wiki-url "http://webservice.wikipathways.org/getCurationTagsByName?tagName=Curation:WormBase_Approved"
               pathways (some->> (:wbprocess/database p)
                                 (filter (fn [d]
                                           (= (:database/id
                                                (:wbprocess.database/database d))
                                              "WikiPathways")))
                                 (map :wbprocess.database/accession))
               approved-pathways (some->> (:content (parse-str (:body (client/get wiki-url))))
                                          (map (fn [content]
                                                 (some->> content
                                                          :content
                                                          (filter #(= (:tag %) :pathway))
                                                          (map (fn [pathway]
                                                                 {(some->> (:content pathway)
                                                                                   (filter #(= (:tag %) :id))
                                                                                   first
                                                                                   :content
                                                                                   first)
                                                                  (some->> (:content pathway)
                                                                           (filter #(= (:tag %) :revision))
                                                                           first
                                                                           :content
                                                                           first)}))
                                                          (into {}))))
                                          (into {})
                                          )]
               (some->> pathways
                       (map (fn [id]
                             (pace-utils/vmap
                        :pathway_id id
                        :revision (get approved-pathways id))))))
   :description "Related wikipathway link"})

(def widget
  {:name generic/name-field
   :pathway pathway})
