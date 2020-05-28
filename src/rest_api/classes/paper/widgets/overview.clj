(ns rest-api.classes.paper.widgets.overview
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.paper.core :as paper-generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn intext-citation [p]
  {:data {:paper (:paper/id p)
          :citation (:label (pack-obj p))}
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

(defn- parse-initials [firstname]
  (str
    (str/join
      ". "
      (for [part (str/split firstname #"")]
        (get part 0)))
    "."))

(defn- create-initials [firstname]
  (str
    (str/join
      ". "
      (for [part (str/split firstname #" ")]
        (get part 0)))
    "."))

(defn- person-parsed-name [p]
  (let [lastname (:person/last-name p)
        firstname (:person/first-name p)
        initials (if (some? firstname)
                   (create-initials firstname))
        ]
    (if (some? initials)
      (str/join ", " [lastname initials])
      lastname)))

(defn authors [p]
  {:data (paper-generic/get-authors p)
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
  {:data (some-> (:paper/title p)
                 (str/replace #"\.$" ""))
   :description "The title of the publication"})

(defn editors [p]
  {:data (when-let [editors (:paper/editor p)]
           (flatten
             (for [editor-str (into [] editors)
                   :let [editor-str-parsed (str/replace
                                             (str/replace
                                               (str/replace editor-str #"\, and " ", ")
                                               #" and " ", ")
                                             #"\."  "")
                         editor-names (str/split editor-str-parsed #"\, ")]]
               (for [editor (str/split editor-str-parsed #"\, ")]
                 (let [name-parts (str/split editor #" ")
                       lastname (str/join " "
                                          (remove nil?
                                                  (for [name-part name-parts]
                                                    (if (every? #(Character/isUpperCase %) name-part)
                                                      nil
                                                      name-part))))
                       firstname (first
                                   (remove nil?
                                           (for [name-part name-parts]
                                             (when (every? #(Character/isUpperCase %) name-part)
                                               name-part))))
                       initials  (when (some? firstname)
                                   (parse-initials firstname))]
                   (if (some? initials)
                     (str/join ", " [lastname initials])
                     lastname))))))
   :description "Editor of publication"})

(defn abstract [p]
  {:data (:longtext/text (first (:paper/abstract p)))
   :description "The abstract of the publication"})

(defn pmid [p]
  {:data (first
           (remove
             nil?
             (for [d (:paper/database p)]
               (when (= (:database/id (:paper.database/database d))
                        "MEDLINE")
                 (:paper.database/accession d)))))
   :description "PubMed ID of publication"})

(defn pages [p]
  {:data (:paper/page p)
   :description "The pages of the publication"})

(defn year [p]
  {:data (when-let [date (:paper/publication-date p)]
           (first (str/split date #"-")))
   :description "The year of publication"})

(defn retracted-in [paper]
  {:data (->> paper
              (:paper/_retraction-of)
              (first)
              (pack-obj))
   :description "In which this paper is retracted, if applicable."})

(def widget
  {:name generic/name-field
   :intext_citation intext-citation
   :keywords keywords
   :merged_into merged-into
   :is_wormbook_paper is-wormbook-paper
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
   :abstract abstract
   :pmid pmid
   :pages pages
   :year year
   :retracted_in retracted-in})
