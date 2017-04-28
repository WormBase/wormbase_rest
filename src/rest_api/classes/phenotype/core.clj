(ns rest-api.classes.phenotype.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]


(defn- phenotype-table [db gene not-observed?]
  (let [var-phenos (into {} (d/q (if not-observed?
                                   q-gene-var-not-pheno
                                   q-gene-var-pheno)
                                 db gene))
        rnai-phenos (into {} (d/q (if not-observed?
                                    q-gene-rnai-not-pheno
                                    q-gene-rnai-pheno)
                                  db gene))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->>
      (flatten
	(for [pid phenos
	      :let [pheno (d/entity db pid)]]
	  (let [pcs (get-pato-combinations-gene
		      db
		      pid
		      rnai-phenos
		      var-phenos
		      not-observed?)]
	    (if (nil? pcs)
	      (phenotype-table-entity db
				      pheno
				      nil
				      nil
				      pid
				      var-phenos
				      rnai-phenos
				      not-observed?)
	      (for [[pato-key entity] pcs]
		(phenotype-table-entity db
					pheno
					pato-key
					entity
					pid
					var-phenos
					rnai-phenos
					not-observed?))))))
      (into []))))

