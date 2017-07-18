(ns integration.test-fallback-404
  (:require
   [clojure.data.json :as json]
   [clojure.test :as t]
   [clojure.walk :as walk]
   [rest-api.db-testing :as db-testing]
   [rest-api.main :as rest-api]
   [clojure.walk :as walk]))

(t/use-fixtures :once db-testing/db-lifecycle)

(t/deftest fallback-404
  (t/testing "When no routes are matched in request processing"
    (let [response (rest-api/app
                    {:uri "/aliens"
                     :headers {:Content-Type "application/json"}
                     :request-method :get})]
      (t/is (= 404 (:status response)))
      (t/is (= "application/json; charset=utf-8"
               (get-in response [:headers "Content-Type"])))
      (t/is (contains? response :body))
      (t/is (not= nil (-> response
                       :body
                       slurp
                       json/read-str
                       walk/keywordize-keys
                       :reason
                       not-empty))))))



