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

                                              :assay
                                              (some->> (:anatomy-function/assay af)
                                                       (map (fn [a]
                                                              (pace-utils/vmap
                                                                :text (:ao-code/id
                                                                        (:anatomy-function.assay/ao-code a))
                                                                :evidence (obj/get-evidence a)))))

                                              :phenotype
                                              (when-let [h (:anatomy-function/phenotype af)]
                                                (pace-utils/vmap
                                                  :text (pack-obj (:anatomy-function.phenotype/phenotype h))
                                                  :evidence (obj/get-evidence h)))

                                              :bp_inv
                                              (some->> (:anatomy-function/involved af)
                                                       (map (fn [bp]
                                                              (pace-utils/vmap
                                                                :text (pack-obj
                                                                        (:anatomy-function.involved/anatomy-term bp))
                                                                :evidence (obj/get-evidence bp)))))

                                              :type
                                              so-type})))
                             {:name (pack-obj gene)
                              :type so-type}))))
                  (flatten))
   :description "genes found within this topic"})

(def widget
  {:name generic/name-field
   :genes genes})
