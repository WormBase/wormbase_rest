(ns rest-api.classes.gene.widgets.test-interactions
  (:require
   [clojure.test :as t]
   [rest-api.classes.gene.widgets.interactions :as gene-interactions]
   [rest-api.classes.gene.regression :as regression]
   [rest-api.regression-testing :as regr-test]
   [rest-api.db-testing :as db-testing]
   [rest-api.regression-testing :as regr-testing]))

(t/use-fixtures :once db-testing/db-lifecycle)

(t/deftest ^:regression test-interactions-widget
  (t/testing "Regression for gene interactions widget."
    (doall
     (doseq [gene-id [;; "WBGene00000001"

                      ;; Simpler then WBGene00000421 (but still
                      ;; complex case):
                      "WBGene00004201"
                      
                      ;; Waay off
                      ;; "WBGene00000421"

                      ;; Way off
                      ;; "WBGene00003421"

                      ;; Passing
                      ;; "WBGene00009192"
                      ;; "WBGene00020398"
                      ]]
       ;; (regr-testing/create-test-fixture
       ;;  (str "http://www.wormbase.org/rest/widget/gene/"
       ;;       gene-id
       ;;       "/interactions?download=1&content-type=application/json")
       ;;  {:fixtures-path "test/fixtures/classes/gene"})
       (let [gene (db-testing/entity "gene" gene-id)
             exp-data (regression/read-gene-fixture gene-id
                                                    "interactions")
             act-data (gene-interactions/interactions gene)]
         (t/is (empty? (regr-test/compare-api-result "interactions"
                                                     exp-data
                                                     act-data
                                                     {:debug? true}))))))))
