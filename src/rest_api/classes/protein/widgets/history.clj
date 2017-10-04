(ns rest-api.classes.protein.widgets.history
  (:require
    [rest-api.classes.generic-fields :as generic]))

(defn history [p]
  {:data (when-let [hs (:protein/history p)]
           (for [h hs]
             {:version (:protein.history/int h)
              :prediction (when-let [prediction (:protein.history/text-c h)]
                            {:id prediction
                             :class "gene"})
              :event (:protein.history/text-b h)}))
   :description "curatorial history of the protein"})

(def widget
  {:name generic/name-field
   :history history})
