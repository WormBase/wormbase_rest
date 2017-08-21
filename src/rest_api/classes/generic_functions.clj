(ns rest-api.classes.generic-functions
  (:require
    [clojure.string :as str]))

(defn xform-species-name
  "Transforms a `species-name` from the WB database into
  a name used to look up connection configuration to a sequence db."
  [species]
  (let  [species-name-parts (str/split species #" ")
         g  (str/lower-case  (ffirst species-name-parts))
         species  (second species-name-parts)]
    (str/join "_"  [g species])))
