(ns rest-api.classes.go-term.widgets.associations
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-field [g]
  {:data nil
   :description "sequences annotated with this term"})

(defn anatomy-term [g]
  {:data nil
   :description "anatomy terms annotated with this term"})

(defn genes [g]
  {:data (keys g)
   :d (:db/id g)
   :description "genes annotated with this term"})

(defn transcript [g]
  {:data nil
   :description "transcripts annotated with this term"})

(defn homology-group [g]
  {:data nil
   :description "homology groups annotated with this term"})

(defn cds [g]
  {:data nil
   :description "CDS annotated with this term"})

(defn genes-summary [g]
  {:data nil
   :description "genes annotated with this term"})

(defn phenotype [g]
  {:data nil
   :description "phenotypes annotated with this term"})

(defn cell [g]
  {:data nil
   :description "cells annotated with this term"})

(defn motif [g]
  {:data nil
   :description "motifs annotated with this term"})

(def widget
  {:name generic/name-field
   :sequence sequence-field
   :anatomy_term anatomy-term
   :genes genes
   :transcript transcript
   :homology_group homology-group
   :cds cds
   :genes_summary genes-summary
   :phenotype phenotype
   :cell cell
   :motif motif})
