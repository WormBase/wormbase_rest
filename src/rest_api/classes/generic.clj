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

(defn xrefs [object]
  (let [data
        (if-let [k (first (filter #(= (name %) "id") (keys object)))]
          (let [role (namespace k)
                ckw (partial (str/join "." [role "database"]))]
            (reduce
              (fn [refs database]
                (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")
                      kw-field (keyword ckw "field")
                      kw-database-field (keyword ckw "database-field")
                      kw-accession (keyword ckw "accession")
                      kw-text (keyword ckw "text")]
                  (update-in refs
                             [(:database/id ((keyword ckw "database") database))
                              (:database-field/id ((if (contains? database kw-database-field)
                                                     kw-database-field
                                                     kw-field)
                                                   database))
                              :ids]
                             pace-utils/conjv
                             (let [acc ((if (contains? database kw-text)
                                          kw-text
                                          kw-accession)
                                        database)]
                               (if (nil? acc)
                                 nil
                                 (if-let [[_ rest] (match-accession acc)]
                                   rest
                                   acc))))))
              {}
              ((keyword role "database") object))))]
    {:data (not-empty data)
     :description  (str "external databases and IDs containing "
                        "additional information on the object")}))
