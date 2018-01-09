(ns rest-api.classes.wbprocess.widgets.genes
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn genes [p]
  {:data (some->> (:wbprocess/gene p)
                  (map :wbprocess.gene/gene)
                  (map (fn [gene]
                         (let [so-type (when-let [t (:so-term/name
                                                      (:gene/biotype gene))]
                                         (obj/humanize-ident t))]
                           (if-let [afh (:anatomy-function.gene/_gene gene)]
                             (some->> afh
                                      (map :anatomy-function/_gene)
                                      (map (fn [af]
                                             {:reference
                                              (when-let [reference (:anatomy-function/reference af)]
                                                (pack-obj reference))

                                              :name
                                              (pack-obj gene)

                                              :assay
                                              (some->> (:anatomy-function/assay af)
                                                       (map (fn [a]
                                                              (let [text (:ao-code/id
                                                                        (:anatomy-function.assay/ao-code a))
                                                                    ev (obj/get-evidence a)]
                                                                (if (some? ev)
                                                                  {:text text
                                                                   :evidence ev}
                                                                  text)))))

                                              :phenotype
                                              (when-let [h (:anatomy-function/phenotype af)]
                                                (let [text-obj (pack-obj (:anatomy-function.phenotype/phenotype h))
                                                      ev (obj/get-evidence h)]
                                                  (if (some? ev)
                                                    {:text text-obj
                                                     :evidence ev}
                                                    text-obj)))

                                              :bp_inv
                                              (some->> (:anatomy-function/involved af)
                                                       (map (fn [bp]
                                                              (pace-utils/vmap
                                                                :text (pack-obj
                                                                        (:anatomy-function.involved/anatomy-term bp))
                                                                :evidence (obj/get-evidence bp)))))

                                              :type
                                              (if (= so-type "Protein coding gene")
                                                "Protein coding"
                                                so-type)})))
                             {:name (pack-obj gene)
                              :type (if (= so-type "Protein coding gene")
                                      "Protein coding"
                                      so-type)}))))
                  (flatten))
   :description "genes found within this topic"})

(def widget
  {:name generic/name-field
   :genes genes})
