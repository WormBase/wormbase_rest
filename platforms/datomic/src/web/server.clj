(ns web.server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:require [datomic.api :as d :refer (db q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.core.async :as async :refer (go chan put! mult tap >! <!)]
            [ninjudd.eventual.server :refer (json-events)]))

(def uri "datomic:free://localhost:4334/smallace")
(def conn (d/connect uri))

;;
;; Allow subscriptions to Datomic updates.
;;
;; There might be an argument for avoiding using a "go" block
;; here because it will block a thread from the core.async pool.
;;

(def update-channel (chan))
(def update-mult (mult update-channel))

(defn- datom-entity [d]
  (.e d))

(let [trq (d/tx-report-queue conn)]
  (go (loop []
        (println "ready...")
        (let [tx (.take trq)]
          (println tx)
          (let [db     (:db-after tx)
                datoms (:tx-data tx)
                ents   (set (map datom-entity datoms))]
            (doseq [eid ents
                  :let [e (entity db eid)]]
              (when-let [gid (:gene/id e)]
                (>! update-channel {:type "gene"
                                    :id gid}))))
          (recur)))))


(def ^:private pheno-by-id
  '[:find ?p 
    :in $ ?x
    :where [?p :phenotype/id ?x]])

(def ^:private gene-by-id
  '[:find ?g
    :in $ ?gid
    :where [?g :gene/id ?gid]])

(def ^:private paper-by-id
  '[:find ?p
    :in $ ?pid
    :where [?p :paper/id ?pid]])

(def ^:private rnai-by-id
  '[:find ?r
    :in $ ?rid
    :where [?r :rnai/id ?rid]])

(defn get-phenotype [id]
  (let [ddb  (db conn)
        pids (q pheno-by-id ddb id)]
    (if (seq pids)
      (let [pheno  (d/touch (d/entity ddb (ffirst pids)))
            rnais  (:rnai/_phenotype pheno)
            nrnais (:rnai/_not.phenotype pheno)]
        (html [:p (str pheno)]
              [:p "rnais " (d/touch (first rnais))]
              [:p "not_rnais " (count (:rnai/_not.phenotype pheno))]))
      (str "Couldn't find " id))))

(defn- paper-first-author [paper]
  (->> (:paper/author paper)
       (sort-by :paper.author/ordinal)
       (first)
       (:paper.author/name)))

(defn- paper-link [paper]
  (html
   [:a.paper-link {:href (str "/paper/" (:paper/id paper))}
    (paper-first-author paper) " et al."]))

(defn evidence [ent]
  (html
   [:div.ev.ui-helper-hidden
    (when-let [curator (first (:evidence/curator ent))]
      [:span [:b "Curator: "] curator [:br]])
    (when-let [papers (seq (:evidence/paper ent))]
      [:span [:b "Paper evidence: "] (str/join "; " (map paper-link papers))])]))

(defn get-gene [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html
         [:head
          [:link {:rel "stylesheet"
                  :href "http://www.wormbase.org/css/jquery-ui.min.css"}]
          [:link {:rel "stylesheet"
                  :href "http://www.wormbase.org/css/main.min.css?va14b17b7c91a52904182ba6f3814c35b94296328"}]
          [:script {:src "http://www.wormbase.org/js/jquery-1.9.1.min.js"}]
          [:script {:src "http://www.wormbase.org/js/jquery-ui-1.10.1.custom.min.js"}]
          [:script {:src "http://www.wormbase.org/js/wormbase.min.js?va14b17b7c91a52904182ba6f3814c35b94296328"}]
          [:script {:src "/js/wb.js"}]]
         [:body {:onload "wb_gene_init();"}
          [:div#header {:data-page "{'history': '0'}"}]
          [:div#content
           [:div#page-title.species-bg [:h2 "Gene foo"]]
           [:div#widgets
            [:div.navigation
             [:div#navigation
              "foo"]]
            [:div#widget-holder.gene]]]])))))


(defn get-gene-overview [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html
         [:head
          [:link {:rel "stylesheet"
                  :href "http://www.wormbase.org/css/jquery-ui.min.css"}]
          [:link {:rel "stylesheet"
                  :href "http://www.wormbase.org/css/main.min.css?va14b17b7c91a52904182ba6f3814c35b94296328"}]
          [:script {:src "http://www.wormbase.org/js/jquery-1.9.1.min.js"}]
          [:script {:src "http://www.wormbase.org/js/jquery-ui-1.10.1.custom.min.js"}]
          [:script {:src "http://www.wormbase.org/js/wormbase.min.js?va14b17b7c91a52904182ba6f3814c35b94296328"}]]

         
         
         [:h2
          [:i (or (:gene/name.public gene)
                  (:gene/name.cgc gene)
                  (:gene/name.sequence gene)
                  "tgwnn-1")]
          [:span#fade {:style {:font-size "0.7em"}}
           " (Gene_class description)"]]
         
         [:div.detail-box.ui-corner-all
          [:div.field
           [:div.field-title
            [:span {:title "the genus and species of the current object"}
             "Species:"]]
           [:div.field-content
            [:span.species "Caenorhabditis elegans"]]]

          (if-let [n (:gene/name.public gene)]
            [:div.field
             [:div.field-title [:span "Public name"]]
             [:div.field-content n]])

          (if-let [n (:gene/name.sequence gene)]
            [:div.field
             [:div.field-title [:span "Sequence name"]]
             [:div.field-content n]])

          (if-let [n (:gene/name.cgc gene)]
            [:div.field
             [:div.field-title [:span "CGC name"]]
             [:div.field-content n]])
          
          [:div.field
           [:div.field-title
            [:span "Wormbase ID:"]]
           [:div.field-content (:gene/id gene)]]]

         
         [:div.description
          [:div.evidence.result
           (:gene.desc/concise (:gene/desc gene))
           (evidence (:gene/desc gene))
           [:div.ev-more
            [:div.v.ev-more-line]
            [:div.ev-more-text [:span "evidence"]]
            [:div.v.ui-icon.ui-incon-triangle-1-s]]]]))

      (str "Couldn't find " id))))



(defn- gene-phenotype-table [gene key]
  (html
   [:table {:border 1}
    (for [[pheno rnais] (group-by first
                          (mapcat #(for [p (key (:gene.rnai/rnai %))] 
                                     [p %])
                                  (:gene/rnai gene)))]
      [:tr
       [:td (:phenotype/name pheno)]
       [:td (for [[_ rl] rnais
                  :let [r (:gene.rnai/rnai rl)]]
              [:div 
               (:rnai/id r)
               (for [p (:rnai/reference r)]
                 [:div
                  [:a {:href (str "/paper/" (:paper/id p))}
                   (paper-first-author p) " et al."]])
               (if-let [strain (:rnai/expt.strain r)]
                 [:div
                  (str "Strain: " strain)])])]])]))

(defn get-gene-phenotypes [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html
         [:h3 "Observed phenotypes"]
         (gene-phenotype-table gene :rnai/phenotype)
         [:h3 "Not-observed phenotypes"]
         (gene-phenotype-table gene :rnai/not.phenotype)))
      (str "Couldn't find " id))))

(defn get-gene-refs [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html 
         [:table {:border 1}
          (for [p (:gene/reference gene)]
            [:tr
             [:td (:paper/brief.citation p)]])]))
      (str "Couldn't find " id))))

(defn get-paper [id]
  (let [ddb  (db conn)
        pids (q paper-by-id ddb id)]
    (if (seq pids)
      (let [paper (d/entity ddb (ffirst pids))]
        (html
         (:longtext/text (:paper/abstract paper))))
      (str "Couldn't find " id))))

(defn updates []
  (->> (chan)
       (tap update-mult)
       (json-events)))

(defroutes routes
  (GET "/phenotype/:id" {params :params}
    (get-phenotype (:id params)))
  (GET "/gene/:id" {params :params}
    (get-gene (:id params)))
  (GET "/gene-overview/:id" {params :params}
    (get-gene-overview (:id params)))
  (GET "/gene-phenotypes/:id" {params :params}
    (get-gene-phenotypes (:id params)))
  (GET "/gene-refs/:id" {params :params}
    (get-gene-refs (:id params)))
  (GET "/paper/:id" {params :params}
       (get-paper (:id params)))
  (GET "/updates" [] (updates))
  (GET "/rest/auth" [] "Hello world")
  (route/files "/" {:root "resources/public"}))

(def appn (wrap-stacktrace routes))
