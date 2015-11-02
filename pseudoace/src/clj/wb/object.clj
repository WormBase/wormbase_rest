(ns wb.object
  (:use web.rest.object)
  (:require [datomic.api :as d :refer (q db entity)]))

;;
;; This namespace currently just re-exports some functionality from web.rest.object
;; with the added capability of working with raw entity IDs as well as entity-maps.
;;

(defn pack [db o]
  (pack-obj (entity db o)))

(defn evidence [db holder]
  (get-evidence (entity db holder)))
