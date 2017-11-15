(ns rest-api.classes.gene.widgets.feature
  (:require
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.sequence.main :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]
    [datomic.api :as d]))

(defn associated-features [gene]
  (let [db (d/entity-db gene)
        data (->>
               (d/q '[:find [?f ...]
                      :in $ ?gene
                      :where
                      [?fg :feature.associated-with-gene/gene ?gene]
                      [?f :feature/associated-with-gene ?fg]]
                    db (:db/id gene))
               (map (partial feature/associated-feature db))
               (seq))]
    {:data (not-empty data)
     :description "Features associated with this Gene"}))

(defn- segment-to-position [gene segment gbrowse]
  (let [[start, stop] (->> segment
                           ((juxt :start :end))
                           (sort-by +))
        padded-start (- start 2000)
        padded-stop (+ stop 2000)
        tracks ["GENES"
                "RNASEQ_ASYMMETRIES"
                "RNASEQ"
                "RNASEQ_SPLICE"
                "POLYSOMES"
                "MICRO_ORF"
                "DNASEI_HYPERSENSITIVE_SITE"
                "REGULATORY_REGIONS"
                "PROMOTER_REGIONS"
                "HISTONE_BINDING_SITES"
                "TRANSCRIPTION_FACTOR_BINDING_REGION"
                "TRANSCRIPTION_FACTOR_BINDING_SITE"
                "BINDING_SITES_PREDICTED"
                "BINDING_SITES_CURATED"
                "BINDING_REGIONS"]]
    (sequence-fns/create-genomic-location-obj padded-start padded-stop gene segment tracks gbrowse)))

(defn feature-image [gene]
  (let [segment (sequence-fns/get-longest-segment gene)
        data (if (not (empty? segment))
               (segment-to-position gene segment true))]
    (if (not (empty? data))
      {:data data
       :description (str "The genomic location of the sequence "
                         "to be displayed by GBrowse")})))

(def widget
  {:feature_image feature-image
   :name          generic/name-field
   :features      associated-features})
