(ns web.rest.interactions
  (:use web.rest.object)
  (:require [cheshire.core :as json]
            [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap]]))


(def ^:private interactor-role-map
  {:interactor-info.interactor-type/effector           :effector
   :interactor-info.interactor-type/affected           :affected
   :interactor-info.interactor-type/trans-regulator    :effector
   :interactor-info.interactor-type/cis-regulatory     :effector
   :interactor-info.interactor-type/trans-regulated    :affected
   :interactor-info.interactor-type/cis-regulated      :affected})
   

(def ^:private interactor-target
  (some-fn
   :interaction.interactor-overlapping-gene/gene
   :interaction.interactor-overlapping-cds/cds
   :interaction.interactor-overlapping-protein/protein
   ;; and more...
   ))

(defn- interactor-role [interactor]
  (or (interactor-role-map (first (:interactor-info/interactor-type interactor)))
      :other))

(defn- humanize-name [ident]
  (-> (name ident)
      (str/replace #"-" " ")
      (str/capitalize)))

(defn- interaction-type [int]
  (cond
   (:interaction/physical int)
   "Physical"

   (:interaction/predicted int)
   "Predicted"

   (:interaction/regulatory int)
   (humanize-name (first (:interaction/regulatory int)))

   (:interaction/genetic int)
   (humanize-name (first (:interaction/genetic int)))

   :default
   "Unknown"))

(defn- interaction-info [int ref-obj]
  (if (or (not (:interaction/predicted int))
          (> (or (:interaction/log-likelihood-score int) 1000) 1.5))
    (let [{effectors :effector
           affecteds :affected
           others :other}
          (group-by interactor-role (:interaction/interactor-overlapping-gene int))
          type (interaction-type int)]
      (->>
       (if (or effectors affecteds)
         (for [objh  (concat effectors others)
               obj2h affecteds
               :let [obj (interactor-target objh)
                     obj2 (interactor-target obj2h)]
               :when (or (= obj ref-obj)
                         (= obj2 ref-obj))]
           [(str (:label (pack-obj obj)) " " (:label (pack-obj obj2)))
            {:type type
             :effector obj
             :affected obj2
             :direction "Effector->Affected"
             :phenotype nil}])
         (for [objh  others
               obj2h others
               :let [obj (interactor-target objh)
                     obj2 (interactor-target obj2h)]
               :when (not= obj obj2)]
           [(str/join " " (sort [(:label (pack-obj obj)) (:label (pack-obj obj2))]))
            {:type type
             :effector obj
             :affected obj2
             :direction "non-directional"
             :phenotype nil}]))
       (into {})
       (vals)))))

(defn obj-interactions [class obj]
  (let [follow #(map :interaction/_interactor-overlapping-gene
                     (:interaction.interactor-overlapping-gene/_gene %))
        ints (follow obj)]
    {:edges
     (->> (mapcat (fn [interaction]
                    (map vector
                         (repeat interaction)
                         (interaction-info interaction obj)))
                  ints)
      
          (reduce
           (fn [data [interaction {:keys [type effector affected direction phenotype]}]]
             (let [ename (:label (pack-obj effector))
                   aname (:label (pack-obj affected))
                   key1 (str ename " " aname " " type)
                   key2 (str aname " " ename " " type)
                   pack-int (pack-obj "interaction" interaction :label (str ename " : " aname))
                   papers (map (partial pack-obj "paper") (:interaction/paper interaction))]
               (cond
                (data key1)
                (-> data
                    (update-in [key1 :interactions] conj pack-int)
                    (update-in [key1 :citations] into papers))
                
                (data key2)
                (-> data
                    (update-in [key2 :interactions] conj pack-int)
                    (update-in [key2 :citations] into papers))
           
                :default
                (assoc data key1
                       {:interactions [pack-int]
                        :citations    (set papers)
                        :type         type
                        :effector     (pack-obj effector)
                        :affected     (pack-obj affected)
                        :direction    direction
                        :phenotype    phenotype
                        :nearby       0}))))
           {})
          (vals))}))

(defn get-interactions [class db id]
  (let [obj (obj-get class db id)]
    (if obj
      {:status 200
       :content-type "application/json"
       :body (json/generate-string
              {:name id
               :class class
               :uri "whatevs"
               :fields
               {:name (obj-name class db id)
                :interactions
                {:description "genetic and predicted interactions"
                 :data (obj-interactions class obj)}}}
              {:pretty true})})))
