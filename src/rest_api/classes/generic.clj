(ns rest-api.classes.generic
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [clojure.string :as str]))

(defn name-field [object]
    (obj/name-field object))

(defn xform-species-name
    "Transforms a `species-name` from the WB database into
      a name used to look up connection configuration to a sequence db."
        [species]
          (let  [species-name-parts (str/split species #" ")
                          g  (str/lower-case  (ffirst species-name-parts))
                                   species  (second species-name-parts)]
                (str/join "_"  [g species])))

(defn available-from [object] ;need to find example
  (let [k (first (filter #(= (name %) "id") (keys object)))
	role (namespace k)]
    {:data (when (= "Vancouver_fosmid"
                    (:method/id (:locatable/method object)))
	     {:label "GeneService"
	      :class "Geneservice_fosmids"})
     :description "availability of clones of the sequence"}))

(defn status [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
	role (namespace k)]
    {:data (if-let [sh ((keyword role "status") object)]
	     (:status/status sh))
    :description (str "current status of the " (str/capitalize role) ": "
                      ((keyword role "id") object) "if not Live or Valid")}))

(defn other-names [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
	role (namespace k)
        text-kw (keyword (str role ".other-name") "text")]
    {:data (when-let [other-names ((keyword role "other-name") object)]
            (for [other-name other-names]
              (if (contains? other-name text-kw)
                (text-kw other-name)
                other-name)))
     :description (str "other names that have been used to refer to " ((keyword role "id") object))}))

(defn laboratory [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
        role (namespace k)]
    {:data (when-let [labs (or ((keyword role "laboratory") object)
                               (or ((keyword role "location") object)
                                   [((keyword role "from-laboratory") object)]))]
             (for [lab labs]
               {:laboratory (pack-obj lab)
                :representative (when-let [reps (:laboratory/representative lab)]
                                  (for [rep reps] (pack-obj rep)))}))
     :description (str "the laboratory where the " role " was isolated, created, or named")}))

(defn description [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
        role (namespace k)]
    {:data ((keyword role "description") object)
     :description (str "description of the " role " " ((keyword role "id") object))}))

(defn taxonomy [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
        role (namespace k)]
  {:data (if-let [species (:species/id ((keyword role "species") object))]
           (let [[genus species] (str/split species #" ")]
             {:genus genus
              :species species}))
   :description "the genus and species of the current object"}))

(defn remarks [object]
  (let [k (first (filter #(= (name %) "id") (keys object)))
        data (when (some? k)
               (let  [role (namespace k)
                      remark-kw (keyword role "remark")]
                 (when-let [remark-holders (remark-kw object)]
                   (let [remark-text-kw (keyword (str role ".remark") "text")]
                     (for [remark-holder remark-holders]
                       {:text (remark-text-kw remark-holder)
                        :evidence (obj/get-evidence remark-holder)})))))]
       {:data (not-empty data)
        :description (if (some? k)
                       (str "Curatorial remarks for the " (namespace k))
                       "Can not determine class for entity and can not determine remarks")}))

(defn xrefs [object]
  (let [data
        (if-let [k (first (filter #(= (name %) "id") (keys object)))]
          (let [role (namespace k)
                kw-db-role (keyword role "database")
                ckw (str role ".database")]
            (reduce
              (fn [refs database]
                (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")
                      kw-field (keyword ckw "field")
                      kw-database-field (keyword ckw "database-field")
                      kw-accession (keyword ckw "accession")
                      kw-text (keyword ckw "text")
                      kw-db-id (keyword ckw "database")
                      kw-field-id (if (contains? database kw-database-field)
                                    kw-database-field
                                    kw-field)
                      kw-db-acc (if (contains? database kw-text)
                                  kw-text
                                  kw-accession)]
                  (update-in refs
                             [(:database/id (kw-db-id database))
                              (:database-field/id (kw-field-id database))
                              :ids]
                             pace-utils/conjv
                             (let [acc (kw-db-acc database)]
                               (if-not (nil? acc)
                                 (if-let [[_ rest] (match-accession acc)]
                                   rest
                                   acc))))))
              {}
              (kw-db-role object))))]
    {:data (not-empty data)
     :description  (str "external databases and IDs containing "
                        "additional information on the object")}))
