(ns rest-api.test-routing
  (:require
   [clojure.test :refer :all]
   [compojure.api.routes :as c-routes]
   [compojure.api.sweet :as sweet]
   [rest-api.db-testing :as db-testing]
   [rest-api.routing :as routing]
   [clojure.string :as str]))

(use-fixtures :once db-testing/db-lifecycle)

(deftest test-make-request-handler
  (testing
    "Main request handler produces correct data structure (widgets)"
    (let [req {:uri "/rest/widget/gene/WBGene00000001/overview"
               :context "/rest/widget/gene"
               :params {:id "WBGene00000001"}}
          [res-x, res-y] [{:data {:x "X"} :description "X desc"}
                          {:data {:y "Y"} :description "Y desc"}]
          handler-x (fn [e] res-x)
          handler-y (fn [e] res-y)
          widgets {:top-x handler-x :top-y handler-y}
          handler (routing/make-request-handler :widget widgets "gene")
          response (handler req)
          expected {:uri "rest/widget/gene/WBGene00000001/overview"
                    :class "gene"
                    :name "WBGene00000001"
                    :fields
                    {:top-x res-x
                     :top-y res-y}}]
      (is (= 200 (:status response)))
      (is (= (:body response) expected))))
  (testing
    "Main request handler produces correct data structure (fields)."
    (let [req {:uri "/rest/field/gene/WBGene00000001/xrefs"
               :context "/rest/widget/gene"
               :params {:id "WBGene00000001"}}
          res-x {:data {:x "X"} :description "X desc"}
          entity-handler (fn [e] res-x)
          handler (routing/make-request-handler :field
                                                entity-handler
                                                "gene")
          response (handler req)
          expected {:xrefs res-x
                    :uri "rest/field/gene/WBGene00000001/xrefs"
                    :class "gene"
                    :name "WBGene00000001"}]
      (is (= (:body response) expected))))
  (testing
      "Entity namespace can be aliased in request URI."
    (let [entity-id "DOID:7"
          entity-ns "do-term"
          req {:uri "/rest/widget/disease/DOID:7/overview"
               :context "/rest/widget/disease"
               :params {:id entity-id}}
          disease-overview {:data {:info "Disease info"}
                            :description "Some disease"}
          entity-handler (fn [e]
                           disease-overview)
          widgets {:overview entity-handler}
          handler (routing/make-request-handler :widget widgets
                                                entity-ns)
          response (handler req)
          expected {:uri "rest/widget/disease/DOID:7/overview"
                    :class entity-ns
                    :name entity-id
                    :fields {:overview disease-overview}}]
      (is (= (:body response) expected)))))

(deftest test-conform-to-scheme
  (testing "Is structure correct for catalyst for widget scheme?"
    (let [req {:uri "/rest/widget/gene/WBGene0101010101/overview"}
          entity {}
          [res-x, res-y] [{:data {:x "X"} :description "X desc"}
                          {:data {:y "Y"} :description "Y desc"}]
          handler-x (fn [e] res-x)
          handler-y (fn [e] res-y)
          entity-handler {:x handler-x :y handler-y}
          expected {:fields {:x res-x :y res-y}}
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
          expected {:f1 (entity-handler entity)}
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
  (routing/defroutes {:entity-ns "some-entity"
                      :uri-name "different_to_entity_namespace"
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
