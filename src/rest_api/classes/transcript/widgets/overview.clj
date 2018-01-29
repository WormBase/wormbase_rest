(ns rest-api.classes.transcript.widgets.overview
  (:require
    [rest-api.classes.generic-fields :as generic]))

(defn sequence-type [t]
  {:data (let [id (:transcript/id t)]
           (cond
             (re-matches #"^cb\d+\.fpc\d+$/" id)
             "C. briggsae draft contig"

             (re-matches #"(\b|_)GAP(\b|_)" id)
             "gap in genomic sequence -- for accounting purposes"

             (= (:method/id (:locatable/method t)) "Vancouver_fosmid")
             "genomic -- fosmid"

             nil
             "WormBase transcript"

             nil
             "predicted coding sequence"

             ; cdna

             (= (:method/id (:locatable/method t)) "EST_nematode")
             "non-Elegans nematode EST sequence"

             ;is merged


             (= (:method/id (:locatable/method t)) "NDB")
             "GenBank/EMBL Entry"

             :else
             "unknown"))
   :description "the general type of the sequence"})

(defn feature [t]
  {:data nil ; can't find example
   :d (:db/id t)
   :description "feature associated with this transcript"})

(def widget
  {:name generic/name-field
   :available_from generic/available-from
   :laboratory generic/laboratory
   :taxonomy generic/taxonomy
   :sequnece_type sequence-type
   :description generic/description
   :feature feature
   :identity generic/identity-field
   :remarks generic/remarks
   :method generic/method
   :corresponding_all generic/corresponding-all})
