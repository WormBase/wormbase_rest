(ns rest-api.classes.generic-functions
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
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

(defn- is-cgc?  [strain]
  (some #(=  (->>  (:strain.location/laboratory %)
                  (:laboratory/id))
            "CGC")
        (:strain/location strain)))

(defn- strain-list  [strains]
  (seq  (map  (fn  [strain]
                (let  [tgs  (:transgene/_strain strain)]
                  (pace-utils/vassoc
                    (pack-obj "strain" strain)
                    :genotype  (:strain/genotype strain)
                    :transgenes  (pack-obj "transgene"  (first tgs)))))
             strains)))

(defn categorize-strains [strains]
     (pace-utils/vmap
      :carrying_gene_alone_and_cgc
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (is-cgc? %))
                           strains))

      :carrying_gene_alone
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (not (is-cgc? %)))
                           strains))

      :available_from_cgc
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (is-cgc? %))
                           strains))

      :others
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (not (is-cgc? %)))
                           strains))))
