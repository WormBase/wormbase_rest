(ns rest-api.classes.paper.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn intext-citation [p]
  {:data {:paper (:paper/id p)
          :citation nil ; will take work but easy
          }
   :description "APA in-text citation"})

(defn keywords [p]
  {:data (:paper/keyword p)
   :description "Keywords related to the publication"})

(defn merged-into [p]
  {:data (when-let [paper (:paper/merged-into p)]
           (pack-obj paper))
   :description "the curatorial history of the gene"})

(defn is-wormbook-paper [p]
  {:data (if (= "WormBook" (:paper.type/type (first (:paper/type p)))) 1 0)
   :description "Whether this is a publication in the WormBook"})

(defn publisher [p]
  {:data (:paper/publisher p)
   :description "Publisher of the publication"})

(defn journal [p]
  {:data (:paper/journal p)
   :description "The journal the paper was published in"})

(defn doi [p]
  {:data (when-let [names (:paper/name p)]
           (first
             (remove
               nil?
               (for [n names]
                 (second (re-matches #"^(?:doi[^/]*)?(10\.[^/]+.+)$" n))))))
   :description "DOI of publication"})

(defn authors [p]
  {:data (when-let [hs (:paper/author p)]
           (for [h hs]
             (pack-obj (:paper.author/author h))))
   :description "The authors of the publication"})

(defn volume [p]
  {:data (:paper/volume p)
   :description "The volume the paper was published in"})

(defn publication-type [p]
  {:data (when-let [hs (:paper/type p)]
           (for [h hs]
             (when-let [t (:paper.type/type h)]
               (str/capitalize (str/replace (name t) #"-" "_")))))
   :description "Type of publication"})

(defn affiliation [p]
  {:data (:paper/affiliation p)
   :description "Affiliations of the publication"})

(defn title [p]
  {:data (:paper/title p)
   :description "The title of the publication"})

(defn editors [p] ; needs more work. have to parse response
  {:data (:paper/editor p) ; WBPaper00035863
   :description "Editor of publication"})

(defn abstract [p]
  {:data (:longtext/text (first (:paper/abstract p)))
   :description "The abstract of the publication"})

(defn pmid [p]
  {:data nil ; need to check database refs for if there is a PMID in list
   :description "PubMed ID of publication"})

(defn pages [p]
  {:data (:paper/page p)
   :description "The pages of the publication"})

(defn year [p]
  {:data (when-let [date (:paper/publication-date p)]
           (first (str/split date #"-")))
   :description "The year of publication"})

(def widget
  {:name generic/name-field
   :intext_citation intext-citation
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
