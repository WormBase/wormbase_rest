(ns rest-api.classes.gene.widgets.references
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [rest-api.formatters.object :refer  [pack-obj]]
    [pseudoace.utils :as pace-utils]))

(defn references [gene]
  (println (:db/id gene))
  (if-let [papers (:gene/reference gene)]
    (let [number-of-papers (count papers)]
      {:count number-of-papers
       :page "all"
       :query (:gene/id gene)
       :species nil
       :type "paper"
       :uri (str "rest/widget/gene/" (:gene/id gene) "/references")
       :results (for [paper papers]
                  (let [abstract (:paper/abstract paper)
                        publication-date (:paper/publication-date paper)
                        pt (:paper/type paper)
                        author-holder (:paper/author paper)
                        year (if (nil? publication-date) nil (first (str/split publication-date #"-")))]
                  {:page (:paper/page paper)
                   :volume (:paper/volume paper)
                   :name {:coord
                          {:strand ""
                           :end ""
                           :taxonomy ""}
                          :class "paper"
                          :label (if-let [people (:affiliation/person author-holder)]
                                  (let [person (first people)] (str (:person/last-name person) " " (first (:person/first-name person)) " et al. (" year  ")" )))
                          :id (:paper/id paper)}
                   :taxonomy {}
                   :title  [(:paper/title paper)]
                   :author (let [author (:paper.author/author author-holder)
                                 people (:affiliation/person author-holder)
                                 person (first people)]
                             (if (nil? (:person/id person))
                               {:class "person"
                                :label (:author/id author)
                                :id (:author/id author)}
                               {:class "person"
                                :label (str (:person/last-name person) " " (first (:person/first-name person)))
                                :id (:person/id person)}))
                   :ptype (if (nil? paper) nil (:paper.type  pt))
                   :abstract (if (nil? abstract) nil [(:longtext/text (first abstract))])
                   :year year
                   :journal [(:paper/journal paper)]}))})))

(def widget
  {:data references})
