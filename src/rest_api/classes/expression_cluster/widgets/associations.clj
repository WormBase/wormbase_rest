(ns rest-api.classes.expression-cluster.widgets.associations
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn life-stages [ec]
  {:data (when-let [lss (:expression-cluster/life-stage ec)]
           (for [ls lss]
             {:life_stages (pack-obj ls)
              :definition (->> (:life-stage/definition ls)
                               (:life-stage.definition/text))}))
   :description "Life stages associated with this expression cluster"})

(defn go-terms [ec] ; non in the database
  {:data (when-let [go-terms (:expression-cluster/go-term ec)]
           (map pack-obj go-terms))
   :description "GO terms associated with this expression cluster"})

(defn anatomy-terms [ec]
  {:data (when-let [aths (:expression-cluster/anatomy-term ec)]
           (for [ath aths
                 :let [at (:expression-cluster.anatomy-term/anatomy-term ath)]]
             {:anatomy_term (pack-obj at)
              :definition (:anatomy-term.definition/text
                            (:anatomy-term/definition at))}))

   :description "anatomy terms associated with this expression cluster"})

(defn processes [ec]
  {:data (when-let [phs (:wbprocess.expression-cluster/_expression-cluster ec)]
           (for [ph phs
                 :let [wbprocess (:wbprocess/_expression-cluster ph)]]
             {:processes (pack-obj wbprocess)
              :definition (:wbprocess.summary/text
                            (:wbprocess/summary wbprocess))}))
   :description "Processes associated with this expression cluster"})

(def widget
  {:name generic/name-field
   :life_stages life-stages
   :go_terms go-terms
   :anatomy_terms anatomy-terms
   :processes processes})
