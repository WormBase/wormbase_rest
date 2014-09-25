(ns wb.web.ace-server
  (:use acetyl.parser
        hiccup.core
        ring.middleware.stacktrace
        clojure.java.io)
  (:import [acetyl AceSocket])
  (:require [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.string :as str]))

(def aceobj-seq #'acetyl.parser/aceobj-seq)

;; NB. emphatically not thread-safe!
(def ace (acetyl.AceSocket. "localhost" 23100))

(defn get-one-ace [ace class name]
  (.transact ace (str "find " class " " name))
  (->> (.transactToStream ace "show -a -T")
       (reader)
       (line-seq)
       (aceobj-seq)
       (doall)
       (first)))

(defn get-one-ace-tag [ace class tag name]
  (.transact ace (str "find " class " " name))
  (->> (.transactToStream ace (str "show -a -T " tag))
       (reader)
       (line-seq)
       (aceobj-seq)
       (doall)
       (first)))

(defn get-follow-ace [ace class name & follows]
  (.transact ace (str "find " class " " name))
  (doseq [f follows]
    (.transact ace (str "follow " f)))
  (->> (.transactToStream ace "show -a -T")
       (reader)
       (line-seq)
       (aceobj-seq)
       (doall)))

(defn get-follow-ace-tag [ace class tag name & follows]
  (.transact ace (str "find " class " " name))
  (doseq [f follows]
    (.transact ace (str "follow " f)))
  (->> (.transactToStream ace (str "show -a -T " tag))
       (reader)
       (line-seq)
       (aceobj-seq)
       (doall)))


;;
;; "Single object" solution
;;

(defn- gene-phenotype-table-so [rnais key]
  (let [phenotypes (->> (mapcat (fn [rnai]
                                  (->> (select rnai key)
                                       (map first))) rnais)
                        (set)
                        (map (partial get-one-ace-tag ace "Phenotype" "Name")))
        papers (->> (mapcat (fn [rnai]
                              (->> (select rnai ["Reference"])
                                   (map first))) rnais)
                    (set)
                    (map (partial get-one-ace-tag ace "Paper" "Author"))
                    (map (juxt :id identity))
                    (into {}))]
    (html
     [:table {:border 1}
      (for [pheno phenotypes]
        [:tr
         [:td (first (select pheno ["Name" "Primary_name"]))]
         [:td
          (for [r rnais
                :when (contains? (set (map first (select r key))) (:id pheno))]
            [:div
             (:id r)
             (for [[pid] (select r ["Reference"])
                   :let [p (papers pid)]]
               [:div
                [:a {:href (str "/paper/" (:id p))}
                 (ffirst (select p ["Author"])) " et al."]])
             (if-let [[[strain]] (select r ["Experiment" "Strain"])]
               [:div
                (str "Strain: " strain)])])]])])))

(defn get-gene-phenotypes-so [id]
  (locking [ace]
    (let [gene (get-one-ace ace "Gene" id)
          rnais (for [[rid & evidence] (select gene ["Experimental_info" "RNAi_result"])]
                  (get-one-ace ace "RNAi" rid))]
      (html
       [:h3 "Observed phenotypes"]
       (gene-phenotype-table-so rnais ["Phenotype"])
       [:h3 "Not-observed phenotypes"]
       (gene-phenotype-table-so rnais ["Phenotype_not_observed"])))))

;;
;; "Bulk fetch" solution
;;

(defn- gene-phenotype-table [gid rnais key]
  (let [phenotypes (->> (get-follow-ace-tag ace "Gene" "Name" gid "RNAi_result" (last key)))
        papers (->> (get-follow-ace-tag ace "Gene" "Author" gid "RNAi_result" "Reference")
                    (map (juxt :id identity))
                    (into {}))]
    (html
     [:table {:border 1}
      (for [pheno phenotypes]
        [:tr
         [:td (first (select pheno ["Name" "Primary_name"]))]
         [:td
          (for [r rnais
                :when (contains? (set (map first (select r key))) (:id pheno))]
            [:div
             (:id r)
             (for [[pid] (select r ["Reference"])
                   :let [p (papers pid)]]
               [:div
                [:a {:href (str "/paper/" (:id p))}
                 (ffirst (select p ["Author"])) " et al."]])
             (if-let [[[strain]] (select r ["Experiment" "Strain"])]
               [:div
                (str "Strain: " strain)])])]])])))

(defn get-gene-phenotypes [id]
  (locking [ace]
    (let [gene (get-one-ace ace "Gene" id)
          rnais (get-follow-ace ace "Gene" id "RNAi_result")]
      (html
       [:h3 "Observed phenotypes"]
       (gene-phenotype-table id rnais ["Phenotype"])
       [:h3 "Not-observed phenotypes"]
       (gene-phenotype-table id rnais ["Phenotype_not_observed"])))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
       (get-gene-phenotypes (:id params)))
  (GET "/gene-phenotypes-so/:id" {params :params}
       (get-gene-phenotypes-so (:id params))))

(def wb-ace-app (wrap-stacktrace routes))
(defonce server (run-jetty #'wb-ace-app {:join? false
                                           :port 8105}))
