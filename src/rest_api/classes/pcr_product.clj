(ns rest-api.classes.pcr-product
  (:require
    [rest-api.classes.pcr-product.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "pcr-product"
   :uri-name "pcr_oligo"
   :widget
   {:overview overview/widget}})
