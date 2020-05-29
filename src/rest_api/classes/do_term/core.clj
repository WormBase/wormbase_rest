(ns rest-api.classes.do-term.core
  (:require
   [clojure.string :as str]
   [rest-api.classes.strain.core :as strain]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- get-model-genotype [model]
  (let [strain
        (->> (:disease-model-annotation/modeled-by-strain model)
             (first)
             (:disease-model-annotation.modeled-by-strain/strain))

        variations
        (concat (->> (:disease-model-annotation/modeled-by-variation model)
                     (map :disease-model-annotation.modeled-by-variation/variation))
                (:disease-model-annotation/interacting-variation model))

        variation-gene-fn
        (fn [var]
          (->> (:variation/gene var)
               (first)
               (:variation.gene/gene)))

        variation-genes
        (map variation-gene-fn variations)

        transgenes
        (->> (:disease-model-annotation/modeled-by-transgene model)
             (map :disease-model-annotation.modeled-by-transgene/transgene)
             (distinct))

        genes
        (concat (->> (:disease-model-annotation/modeled-by-disease-relevant-gene model)
                     (map :disease-model-annotation.modeled-by-disease-relevant-gene/gene))
                (:disease-model-annotation/interacting-gene model))

        non-redundant-genes
        (if (and (not (:disease-model-annotation/modeled-by-strain model))
                 (not (:disease-model-annotation/modeled-by-variation model))
                 (not (:disease-model-annotation/modeled-by-transgene model)))
          genes)

        ;; previous rule to exclude certain genes
        ;; non-redundant-genes
        ;; (let [variation-gene-set (set variation-genes)
        ;;       strain-gene-set (set (:gene/_strain strain))]
        ;;   (filter (fn [gene]
        ;;             (and (not (variation-gene-set gene))
        ;;                  (not (strain-gene-set gene))))
        ;;           genes))

        ]

    (let [strain-genotype (strain/get-genotype strain)
          {strain-str :str
           strain-data :data} (strain/get-genotype strain)
          non-strain-entities (concat variations transgenes non-redundant-genes)
          entities (if (and strain (not strain-genotype))
                     (cons strain non-strain-entities)
                     non-strain-entities)
          entities-str (->> entities
                            (map (fn [e]
                                   (cond (:variation/id e)
                                         (let [gene (variation-gene-fn e)]
                                           (format "%s(%s)" (:label (pack-obj gene)) (:label (pack-obj e))))

                                         (:transgene/summary e)
                                         (->> e
                                              (:transgene/summary)
                                              (:transgene.summary/text))

                                         :else (:label (pack-obj e)))))
                            (cons (when strain-str
                                    (str/replace strain-str #"\.$" "")))
                            (filter identity)
                            (clojure.string/join "; "))]
      {:str (str/replace entities-str #"\s+" "&nbsp;")
       :data (reduce (fn [result obj]
                       (assoc result (:label (pack-obj obj)) (pack-obj obj)))
                     strain-data
                     (concat entities variation-genes))
       :entities (->> entities
                      (cons strain)
                      (filter identity)
                      (map pack-obj))})))

(defn process-disease-models [models]
  (->> models
       (map (fn [model]
              {:disease_term (pack-obj (:disease-model-annotation/disease-term model))
               :genotype {:genotype (get-model-genotype model)}
               :genetic_entity (:entities(get-model-genotype model))
               :association_type (obj/humanize-ident (:disease-model-annotation/association-type model))
               :evidence_code (->> (:disease-model-annotation/evidence-code model)
                                   (map (fn [evidence-code]
                                          {:text (:go-code/id evidence-code)
                                           :evidence {:description (:go-code/description evidence-code)
                                                      :annotation (:disease-model-annotation/id model)}}))
                                   (seq))
               :experimental_condition (->> [:disease-model-annotation/inducing-chemical
                                             :disease-model-annotation/inducing-agent]
                                            (reduce (fn [result attribute]
                                                      (concat result (attribute model)))
                                                    [])
                                            (map pack-obj)
                                            (seq))
               :modifier (let [modifiers (->> [:disease-model-annotation/modifier-transgene
                                               :disease-model-annotation/modifier-variation
                                               :disease-model-annotation/modifier-strain
                                               :disease-model-annotation/modifier-gene
                                               :disease-model-annotation/modifier-molecule
                                               :disease-model-annotation/other-modifier]
                                              (reduce (fn [result attribute]
                                                        (concat result (attribute model)))
                                                      [])
                                              (map pack-obj)
                                              (seq))]
                           (if modifiers
                             {:text modifiers
                              :evidence {:modifier_association_type (obj/humanize-ident (:disease-model-annotation/modifier-association-type model))}}))
               :description (->> model
                                 (:disease-model-annotation/disease-model-description)
                                 (first))
               :reference (pack-obj (:disease-model-annotation/paper-evidence model))}))
       (seq)))
