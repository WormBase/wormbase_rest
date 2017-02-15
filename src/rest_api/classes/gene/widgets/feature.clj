(ns rest-api.classes.gene.widgets.feature
  (:require
   [clojure.string :as str]
   [rest-api.formatters.object :refer [pack-obj]]
   [rest-api.classes.gene.sequence :as sequence-fns]
   [rest-api.classes.gene.generic :as generic]
   [datomic.api :as d]))

(defn- not-nil [xs]
  (if (nil? xs)
    []
    xs))

(defn- expr-pattern [db fid]
  (->> (d/q '[:find [?e ...]
              :in $ ?f
              :where
              [?ef :expr-pattern.associated-feature/feature ?f]
              [?e :expr-pattern/associated-feature ?ef]
              [?e :expr-pattern/anatomy-term _]]
            db fid)
       (map
        (fn [eid]
          (let [expr (d/entity db eid)]
            {:text
             (map #(pack-obj "anatomy-term" (:expr-pattern.anatomy-term/anatomy-term %))
                  (:expr-pattern/anatomy-term expr))
             :evidence {:by (pack-obj "expr-pattern" expr)}})))
       (seq)))

(defn- interaction [feature]
  (->> (:interaction.feature-interactor/_feature feature)
       (map #(pack-obj "interaction" (:interaction/_feature-interactor %)))
       (seq)))

(defn- bounded-by [feature]
  (->> (:feature/bound-by-product-of feature)
       (map #(pack-obj "gene" (:feature.bound-by-product-of/gene %)))
       (seq)))

(defn- transcription-factor [feature]
  (when-first [f (:feature/associated-with-transcription-factor feature)]
    (pack-obj
     "transcription-factor"
     (:feature.associated-with-transcription-factor/transcription-factor
      f))))

(defn- associated-feature [db fid]
  (let [feature (d/entity db fid)
        method (-> (:locatable/method feature)
                   (:method/id))
        inter (interaction feature)
        ep (expr-pattern db fid)
        bbpo (bounded-by feature)
        tf (transcription-factor feature)]
    {:name (pack-obj "feature" feature)
     :description (first (:feature/description feature))
     :method (if (not (nil? method))
               (str/replace method #"_" " "))
     :interaction (not-nil inter)
     :expr_pattern (not-nil ep)
     :bounded_by (not-nil bbpo)
     :tf tf}))

(defn associated-features [gene]
  (let [db (d/entity-db gene)
        data  (->>
                (d/q '[:find [?f ...]
                       :in $ ?gene
                       :where
                       [?fg :feature.associated-with-gene/gene ?gene]
                       [?f :feature/associated-with-gene ?fg]]
                   db (:db/id gene))
                (map (partial associated-feature db))
                (seq))]
    {:data (not-nil data)
     :description
     "Features associated with this Gene"}))

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
