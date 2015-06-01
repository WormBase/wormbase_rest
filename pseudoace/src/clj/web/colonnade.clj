(ns web.colonnade
  (:use hiccup.core
        ring.middleware.anti-forgery)   ;; for *anti-forgery-token*
  (:require [datomic.api :as d :refer (history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET POST routes)]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(defn- page []
  (html
   [:html
    [:head
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css"}]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css"}]
     [:link {:rel "stylesheet"
             :href "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"}]
     [:link {:rel "stylesheet"
             :href "/css/trace.css"}]]
    [:body
     [:div.root
      [:div.header
       [:img.banner {:src "/img/logo_wormbase_gradient_small.png"}]
       [:h1#page-title "Colonnade"]
       [:img.banner {:src "/img/kazannevsky.jpg" :width 130 :style "float: right"}]
       [:div#header-content]]
     [:div.container-fluid
      [:div#table-maker]]]
     [:script {:src "/js/out/goog/base.js"
               :type "text/javascript"}]
     [:script {:src "/js/main.js"
               :type "text/javascript"}]
     [:script {:type "text/javascript"}
      (str "trace_token = '" *anti-forgery-token* "';")
      "goog.require('trace.colonnade');"]]]))


(defn post-query [db {:keys [query rules args max-rows]}]
  (let [args (if (seq rules)
               (cons rules args)
               args)
        results (apply q query db args)]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str {:query query
                    :results (take max-rows (sort-by first results))
                    :count (count results)})}))

(defn colonnade [db]
  (routes
   (GET "/" [] (page))
   (POST "/query" {edn-params :edn-params} (post-query db edn-params))))
