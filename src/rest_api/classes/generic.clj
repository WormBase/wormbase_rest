(ns rest-api.classes.generic
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [clojure.string :as str]))

(defn xrefs [role obj]
  {:data
   (let [ckw (partial (str/join "." [role "database"]))]
     (reduce
       (fn [refs db]
         (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")]
           (update-in refs
                      [(:database/id ((ckw "database") db))
                       (:database-field/id ((ckw "field") db))
                       :ids]
                      pace-utils/conjv
                      (let [acc ((ckw "accession") db)]
                        (if-let [[_ rest] (match-accession acc)]
                          rest
                          acc)))))
       {}
       ((keyword role "database") obj)))
   :description  (str "external databases and IDs containing "
                      "additional information on the object")})

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
