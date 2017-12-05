(ns rest-api.classes.molecule.widgets.affected
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn- phenotype-evidence-filter [evidence]
  (dissoc
    evidence
    :Remark
    :Affected_by_molecule
    :Dominant
    :Recessive
    :Variation_effect
    :Semi_dominant))

(defn- transgene-phenotype [transgene phenotype]
  (some->> (:transgene/phenotype transgene)
           (filter #(= phenotype
                       (:transgene.phenotype/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:transgene.phenotype/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder transgene phenotype)]
                    (some->> (or
                               (not-empty
                                 (map :phenotype-info.caused-by-gene/gene
                                      (:phenotype-info/caused-by-gene pheno-holder)))
                               (:phenotype-info/caused-by-other pheno-holder))
                             (map (fn [gene]
                                    (pace-utils/vmap
                                      "affected"
                                      (let [affected-obj (update
                                                           (pack-obj transgene)
                                                           :label #(str % " [Transgene]"))]
                                        {:text [affected-obj
                                                (first (:Remark evidence))]
                                         :evidence (phenotype-evidence-filter evidence)})

                                      "phenotype"
                                      (if-let [ev (obj/get-evidence pheno-holder)]
                                        {:text phenotype-obj
                                         :evidence ev}
                                        phenotype-obj)

                                      "affected_gene"
                                      (if-let [label (:phenotype-info.caused-by-other/text gene)]
                                        {:id label
                                         :label label
                                         :class "text"
                                         :taxonomy "all"}
                                        (pack-obj gene)))))))))))

(defn- transgene-phenotype-not-observed [transgene phenotype]
  (some->> (:transgene/phenotype-not-observed transgene)
           (filter #(= phenotype
                       (:transgene.phenotype-not-observed/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:transgene.phenotype-not-observed/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder transgene phenotype)]
                    (some->> (or
                               (not-empty
                                 (map :phenotype-info.caused-by-gene/gene
                                      (:phenotype-info/caused-by-gene pheno-holder)))
                                 (:phenotype-info/caused-by-other pheno-holder))
                               (map (fn [gene]
                                      (pace-utils/vmap
                                        "affected"
                                        (let [affected-obj (update
                                                             (pack-obj transgene)
                                                             :label #(str % " [Transgene]"))]
                                          {:text [affected-obj
                                                  (first (:Remark evidence))]
                                           :evidence (phenotype-evidence-filter evidence)})

                                        "phenotype_not_observed"
                                        (if-let [ev (obj/get-evidence pheno-holder)]
                                          {:text phenotype-obj
                                           :evidence ev}
                                          phenotype-obj)

                                        "affected_gene"
                                        (if-let [label (:phenotype-info.caused-by-other/text gene)]
                                          {:id label
                                           :label label
                                           :class "text"
                                           :taxonomy "all"}
                                          (pack-obj gene)))))))))))

(defn- rnai-phenotype [rnai phenotype gene]
  (some->> (:rnai/phenotype rnai)
           (filter #(= phenotype
                       (:rnai.phenotype/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:rnai.phenotype/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder rnai phenotype)]
                    (pace-utils/vmap
                      "affected"
                      (let [affected-obj (update
                                           (pack-obj rnai)
                                           :label #(str % " [RNAi]"))]
                        {:text [affected-obj
                                (first (:Remark evidence))]
                         :evidence (phenotype-evidence-filter evidence)})

                      "phenotype"
                      (if-let [ev (obj/get-evidence pheno-holder)]
                        {:text phenotype-obj
                         :evidence ev}
                        phenotype-obj)

                      "affected_gene"
                      (pack-obj gene)))))))

(defn- rnai-phenotype-not-observed [rnai phenotype gene]
  (some->> (:rnai/phenotype-not-observed rnai)
           (filter #(= phenotype
                       (:rnai.phenotype-not-observed/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:rnai.phenotype-not-observed/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder rnai phenotype)]
                    (pace-utils/vmap
                      "affected"
                      (let [affected-obj (update
                                           (pack-obj rnai)
                                           :label #(str % " [RNAi]"))]
                        {:text [affected-obj
                                (first (:Remark evidence))]
                         :evidence (phenotype-evidence-filter evidence)})

                      "phenotype_not_observed"
                      (if-let [ev (obj/get-evidence pheno-holder)]
                        {:text phenotype-obj
                         :evidence ev}
                        phenotype-obj)

                      "affected_gene"
                      (pack-obj gene)))))))

(defn- variation-phenotype [variation phenotype gene]
  (some->> (:variation/phenotype variation)
           (filter #(= phenotype
                       (:variation.phenotype/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:variation.phenotype/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder variation phenotype)]
                    (pace-utils/vmap
                      "affected"
                      (let [affected-obj (update
                                           (pack-obj variation)
                                           :label #(str % " [Variation]"))]
                        {:text [affected-obj
                                (first (:Remark evidence))]
                         :evidence (phenotype-evidence-filter evidence)})

                      "phenotype"
                      (if-let [ev (obj/get-evidence pheno-holder)]
                        {:text phenotype-obj
                         :evidence ev}
                        phenotype-obj)

                      "affected_gene"
                      (pack-obj gene)))))))

(defn- variation-phenotype-not-observed [variation phenotype gene]
  (some->> (:variation/phenotype-not-observed variation)
           (filter #(= phenotype
                       (:variation.phenotype-not-observed/phenotype %)))
           (map (fn [pheno-holder]
                  (let [phenotype (:variation.phenotype-not-observed/phenotype pheno-holder)
                        phenotype-obj (pack-obj phenotype)
                        evidence (phenotype-core/get-evidence pheno-holder variation phenotype)]
                    (pace-utils/vmap
                      "affected"
                      (let [affected-obj (update
                                           (pack-obj variation)
                                           :label #(str % " [Variation]"))]
                        {:text [affected-obj
                                (first (:Remark evidence))]
                         :evidence (phenotype-evidence-filter evidence)})

                      "phenotype_not_observed"
                      (if-let [ev (obj/get-evidence pheno-holder)]
                        {:text phenotype-obj
                         :evidence ev}
                        phenotype-obj)

                      "affected_gene"
                      (pack-obj gene)))))))

(defn affected-genes [m]
  {:data (remove
           nil?
           (flatten
             (conj
               (some->> (:molecule/affects-phenotype-of-rnai m)
                        (map (fn [holder]
                               (let [rnai (:molecule.affects-phenotype-of-rnai/rnai holder)
                                     phenotype (:molecule.affects-phenotype-of-rnai/phenotype holder)]
                                 (some->> (:rnai/gene rnai)
                                          (map :rnai.gene/gene)
                                          (map (fn [gene]
                                                 (conj
                                                   (rnai-phenotype rnai phenotype gene)
                                                   (rnai-phenotype-not-observed rnai phenotype gene))))))))
                        (into [])
                        (flatten)
                        (remove nil?))
               (some->> (:molecule/affects-phenotype-of-transgene m)
                        (map (fn [holder]
                               (let [transgene (:molecule.affects-phenotype-of-transgene/transgene holder)
                                     phenotype (:molecule.affects-phenotype-of-transgene/phenotype holder)]
                                 (conj
                                   (transgene-phenotype transgene phenotype)
                                   (transgene-phenotype-not-observed transgene phenotype)))))
                        (into [])
                        (flatten)
                        (remove nil?))
               (some->> (:molecule/affects-phenotype-of-variation m)
                        (map (fn [holder]
                               (let [variation (:molecule.affects-phenotype-of-variation/variation holder)
                                     phenotype (:molecule.affects-phenotype-of-variation/phenotype holder)]
                                 (some->> (:variation/gene variation)
                                          (map :variation.gene/gene)
                                          (map (fn [gene]
                                                 (conj
                                                   (variation-phenotype variation phenotype gene)
                                                   (variation-phenotype-not-observed variation phenotype gene))))))))
                        (into [])
                        (flatten)
                        (remove nil?)))))
   :description "genes affected by the molecule"})

(def widget
  {:name generic/name-field
   :affected_genes affected-genes})
