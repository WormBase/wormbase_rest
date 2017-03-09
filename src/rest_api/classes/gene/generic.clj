(ns rest-api.classes.gene.generic
  (:require
   [rest-api.formatters.object :as obj]))

(defn name-field [gene]
  (obj/name-field gene))
