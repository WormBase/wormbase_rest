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

; this is for sorting gene names
(defn compare-gene-name [a b]
  (let [[a1 a2] (str/split (str/lower-case (:label a)) #"-")
        [b1 b2] (str/split (str/lower-case (:label b)) #"-")
        a2-padded (if (= (count a2) 1) (str "0" a2) a2)
        b2-padded (if (= (count b2) 1) (str "0" b2) b2)
        a-padded (str a1 a2-padded)
        b-padded (str b1 b2-padded)]
    (compare a-padded b-padded)))

(defn- is-cgc? [strain]
  (some #(= (->> (:strain.location/laboratory %)
                 (:laboratory/id))
            "CGC")
        (:strain/location strain)))

(defn- strain-list [strains]
  (seq (map (fn [strain]
              (let [tgs (:transgene/_strain strain)]
                (pace-utils/vassoc
                  (pack-obj "strain" strain)
                  :genotype (:strain/genotype strain)
                  :transgenes (pack-obj "transgene" (first tgs)))))
            strains)))

(defn- include-strain-and? [cgc-pred]
  #(and (not (seq (:transgene/_strain %)))
	(= (count (:gene/_strain %)) 1)
	(cgc-pred %)))

(defn- include-strain-or? [cgc-pred]
  #(and (or (seq (:transgene/_strain %))
	    (not= (count (:gene/_strain %)) 1))
	(cgc-pred %)))

(defn categorize-strains [strains]
  (pace-utils/vmap
    :carrying_gene_alone_and_cgc
    (->> strains (filter (include-strain-and? is-cgc?)) strain-list)

    :carrying_gene_alone
    (->> strains (filter (include-strain-and? (complement is-cgc?))) strain-list)

    :available_from_cgc
    (->> strains (filter (include-strain-or? is-cgc?)) strain-list)

    :others
    (->> strains (filter (include-strain-or? (complement is-cgc?))) strain-list)))
