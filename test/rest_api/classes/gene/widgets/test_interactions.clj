(ns rest-api.classes.gene.widgets.test-interactions
  (:require
   [clojure.data :refer [diff]]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.test :as t]
   [clojure.walk :as walk]
   [rest-api.classes.gene.widgets.interactions :as gene-interactions]
   [rest-api.classes.gene.regression :as regression]
   [rest-api.regression-testing :as regr-test]
   [rest-api.db-testing :as db-testing]
   [rest-api.regression-testing :as regr-testing]
   [clojure.string :as str]))

(t/use-fixtures :once db-testing/db-lifecycle)

(defn normalize [value]
  (let [res (walk/keywordize-keys value)]
    (if (and (vector? res) (every? map? res))
      (map (fn [m]
             (into (sorted-map) m)) res)
      res)))

(defn write-diff [filename-suffix exp act]
  (let [mk-filename (fn [prefix]
                      (str prefix "-" filename-suffix ".edn"))
        filenames (map mk-filename ["exp" "act"])]
    (doseq [[filename data] (zipmap filenames [exp act])]
        (binding [*out* (io/writer (io/file "/tmp" filename))]
          (pprint data)))))

(defn diff-eq
  [filename-suffix extract-data-item exp act & [{:keys [debug?]
                                                 :or {debug? false}}]]
  (println "DEBUG?" debug?)
  (println "Filename suffix:" filename-suffix)
  (let [[x a] (map extract-data-item [exp act])
        [y b] (map normalize [x a])
        [left right both] (diff y b)
        no-differences (every? nil? [left right])]
    (when-not no-differences
      (write-diff filename-suffix y b))
    (when debug?
      (println "EXPECTED:")
      (pprint y)
      (println)
      (println "ACTUAL:")
      (println)
      (pprint b))
    (t/is no-differences
          (str "LEFT:\n" (pr-str left) "\n"
               "RIGHT:\n" (pr-str right) "\n"
               "BOTH:\n" (pr-str both) "\n"))))

(defn diff-test
  [gene-id exp-data act-data label extract-data-fn]
  (println label gene-id)
  (t/testing label
    (let [filename-suffix (str/join "-" [(str/replace label #" " "-")
                                         gene-id])]
      (diff-eq filename-suffix
               extract-data-fn
               exp-data
               act-data
               {:debug? true}))))

(defn- edges-sort-key [key-xform edge1 edge2]
  (let [get-in-edge #(get-in %1 (map key-xform [%2 "id"]))
        edge-keys ["affected" "effector" "type"]
        alpha (vec (map (partial get-in-edge edge1) edge-keys))
        beta (vec (map (partial get-in-edge edge2) edge-keys))]
    (reduce #(.compareTo %1 %2) [alpha beta])))

(defn- munge-expected-data [data]
  (->> data
       (walk/postwalk (fn [xform]
                        (if (map? xform)
                          (cond
                            (contains? xform "label")
                            (update xform
                                    "label"
                                    (fn [lbl]
                                      (-> lbl
                                          (str/replace "  " " ")
                                          (str/replace ", &" " &")
                                          (str/triml))))

                            (contains? xform "edges")
                            (let [skey #(get-in % ["affected" "id"])]
                              (update xform
                                      "edges"
                                      (fn [edges]
                                        (->> edges
                                             (sort
                                              (partial edges-sort-key
                                                       str))
                                             (vec)))))
                            
                            :default xform)
                          xform)))))

(t/deftest ^{:regression true :manual true} test-interactions-widget
  (doall
   (doseq [gene-id ["WBGene00000001"
                    "WBGene00004201"
                    "WBGene00004417"
                    "WBGene00000421"
                    "WBGene00003421"
                    "WBGene00009192"
                    "WBGene00020398"]]
     (regr-testing/create-test-fixture
      (str "http://www.wormbase.org/rest/widget/gene/"
           gene-id
           "/interactions?download=1&content-type=application/json")
      {:fixtures-path "test/fixtures/classes/gene"}
      munge-expected-data)
     (let [gene (db-testing/entity "gene" gene-id)
           exp-data (regression/read-gene-fixture gene-id
                                                    "interactions")
           act-data (->> (gene-interactions/interactions gene)
                         (walk/keywordize-keys))
           do-diff-test (partial diff-test gene-id exp-data act-data)]
       (do-diff-test "interaction nodes" #(get-in % [:data :nodes]))
       (do-diff-test "interaction edges"
                     (fn [item]
                       (->> (get-in item [:data :edges])
                            (sort (partial edges-sort-key keyword))
                            vec)))
       (do-diff-test "interaction edges_all"
                     (fn [item]
                       (->> (get-in item [:data :edges_all])
                            (sort (partial edges-sort-key keyword))
                            vec)))
       (let [data-leaf #(get-in %1 [:data %2])]
         (t/testing "interaction types"
           (t/is (= (data-leaf exp-data :types)
                    (data-leaf act-data :types))))
         (t/testing "interaction ntypes"
           (t/is (= (data-leaf exp-data :ntypes)
                    (data-leaf act-data :ntypes))))
         (t/testing "interaction showall"
           (t/is (= (data-leaf exp-data :showall)
                    (data-leaf act-data :showall)))))))))


