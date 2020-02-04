(ns rest-api.classes.go-term.widgets.associations
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [clojure.string :as str]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-field [g] ; zero found with this reference in db
  {:data nil
   :description "sequences annotated with this term"})

(defn anatomy-term [g] ; GO:0043058
  {:data (some->> (:anatomy-term.go-term/_go-term g)
                  (map (fn [ath]
                     (let [at (:anatomy-term/_go-term ath)]
                       {:class "Anatomy_term"
                        :description (some->> (:anatomy-term/definition at)
                                              (:anatomy-term.definition/text))
                        :evidence_code (when-let [evidence (obj/get-evidence ath)]
                                         {:evidence evidence})
                        :term (pack-obj at)}))))
   :description "anatomy terms annotated with this term"})

(defn get-genes [g] ;GO:0000032
 (some->> (:go-annotation/_go-term g)
                  (map (fn [annot]
                    (let [gene (:go-annotation/gene annot)]
                     {:gene (pack-obj gene)
                      :annot_id (:go-annotation/id annot)
                      :extensions (when-let [extensions (some->> (:go-annotation/anatomy-relation annot)  ;GO_0001764
					                         (map (fn [ar]
								       {(:go-annotation.anatomy-relation/text ar)
								       (pack-obj (:go-annotation.anatomy-relation/anatomy-term ar))}))
                                                                 (merge)
                                                                 (first))]
                                     {:evidence extensions})
                      :evidence_code {:evidence {:Date_last_updated (when-let [d (:go-annotation/date-last-updated annot)]
                                                                      (date/format-date3 (str d)))
                                                 :Contributed_by (when-let [contributed-by (:go-annotation/contributed-by annot)]
                                                                    (pack-obj contributed-by))
                                                 :Reference (when-let [reference (:go-annotation/reference annot)]
                                                             (pack-obj reference)) }
                                      :text (some->> (:go-annotation/go-code annot)
                                                     (:go-code/id))}
                      :species (when-let [species (:gene/species gene)]
				      (pack-obj species))
                      :with (some->> (:go-annotation/database annot)
                                     (map (fn [dh]
                                        (let [db-class (some->> dh :go-annotation.database/database :database/id)
                                              db-id (:go-annotation.database/text dh)]
                                       {:class db-class
                                        :id db-id
                                        :label (str/join ":" [db-class db-id])
                                        :dbt (some->> dh :go-annotation.database/database-field :database-field/id)}))))})))))

(defn genes [g]
  {:data (get-genes g)
   :description "genes annotated with this term"})

(defn transcript [g] ; non found in database
  {:data nil
   :description "transcripts annotated with this term"})

(defn homology-group [g] ; non found in database
  {:data nil
   :description "homology groups annotated with this term"})

(defn cds [g] ; non found in database
  {:data nil
   :description "CDS annotated with this term"})

(defn genes-summary [g]
  {:data (some->> (get-genes g)
                  (map (fn [annot]
                      (apply dissoc annot [:with :evidence_code :annot_id]))))
   :description "genes annotated with this term"})

(defn phenotype [g] ;GO:0005739
  {:data (some->> (:phenotype-info.go-term/_go-term g)
                  (map :phenotype-info/_go-term)
                  (map :rnai.phenotype/phenotype)
                  (map (fn [p]
                      {:description (->> p :phenotype/description :phenotype.description/text)
                       :phenotype_info (pack-obj p)})))
   :description "phenotypes annotated with this term"})

(defn cell [g] ; beleived to be contained in anatomy-term
  {:data nil
   :description "cells annotated with this term"})

(defn motif [g] ;GO_0070060
  {:data (some->> (:motif.go-term/_go-term g)
                  (map :motif/_go-term)
                  (map pack-obj))
   :description "motifs annotated with this term"})

(def widget
  {:name generic/name-field
;   :sequence sequence-field
   :anatomy_term anatomy-term
   :genes genes
;   :transcript transcript
;   :homology_group homology-group
;   :cds cds
   :genes_summary genes-summary
   :phenotype phenotype
;   :cell cell
   :motif motif})
