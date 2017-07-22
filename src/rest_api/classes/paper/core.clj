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
(defn get-authors [p]
  (when-let [hs (:paper/author p)]
           (vals
             (into
               (sorted-map)
               (into
                 {}
                 (for [h hs
                       :let [author (cond
                                      (contains? h :affiliation/person)
                                      (first (:affiliation/person h))

                                      (contains? h :paper.author/author)
                                      (:paper.author/author h))]]
                   {(:ordered/index h)
                      {:id (or (:author/id author)
                               (:person/id author))
                       :class "author"
                       :label (if (:person/id author)
                                (let [lastname (:person/last-name author)
                                      first-initial (when-let [firstname (:person/first-name author)]
                                                      (str/capitalize (get firstname 0)))]
                                  (if-let [middlenames (:person/middle-name author)]
                                    (let [middle-initials (str/join ". " middlenames)]
                                      (str lastname ", " first-initial ". " middle-initials "."))
                                    (str lastname ", " first-initial ".")))
                                (if-let [[lastname initial] (str/split (:author/id author) #" ")]
                                  (str lastname ", " initial ".")))
                       :taxonomy "all"}}))))))
