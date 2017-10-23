(ns rest-api.classes.feature.core
  (:require
   [clojure.string :as str]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.generic-fields :as generic]
   [datomic.api :as d]))

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
       (map #(pack-obj (:feature.bound-by-product-of/gene %)))
       (seq)))

(defn- transcription-factor [feature]
  (when-first [f (:feature/associated-with-transcription-factor feature)]
    (pack-obj
     "transcription-factor"
     (:feature.associated-with-transcription-factor/transcription-factor
      f))))

(defn associated-feature [db fid]
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
     :interaction (not-empty inter)
     :expr_pattern (not-empty ep)
     :bounded_by (not-empty bbpo)
     :tf tf}))
