(ns rest-api.main
  (:require
   [compojure.api.sweet :as sweet]
   [environ.core :as environ]
   [mount.core :as mount]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.gene :as gene]
   [rest-api.classes.person :as person]
   [rest-api.classes.anatomy-term :as anatomy-term]
   [rest-api.classes.antibody :as antibody]
   [rest-api.classes.analysis :as analysis]
   [rest-api.classes.variation :as variation]
   [rest-api.classes.transgene :as transgene]
   [rest-api.classes.strain :as strain]
   [rest-api.classes.wbprocess :as wbprocess]
   [rest-api.classes.rearrangement :as rearrangement]
   [rest-api.classes.cds :as cds]
   [rest-api.classes.clone :as clone]
   [rest-api.classes.construct :as construct]
   [rest-api.classes.expression-cluster :as expression-cluster]
   [rest-api.classes.disease :as disease]
   [rest-api.classes.homology-group :as homology-group]
;;   [rest-api.classes.interaction :as interaction] ; comment out for WS258
   [rest-api.classes.molecule :as molecule]
   [rest-api.classes.motif :as motif]
   [rest-api.classes.paper :as paper]
   [rest-api.classes.protein :as protein]
   [rest-api.classes.rnai :as rnai]
   [rest-api.classes.sequence :as seqs]
   [rest-api.classes.structure-data :as structure-data]
   [rest-api.classes.transcript :as transcript]
   [ring.util.http-response :as res]
   [ring.middleware.gzip :as ring-gzip]))

(def ^:private all-routes
  "A collection of all routes to served by the application."
  [gene/routes
   person/routes
   anatomy-term/routes
   antibody/routes
   analysis/routes
   variation/routes
   transgene/routes
   strain/routes
   wbprocess/routes
   expression-cluster/routes
   rearrangement/routes
   cds/routes
   clone/routes
   construct/routes
   disease/routes
   homology-group/routes
;;   interaction/routes ; comment out for WS258
   molecule/routes
   motif/routes
   paper/routes
   protein/routes
   rnai/routes
   seqs/routes
   structure-data/routes
   transcript/routes])

(def ^:private swagger-validator-url
  "The URL used to validate the swagger JSON produced by the application."
  (if-let [validator-url (environ/env :swagger-validator-url)]
    validator-url
    "//online.swagger.io/validator"))

(def ^:private api-output-formats
  "The formats API endpoints will produce data in."
  ["application/json"])

(defn- wrap-not-found
  "Fallback 404 handler."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if response
        response
        (res/not-found
         {:reason "These are not the worms you're looking for"})))))

(defn init
  "Entry-point for ring server initialization."
  []
  (mount/start))

(def app
  "Entry-point for ring request handler."
  (sweet/api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :formats {:format api-output-formats}
     :coercion nil
     :consumes nil
     :produces api-output-formats
     :definitions {}
     :options
     {:ui
      {:validatorUrl swagger-validator-url}}
     :data
     {:info
      {:title "WormBase REST API"
       :description
       (str "Widget and field endpoints "
            "used by the official [WormBase]"
            "(http://www.wormbase.org) site.")
       :contact {:name "the WormBase development team"
                 :email "developers@wormbase.org"}
       :version (pace-utils/package-version "wormbase/rest-api")}}}}
   (sweet/context "/" []
     :middleware [ring-gzip/wrap-gzip wrap-not-found]
     (sweet/context "/rest" []
       (->> all-routes
            (flatten)
            (apply sweet/routes))))))
