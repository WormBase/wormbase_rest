(ns rest-api.main
  (:require
   [compojure.api.sweet :as sweet]
   [compojure.api.routes :as routes]
   [rest-api.routes :refer [routes-from-spec]]
   [rest-api.classes.gene.routing :as gene]
   [rest-api.classes.transcript.routing :as transcript]
   [mount.core :as mount]))

(def ^:private all-route-specs
  [gene/route-spec
   transcript/route-spec])

(defn init []
  (mount/start))

;; `validatorUrl` doesn't work with private urls, see:
;; https://github.com/Orange-OpenSource/angular-swagger-ui/issues/43
(def app
  (sweet/api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :options {:ui {:validatorUrl nil}}
     :data {:info
            {:title "WormBase REST API"
             :description (str "Widget and field endpoints "
                               "used by the official [WormBase]"
                               "(http://www.wormbase.org) site.")}}}}
   (sweet/context "/rest" []
     :tags ["rest api"]
     (->> all-route-specs
          (flatten)
          (apply sweet/routes)))))
