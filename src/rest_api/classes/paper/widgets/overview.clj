(ns rest-api.classes.paper.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn intext-citation [p]
  {:data nil
   :description "APA in-text citation"})

(defn status [p]
  {:data nil
   :description (str "current status of the Paper:" (:paper/id p) " if not Live or Valida")})

(defn keywords [p]
  {:data nil
   :description "Keywords related to the publication"})

(defn merged-into [p]
  {:data nil
   :description "the curatorial history of the gene"})

(defn is-wormbook-paper [p]
  {:data nil
   :description "Whether this is a publication in the WormBook"})

(defn publisher [p]
  {:data nil
   :description "Publisher of the publication"})

(defn journal [p]
  {:data nil
   :description "The journal the paper was published in"})

(defn doi [p]
  {:data nil
   :description "DOI of publication"})

(defn authors [p]
  {:data nil
   :description "The authors of the publication"})

(defn volume [p]
  {:data nil
   :description "The volume the paper was published in"})

(defn publication-type [p]
  {:data nil
   :description "Type of publication"})

(defn affiliation [p]
  {:data nil
   :description "Affiliations of the publication"})

(defn title [p]
  {:data nil
   :description "The title of the publication"})

(defn editors [p]
  {:data nil
   :description "Editor of publication"})

(defn abstract [p]
  {:data nil
   :description "The abstract of the publication"})

(defn pmid [p]
  {:data nil
   :description "PubMed ID of publication"})

(defn pages [p]
  {:data nil
   :description "The pages of the publication"})

(defn year [p]
  {:data nil
   :description "The year of publication"})

(def widget
  {:name generic/name-field
   :intext_citation intext-citation
   :status status
   :keywords keywords
   :merged_into merged-into
   :is_wormbaook_paper is-wormbook-paper
   :remarks generic/remarks
   :publisher publisher
   :journal journal
   :doi doi
   :authors authors
   :volume volume
   :publication_type publication-type
   :affiliation affiliation
   :title title
   :editors editors
   :abstact abstract
   :pmid pmid
   :pages pages
   :year year})
