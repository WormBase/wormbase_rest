(ns rest-api.classes.variation.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn corresponding-gene [v]
  {:data (when-let [ghs (:variation/gene v)]
           (for [gh ghs
                 :let [gene (:variation.gene/gene gh)]]
             (pack-obj gene)))
   :description "gene in which this variation is found (if any)"})

(defn evidence [v]
  {:data (when-let [evidence (:variation/evidence v)]
           {:text ""
            :evidence (obj/get-evidence evidence)})
   :description "Evidence for this Variation"})

(defn variation-type [v]
  {:data
   {:general_class (not-empty
                     (remove
                       nil?
                       [(when (:variation/engineered-allele v)
                          "Engineered Allele")
                        (when (:variation/allele v)
                          "Allele")
                        (when (:variation/snp v)
                          "SNP")
                        (when (:variation/confirmed-snp v)
                          "Confirmed SNP")
                        (when (:variation/predicted-snp v)
                          "Predicted SNP")]))
    :physical_class (if (or (contains? v :variation/transposon-insertion)
                              (= (:method/id (:location/method v))
                                 "Transposon_insertion"))
                      "Transposon insertion"
                      (cond
                        (contains? v :variation/substitution)
                        "Substitution"

                        (contains? v :variation/insertion)
                        "Insertion"

                        (contains? v :variation/deletion)
                        "Deletion"

                        (contains? v :variatino/tandem-duplication)
                        "Tandem Duplication"))}
   :k (keys v)
   :description "the general type of the variation"})

(def widget
  {:name generic/name-field
   :status generic/status
   :taxonomy generic/taxonomy
   :corresponding_gene corresponding-gene
   :evidence evidence
   :variation_type variation-type
   :remarks generic/remarks
   :other_names generic/other-names})
