(ns rest-api.test-routing
  (:require
   [clojure.test :refer :all]
   [compojure.api.routes :as c-routes]
   [compojure.api.sweet :as sweet]
   [rest-api.db-testing :as db-testing]
   [rest-api.routing :as routing]))

(use-fixtures :once db-testing/db-lifecycle)

(deftest test-conform-to-scheme
  (testing "Is structure correct for catalyst for widget scheme?"
    (let [req {:uri "/rest/widget/gene/WBGene0101010101/overview"}
          entity {}
          [res-x, res-y] [{:data {:x "X"} :description "X desc"}
                          {:data {:y "Y"} :description "Y desc"}]
          handler-x (fn [e] res-x)
          handler-y (fn [e] res-y)
          entity-handler {:x handler-x :y handler-y}
          expected {:fields {:x res-x :y res-y}
                    :uri "rest/widget/gene/WBGene0101010101/overview"}
          result (routing/conform-to-scheme :widget
                                            entity-handler
                                            entity
                                            req)]
      (is (= result expected))))
  (testing "Is structure correct for catalyst for field scheme?"
    (let [req {:uri "/rest/field/gene/WBGene0101010101/f1"}
          entity {}
          result {:data {:x "X"} :description "D"}
          entity-handler (fn [e] result)
          expected {"f1" (entity-handler entity)
                    ;; TODO: dubious
                    ;; should probably be /rest/field/ rather than
                    ;; /species
                    ;; See /WormBase/datomic-to-catalyst/issues/61
                    :uri "/species/gene/WBGene0101010101/f1"}
          result (routing/conform-to-scheme :field
                                            entity-handler
                                            entity
                                            req)]
      (is (= result expected)))))


;; setup for test-defroutes
(let [dummy-handler (fn [_] {})
      mk-route-spec (fn [keys]
                      (into {} (for [k keys]
                                 [k dummy-handler])))
      widget {:x (mk-route-spec [:a :b :c :d])
              :y (mk-route-spec [:e :f :g])}
      fields {:z (mk-route-spec [:h :i])}]
  (routing/defroutes {:datatype "testing"
                      :widget widget
                      :field fields})
(deftest test-defroutes
  (testing "Defining routes via `defroutes` macro."
    ;; Expected counts:
    ;; 1 (widget) +
    ;; count of fields in widget +
    ;; count of independent fields.
    ;; symbol `routes` is defn'd by defroutes
    (let [sweet-routes (c-routes/get-routes
                        (sweet/context "/" []
                          (apply sweet/routes routes)))]
      (is (= (count sweet-routes) 10)))
    (is (every? #(satisfies? c-routes/Routing %) routes)))))
