(ns datomic-rest-api.helpers.species
  (:require [clojure.string :as str]))

(defn parse-species-name [species]
  (let [species_name_parts (str/split species #" ")
        g  (str/lower-case (first (first species_name_parts)))
        species (second species_name_parts)]
    (str/join "_" [g species])))
