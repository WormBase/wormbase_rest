(ns rest-api.classes.operon.widgets.structure
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn structure [o]
  {:data (some->> (:operon/contains-gene o)
                  (map (fn [h]
                         {:splice_info
                          (some->> (pace-utils/vmap
                                     "SL1"
                                     (when-let [tsl (:trans-splice-leader/sl1 h)]
                                       {:text "SL1"
                                        :evidence (obj/get-evidence tsl)})

                                     "SL2"
                                     (when-let [tsl (:trans-splice-leader/sl2 h)]
                                       {:text "SL2"
                                        :evidence (obj/get-evidence tsl)})

                                     "Microarray"
                                     (when-let [tsl (:trans-splice-leader/microarray h)]
                                       {:text "Microarray"
                                        :evidence (obj/get-evidence tsl)})

                                     "Inferred"
                                     (when-let [tsl (:trans-splice-leader/inferred h)]
                                       {:text "Inferred"
                                        :evidence (obj/get-evidence tsl)}))
                                   (vals))

                          :gene_info
                          (pack-obj (:operon.contains-gene/gene h))})))
   :description "structure information for this operon"})

(def widget
    {:name generic/name-field
     :structure structure})
