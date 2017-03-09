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
  {:data
   (if-let [k (first (filter #(= (name %) "id") (keys object)))]
     (let [role (namespace k)
           ckw (partial (str/join "." [role "database"]))]
       (reduce
	 (fn [refs db]
	   (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")]
	     (update-in refs
			[(:database/id ((keyword ckw "database") db))
			 (:database-field/id ((keyword ckw "field") db))
			 :ids]
			pace-utils/conjv
			(let [acc ((keyword ckw "accession") db)]
			  (if-let [[_ rest] (match-accession acc)]
			    rest
			    acc)))))
	 {}
	 ((keyword role "database") object))))
   :description  (str "external databases and IDs containing "
		      "additional information on the object")})
