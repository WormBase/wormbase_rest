(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d :refer (db q touch entity)]
            [cheshire.core :as c :refer (generate-string)]
            [clojure.string :as str]))


(defn wrap-field [field-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (field-fn (d/entity db [(keyword wbid-field) id])))))

;; (defn wrap-widget [fields-map]
;;   (reduce (fn [new-fields-map [field-key field-fn]]
;;             (assoc new-fields-map
;;                    field-key
;;                    (wrap-field field-fn)))
;;           {}
;;           fields-map))

(defn wrap-widget [widget-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (widget-fn (d/entity db [(keyword wbid-field) id])))))

(defmacro def-rest-widget
  "def-rest-widget is synonymous to defn"
  [name [binding] & body]
  `(defn ~name [~binding]
     (do ~@body)))



;; (defmacro def-rest-widget
;;   "Define a handler for a rest widget endpoint.  `body` is executed with `gene-binding`
;;    will bound to the gene's entity-map, and should return a map of field values."
;;   [name [gene-binding] & body]
;;   `(defn ~name [db# class# id#]
;;      (if-let [~gene-binding (entity db# [:gene/id id#])]
;;        {:status 200
;;         :content-type "application/json"
;;         :body (generate-string
;;                {:class "gene"
;;                 :name id#
;;                 :uri uri#
;;                 :fields (do ~@body)}
;;                {:pretty true})}
;;        {:status 404
;;         :content-type "text/plain"
;;         :body (format "Can't find gene %s" id#)})
;;      ))



;; (defmacro def-rest-widget
;;   "Define a handler for a rest widget endpoint.  `body` is executed with `gene-binding`
;;    will bound to the gene's entity-map, and should return a map of field values."
;;   [name [gene-binding] & body]
;;   `(defn ~name [db# class# id#]
;;      (if-let [~gene-binding (entity db# [:gene/id id#])]
;;        {:status 200
;;         :content-type "application/json"
;;         :body (generate-string
;;                {:class "gene"
;;                 :name id#
;;                 :uri uri#
;;                 :fields (do ~@body)}
;;                {:pretty true})}
;;        {:status 404
;;         :content-type "text/plain"
;;         :body (format "Can't find gene %s" id#)})
;;      ))
