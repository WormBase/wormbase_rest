(ns rest-api.classes.generic-functions
  (:require
    [clojure.string :as str]))

(defn xform-species-name
  "Transforms a `species-name` from the WB database into
  a name used to look up connection configuration to a sequence db."
  [species]
  (if species
    (let [species-name-parts (str/split species #" ")
          g (str/lower-case (ffirst species-name-parts))
          species (second species-name-parts)]
      (str/join "_" [g species]))))

(defn certainty [h]
  (cond
    (contains? h :qualifier/certain)
    "Certain"

    (contains? h :qualifier/uncertain)
    "Uncertain"

    (contains? h :qualifier/partial)
    "Partial"))
