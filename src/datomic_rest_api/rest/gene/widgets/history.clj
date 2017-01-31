(ns datomic-rest-api.rest.gene.widgets.history
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [datomic-rest-api.formatters.date :as dates]
   [datomic-rest-api.formatters.object :refer [pack-obj]]
   [datomic-rest-api.rest.gene.generic :as generic]
   [pseudoace.utils :as pace-utils]))

(defn history-events [gene]
  (let [data
   (->>
    (:gene/version-change gene)
    (mapcat
     (fn [h]
       (let [result {:version (:gene.version-change/version h)
                     :curator (pack-obj "person"
                                        (:gene.version-change/person h))
                     :remark nil
                     :date (if (:gene.version-change/date h)
                             (dates/format-date
                              (:gene.version-change/date h)))
                     :type "Version_change"
                     :gene nil
                     :action "Unknown"}]
         (pace-utils/those
           (if (:gene-history-action/created h)
             (assoc result :action "Created"))

           (if (:gene-history-action/killed h)
             (assoc result :action "Killed"))

           (if (:gene-history-action/suppressed h)
             (assoc result :action "Suppressed"))

           (if (:gene-history-action/resurrected h)
             (assoc result :action "Resurrected"))

           (if (:gene-history-action/transposon-in-origin h)
             (assoc result :action "Transposon_in_origin"))

           (if (:gene-history-action/changed-class h)
             (assoc result :action "Changed_class"))

           (if-let [info (:gene-history-action/merged-into h)]
             (assoc result :action "Merged_into"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/acquires-merge h)]
             (assoc result :action "Acquires_merge"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-from h)]
             (assoc result :action "Split_from"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/split-into h)]
             (assoc result :action "Split_into"
                    :gene (pack-obj "gene" info)))

           (if-let [info (:gene-history-action/imported h)]
             (assoc result :action "Imported"
                    :remark (first info)))

           (if-let [name (:gene-history-action/cgc-name-change h)]
             (assoc result :action "CGC_name" :remark name))

           (if-let [name (:gene-history-action/other-name-change h)]
             (assoc result :action "Other_name" :remark name))

           (if-let [name (:gene-history-action/sequence-name-change h)]
             (assoc result :action "Sequence_name" :remark name)))))))]
  {:data  (if (empty? data) nil data)
   :description
   "the curatorial history of the gene"}))

(def q-historic
  '[:find [?historic ...]
    :in $ ?gene
    :where (or
            [?gene :gene/corresponding-cds-history ?historic]
            [?gene :gene/corresponding-pseudogene-history ?historic]
            [?gene :gene/corresponding-transcript-history ?historic])])
  
(defn old-annot [gene]
  (let [db (d/entity-db gene)]
    {:data (if-let [data
                    (->> (d/q q-historic db (:db/id gene))
                         (map (fn [hid]
                                (let [hobj (pack-obj (d/entity db hid))]
                                  {:class (str/upper-case (:class hobj))
                                   :name hobj})))
                         (seq))]
             data)
     :description "the historical annotations of this gene"}))

(def widget
  {:name generic/name-field
   :history history-events
   :old_annot old-annot})
