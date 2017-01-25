(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [cheshire.core :as json]
            [compojure.api.sweet :as sweet :refer (GET)]))


(defn field-adaptor [field-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (field-fn (d/entity db [(keyword wbid-field) id])))))

(defn widget-adaptor [widget-fn]
  (fn [db class id]
    (let [wbid-field (str class "/id")]
      (widget-fn (d/entity db [(keyword wbid-field) id])))))

(declare handle-field-get)
(declare handle-widget-get)
(declare json-response)

(defn register-endpoint [db scope schema-name endpoint-fn endpoint-name]
)

(defn- rest-widget-fn [field-map]
  (fn [binding]
    (reduce (fn [result-map [key field-fn]]
              (assoc result-map key (field-fn binding)))
            {}
            field-map)))

;; registered widgets and fields will be added to routes and documentation

(defn register-widget [widget-name field-map]
  ;; (do (register-endpoint "widget" (rest-widget-fn field-map) widget-name)
  ;;     (doseq [[key field-fn] field-map]
  ;;       (register-endpoint "field" schema-name field-fn (name key))))
  )

(defn register-independent-field [db schema-name field-name field-fn]
  (let [field-url (str/join "/" ["/rest" "field" schema-name ":id" field-name])]
    (GET field-url [id]
         :tags ["field" schema-name]
         (let [adapted-field-fn (field-adaptor field-fn)
               data (adapted-field-fn db schema-name id)]
           (-> {:name id
                :class schema-name
                :url (str/replace field-url #":id" id)}
               (assoc (keyword field-name) data)
               (json-response))))))

(defmacro def-rest-widget
  [name body]
  `(register-widget (str (quote ~name)) ~body))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal functions and helper ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; start of REST handler for widgets and fields
(defn- json-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring.util.response/response)
      (ring.util.response/content-type "application/json")))

;; start of REST handler for widgets and fields

(defn- handle-field-get [db schema-name id field-name]

  )

;; (defn- handle-widget-get [db schema-name id widget-name]
;;   (if-let [widget-fn (resolve-endpoint "widget" schema-name widget-name)]
;;     (let [adapted-widget-fn (widget-adaptor widget-fn)
;;           data (adapted-widget-fn db schema-name id)]
;;       (-> {:name id
;;            :class schema-name
;;            :url (str/join "/" ["/rest" "widget" schema-name id widget-name])
;;            :fields data}
;;           (json-response)))
;;     (-> {:message (format "%s widget for %s not exist or not available to public"
;;                           (str/capitalize widget-name)
;;                           (str/capitalize schema-name))}
;;         (json-response)
;;         (ring.util.response/status 404))))

;; END of REST handler for widgets and fields
