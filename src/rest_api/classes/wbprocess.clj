(ns rest-api.classes.wbprocess
  (:require
    [rest-api.classes.wbprocess.widgets.overview :as overview]
    [rest-api.classes.wbprocess.widgets.pathways :as pathways]
    [rest-api.classes.wbprocess.widgets.molecule :as molecule]
    [rest-api.classes.wbprocess.widgets.genes :as genes]
    [rest-api.classes.wbprocess.widgets.go-term :as go-term]
    [rest-api.classes.wbprocess.widgets.anatomy :as anatomy]
    [rest-api.classes.wbprocess.widgets.expression-clusters :as expression-clusters]
    [rest-api.classes.wbprocess.widgets.interactions :as interactions]
    [rest-api.classes.wbprocess.widgets.phenotypes :as phenotypes]
    [rest-api.classes.wbprocess.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "wbprocess"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :pathways pathways/widget
    :expression_clusters expression-clusters/widget
    :molecule molecule/widget
    :interactions interactions/widget
    :go_term go-term/widget
    :anatomy anatomy/widget
    :genes genes/widget
    :references references/widget}
   :field
   {:interaction_details interactions/interaction-details}})
