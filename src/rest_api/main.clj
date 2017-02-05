(ns rest-api.main
  (:require
   [compojure.api.sweet :as sweet]
   [environ.core :as environ]
   [mount.core :as mount]
   [rest-api.classes.gene :as gene]
   [rest-api.classes.transcript :as transcript]))

(defn init []
  (mount/start))

(def ^:private all-routes
  [gene/routes
   transcript/routes])

;; `validatorUrl` doesn't work with private urls, see:
;; https://github.com/Orange-OpenSource/angular-swagger-ui/issues/43
(def app
  (sweet/api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :options {:ui {:validatorUrl nil}}
     :data
     {:info
      {:title "WormBase REST API"
       :description (str "Widget and field endpoints "
                         "used by the official [WormBase]"
                         "(http://www.wormbase.org) site.")}}}}
   (sweet/context "/rest" []
     :tags ["rest api"]
     (->> all-routes
          (flatten)
          (apply sweet/routes)))))
