(ns rest-api.classes.gene.widgets.test-interactions
  (:require
   [clojure.test :as t]
   [rest-api.classes.gene.widgets.interactions :as gene-interactions]
   [rest-api.classes.gene.regression :as regression]
   [rest-api.regression-testing :as regr-test]
   [rest-api.db-testing :as db-testing]
   [rest-api.regression-testing :as regr-testing]))

(t/use-fixtures :once db-testing/db-lifecycle)

(t/deftest test-interactions-widget
  (t/testing "Regression for gene interactions widget."
    (doseq [gene-id ["WBGene00000001"
                     "WBGene00000421"
                     "WBGene00003421"
                     "WBGene00009192"]]
      (let [gene (db-testing/entity "gene" gene-id)
            exp-data (regression/read-gene-fixture gene-id "interactions")
            act-data (gene-interactions/interactions gene)]
        (t/is (empty? (regr-test/compare-api-result "interactions"
                                                    exp-data
                                                    act-data)))))))
