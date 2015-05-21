(ns web.curate
  (:use ring.middleware.keyword-params)
  (:require [compojure.core :refer (routes GET POST context wrap-routes)]
            [web.curate.gene :as gene]))

(def curation-forms
 (wrap-routes
  (routes
   (GET "/gene/query" req (gene/query-gene req))
   (GET "/gene/new"   req (gene/new-gene req))
   (POST "/gene/new"   req (gene/new-gene req)))
  wrap-keyword-params))
