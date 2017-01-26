(ns datomic-rest-api.rest.core
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [cheshire.core :as json]
            [compojure.api.sweet :as sweet :refer (GET)]))


(defn endpoint-adaptor [endpoint-fn]
  (fn [db class id]
    (let [wbid (str class "/id")]
      (endpoint-fn (d/entity db [(keyword wbid) id])))))

(defn rest-widget-fn [fields-map]
  (fn [binding]
    (reduce (fn [result-map [key field-fn]]
              (assoc result-map key (field-fn binding)))
            {}
            fields-map)))

(defn- json-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring.util.response/response)
      (ring.util.response/content-type "application/json")))

(defn field-route [db schema-name field-name field-fn]
  (let [field-url (str/join "/" ["/rest" "field" schema-name ":id" field-name])
        adapted-field-fn (endpoint-adaptor field-fn)]
    (GET field-url [id]
         :tags [(str schema-name " fields")]
         (-> {:name id
              :class schema-name
              :url (str/replace field-url #":id" id)}
             (assoc (keyword field-name)
                    (adapted-field-fn db schema-name id))
             (json-response)))))

(defn widget-route [db schema-name widget-name fields-map]
  (let [widget-url (str/join "/" ["/rest" "widget" schema-name ":id" widget-name])
        adapted-widget-fn (endpoint-adaptor (rest-widget-fn fields-map))]
    (->> (cons (GET widget-url [id]
                    :tags [(str schema-name " widgets")]
                    (-> {:name id
                         :class schema-name
                         :url (str/replace widget-url #":id" id)}
                        (assoc (keyword widget-name)
                               (adapted-widget-fn db schema-name id))
                        (json-response)))
               (map (fn [[field-name field-fn]]
                      (field-route db schema-name (name field-name) field-fn))
                    fields-map))
         (apply sweet/routes))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal functions and helper ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



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
