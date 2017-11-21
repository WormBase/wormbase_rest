(ns rest-api.classes.molecule.widgets.strains
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn affected-strains [m]
  {:data (some->> (:molecule/affects-phenotype-of-strain m)
                  (map (fn [h]
                         (let [obj
                               {:affected
                                (pack-obj (:molecule.affects-phenotype-of-strain/strain h))

                                :phenotype
                                (pack-obj (:molecule.affects-phenotype-of-strain/phenotype h))}]
                           (if-let [ev (obj/get-evidence h)]
                             (conj obj {:evidence ev})
                             obj)))))
   :description "strain affected by molecule"})

(def widget
  {:name generic/name-field
   :affected_strains affected-strains})
