(ns rest-api.classes.phenotype.widgets.transgene
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn transgene-info [p obs]
  (when-let [holders (if obs
                       (:transgene.phenotype/_phenotype p)
                       (:transgene.phenotype-not-observed/_phenotype p))]
    (for [holder holders
          :let [transgene (if obs
                            (:transgene/_phenotype holder)
                            (:transgene/_phenotype-not-observed holder))]]
      {:transgene (pack-obj transgene)
       :remark (when-let [rh (first (:phenotype-info/remark holder))]
                 {:text (:phenotype-info.remark/text rh)
                  :evidence (obj/get-evidence rh)})
       :caused_by (not-empty
                    (remove
                      nil?
                      (flatten
                        (conj
                          (when-let [cbs (:phenotype-info/caused-by holder)]
                            (for [cb cbs]
                              {:text (:phenotype-info.caused-by/text cb)
                               :evidence (obj/get-evidence cb)}))
                          (when-let [cbos (:phenotype-info/caused-by-other holder)]
                            (for [cbo cbos]
                              {:text (:phenotype-info.caused-by-other/text cbo)
                               :evidence (obj/get-evidence cbo)}))))))})))

(defn transgene [p]
  {:data (transgene-info p true)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(defn transgene-not [p]
  {:data (transgene-info p false)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(def widget
  {:transgene transgene
   :transgene_not transgene-not
   :name generic/name-field})
