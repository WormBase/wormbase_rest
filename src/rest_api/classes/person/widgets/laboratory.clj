(ns rest-api.classes.person.widgets.laboratory
  (:require
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(defn lab-info [person]
  (let [db (d/entity-db person)
        data
        (->> (d/q '[:find [?laboratory ...]
                    :in $ ?person
                    :where [?laboratory :laboratory/registered-lab-members ?person]]
                  db (:db/id person))
             (map (fn [oid]
                    (let [laboratory (d/entity db oid)]
                      (pace-utils/vmap
                        :strain
                        (pace-utils/vmap
                          :taxonomy "all"
                          :class "laboratory"
                          :label (:laboratory/id laboratory)
                          :id (:laboratory/id laboratory))
                        :lab
                        (pace-utils/vmap
                          :taxonomy "all"
                          :class "laboratory"
                          :label (:laboratory/id laboratory)
                          :id (:laboratory/id laboratory))
                        :allele (:laboratory/allele-designation laboratory)
                        :rep
                        (->> (:laboratory/representative laboratory)
                             (map (fn [rep]
                                    {:taxonomy "all"
                                     :class "person"
                                     :label (:person/standard-name rep)
                                     :id (:person/id rep)})))))))
             (seq))]
    {:data (if (empty? data) nil data)
     :description
     "allele designation of the affiliated laboratory"}))
;; note that the perl catalyst interface does not return proper data for "rep", it gives a single value while the data could have multiple persons, e.g. WBPerson8705


(defn gene-classes [person]
  (let [db (d/entity-db person)
        data
        (first
          (->> (d/q '[:find [?laboratory ...]
                      :in $ ?person
                      :where [?laboratory :laboratory/registered-lab-members ?person]]
                    db (:db/id person))
               (map (fn [oid]
                      (let [laboratory (d/entity db oid)]
                        (->> (d/q '[:find [?geneclass ...]
                                    :in $ ?laboratory
                                    :where [?geneclass :gene-class/designating-laboratory ?laboratory]]
                                  db oid)
                             (map (fn [lid]
                                    (let [geneclass (d/entity db lid)]
                                      (pace-utils/vmap
                                        :lab
                                        {:taxonomy "all"
                                         :class "laboratory"
                                         :label (:laboratory/id laboratory)
                                         :id (:laboratory/id laboratory)}
                                        :desc (first (:gene-class/description geneclass))
                                        :gene_class
                                        {:taxonomy "all"
                                         :class "gene_class"
                                         :label (:gene-class/id geneclass)
                                         :id (:gene-class/id geneclass)}))))))))
               (seq)))]
    {:data (if (empty? data) nil data)
     :description
     "allele designation of the affiliated laboratory"}))

(defn previous-laboratories [person]
  (let [db (d/entity-db person)
        data
        (->> (d/q '[:find [?old-laboratory ...]
                    :in $ ?person
                    :where [?old-laboratory :laboratory/past-lab-members ?person]]
                  db (:db/id person))
             (map (fn [oid]
                    (let [old-laboratory (d/entity db oid)]
                      (first (into []
                                   (pace-utils/vmap
                                     (pace-utils/vmap
                                       :taxonomy "all"
                                       :class "laboratory"
                                       :label (:laboratory/id old-laboratory)
                                       :id (:laboratory/id old-laboratory))
                                     (or
                                       (first (->> (:laboratory/representative old-laboratory)
                                                   (map (fn [rep]
                                                          {:taxonomy "all"
                                                           :class "person"
                                                           :label (:person/standard-name rep)
                                                           :id (:person/id rep)}) )) )
                                       "no representative")))))))
             (seq))]
    {:data (if (empty? data) nil data)
     :description
     "previous laboratory affiliations."}))

(def widget
  {:lab_info                 lab-info
   :gene_classes             gene-classes
   :previous_laboratories    previous-laboratories})
