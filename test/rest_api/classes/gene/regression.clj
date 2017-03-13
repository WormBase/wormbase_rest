(ns rest-api.classes.gene.regression
  (:require
   [rest-api.regression-testing :as regr-testing]))

(defn read-gene-fixture
  "Read a fixture for a given `gene-name` for testing an endpoint
  with the name `endpoint-name`."
  [gene-name endpoint-name & opts]
  (let [url (str "/rest/widget/gene/" gene-name "/" endpoint-name)
        opts {:fixtures-path "test/fixtures/classes/gene"}]
    (regr-testing/read-test-fixture url opts)))


