(ns rest-api.main
  (:require
   [compojure.api.sweet :as sweet]
   [environ.core :as environ]
   [mount.core :as mount]
   [pseudoace.utils :as pace-utils]
   [ring.util.http-response :as res]
   [ring.middleware.gzip :as ring-gzip]
   [rest-api.classes.analysis :as analysis]
   [rest-api.classes.anatomy-term :as anatomy-term]
   [rest-api.classes.antibody :as antibody]
   [rest-api.classes.blast-hit :as blast-hit]
   [rest-api.classes.cds :as cds]
   [rest-api.classes.clone :as clone]
   [rest-api.classes.construct :as construct]
   [rest-api.classes.do-term :as do-term]
   [rest-api.classes.expression-cluster :as expression-cluster]
   [rest-api.classes.expr-pattern :as expr-pattern]
   [rest-api.classes.expr-profile :as expr-profile]
   [rest-api.classes.feature :as feature]
   [rest-api.classes.gene :as gene]
   [rest-api.classes.gene-class :as gene-class]
   [rest-api.classes.gene-cluster :as gene-cluster]
   [rest-api.classes.go-term :as go-term]
   [rest-api.classes.homology-group :as homology-group]
   [rest-api.classes.interaction :as interaction]
   [rest-api.classes.laboratory :as laboratory]
   [rest-api.classes.life-stage :as life-stage]
   [rest-api.classes.microarray-results :as microarray-results]
   [rest-api.classes.molecule :as molecule]
   [rest-api.classes.motif :as motif]
   [rest-api.classes.operon :as operon]
   [rest-api.classes.paper :as paper]
   [rest-api.classes.pcr-product :as pcr-product]
   [rest-api.classes.person :as person]
   [rest-api.classes.phenotype :as phenotype]
   [rest-api.classes.position-matrix :as position-matrix]
   [rest-api.classes.protein :as protein]
   [rest-api.classes.pseudogene :as pseudogene]
   [rest-api.classes.rearrangement :as rearrangement]
   [rest-api.classes.rnai :as rnai]
   [rest-api.classes.sequence :as seqs]
   [rest-api.classes.strain :as strain]
   [rest-api.classes.structure-data :as structure-data]
   [rest-api.classes.transcript :as transcript]
   [rest-api.classes.transgene :as transgene]
   [rest-api.classes.transposon :as transposon]
   [rest-api.classes.transposon-family :as transposon-family]
   [rest-api.classes.variation :as variation]
   [rest-api.classes.wbprocess :as wbprocess]))

(def ^:private all-routes
  "A collection of all routes to served by the application."
  [analysis/routes
   anatomy-term/routes
   antibody/routes
   blast-hit/routes
   cds/routes
   clone/routes
   construct/routes
   do-term/routes
   expr-pattern/routes
   expr-profile/routes
   expression-cluster/routes
   feature/routes
   gene-class/routes
   gene-cluster/routes
   gene/routes
   go-term/routes
   homology-group/routes
   interaction/routes
   laboratory/routes
   life-stage/routes
   microarray-results/routes
   molecule/routes
   motif/routes
   operon/routes
   paper/routes
   pcr-product/routes
   person/routes
   phenotype/routes
   position-matrix/routes
   protein/routes
   pseudogene/routes
   rearrangement/routes
   rnai/routes
   seqs/routes
   strain/routes
   structure-data/routes
   transcript/routes
   transgene/routes
   transposon-family/routes
   transposon/routes
   variation/routes
   wbprocess/routes])

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
