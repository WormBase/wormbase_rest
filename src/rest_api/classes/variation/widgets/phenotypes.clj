(ns rest-api.classes.variation.widgets.phenotypes
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.classes.variation.generic :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- phenotype-data [vh phenotype]
  (let [remark (if-let [prhs (:phenotype/remark phenotype)]
                 (for [prh prhs
                       :let [remark (:phenotype.remark/text prh)]]
                   remark))
        description (if (contains? phenotype :phenotype/description)
                      (:phenotype.description/text
                        (:phenotype/description phenotype)))
        evidence {:evidence
                  {:Phenotype_assay (if-let [pah (:phenotype/assay phenotype)]
                                     (pack-obj (:phenotype.assay/text pah)))
                  :Curator (if-let [chs (:phenotype-info/curator-confirmed vh)]
                             (for [ch chs]
                               (pack-obj ch)))
                  :Paper_evidence (if-let [phs (:phenotype-info/paper-evidence vh)]
                             (for [ph phs]
                               (pack-obj ph)))
                  :remark (if-let  [pirhs  (:phenotype-info/remark vh)]
                            (for  [pirh pirhs
                                   :let [remark  (:phenotype-info.remark/text pirh)]]
                              remark))}}
        ]
    {:entities_affected nil
     :evidence evidence
     :keysvh (keys vh)
     :keysp (keys phenotype)
     :phenotype nil
     :remark remark
     :description description}))

(defn phenotypes [variation]
  (let [data (if-let [vhs (:variation/phenotype variation)]
               (for [vh vhs
                     :let [phenotype (:variation.phenotype/phenotype vh)]]
                 (phenotype-data vh phenotype)))]
    {:data data
     :description "phenotypes annotated with this term"}))

(defn phenotypes_not_observed [variation]
  {:data nil
   :description "phenotypes NOT observed or associated with this object"})

(def widget
  {:name generic/name-field
   :phenotypes phenotypes
   :phenotypes_not_observed phenotypes_not_observed})
