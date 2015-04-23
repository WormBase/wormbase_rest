(ns web.trace
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params
        web.edn
        ring.middleware.gzip
        ring.middleware.session
        ring.middleware.anti-forgery
        web.widgets
        web.colonnade
        clojure.walk)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET POST context)]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [file-response]]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [web.rest.gene :refer (gene-overview
                                   gene-history
                                   gene-phenotype-rest
                                   gene-mapping-data-rest
                                   gene-human-diseases-rest
                                   gene-reagents
                                   gene-ontology
                                   gene-expression
                                   gene-homology
                                   gene-sequences
                                   gene-features
                                   gene-genetics)]
            [web.rest.interactions :refer (get-interactions get-interaction-details)]
            [web.rest.references :refer (get-references)]))

(def uri "datomic:free://localhost:4334/wb248-imp1")
(def con (d/connect uri))

(declare touch-link)

(defn touch-link-ref [ke v]
  (cond
   (:db/isComponent ke)  (touch-link v)
   (:pace/obj-ref ke)    [(:pace/obj-ref ke) ((:pace/obj-ref ke) v)]
   (:db/ident v)         (:db/ident v)
   :default              (if-let [class (first (filter #(= (name %) "id") (keys v)))]
                           [class (class v)]
                           v)))

(defn touch-link [ent]
  (let [db (d/entity-db ent)]
    (into {}
      (for [k     (keys ent)
            :let  [v (k ent)
                   ke (entity db k)]]
        [k
         (if (= (:db/valueType ke) :db.type/ref)
           (if (= (:db/cardinality ke) :db.cardinality/one)
             (touch-link-ref ke v)
             (into #{}
               (for [i v]
                 (touch-link-ref ke i))))
           v)]))))

(defn xref-link [ent clid]
  (let [xrefs (:pace/xref clid)]
    (into
     (touch-link ent)
     (for [xref xrefs
           :let [attr    (:pace.xref/attribute xref)
                 attr-ns (namespace attr)
                 attr-name (name attr)
                 revattr (keyword attr-ns
                                  (str "_" attr-name))
                 obj-ref (:pace.xref/obj-ref xref)
                 vals    (revattr ent)
                 [a1 a2] (str/split attr-ns #"\.")
                 vals    (if a2
                           (map (keyword a1 (str "_" a2)) vals)
                           vals)]
           :when (and (seq vals)
                      (not (.startsWith attr-ns "2")))]
       [revattr (into #{}
                  (for [i vals]
                    [obj-ref (obj-ref i)]))]))))      
       
          

(defn get-raw-obj [class id]
  (let [db    (db con)
        clid  (keyword class "id")
        obj   (entity db [clid id])]
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str (xref-link obj (entity db clid)))}))

(declare obj2)

(defn obj2-attr [db maxcount datoms]
  (let [attr (entity db (:a (first datoms)))]
    {:key   (:db/ident attr)
     :type  (:db/valueType attr)
     :comp  (or (:db/isComponent attr) false)
     :count (count datoms)
     :values
     (if (or (not maxcount)
             (< (count datoms) maxcount))
       (for [d datoms]
         {:txn (:tx d)
          :id (if (:db/isComponent attr)
                (str (:v d)))
          :val (cond
                (:db/isComponent attr)
                 (obj2 db (:v d) maxcount)
                (= (:db/valueType attr) :db.type/ref)
                 (touch-link-ref attr (entity db (:v d)))
                :default
                 (:v d))}))}))
         

(defn obj2 [db ent maxcount]
  (->> (d/datoms db :eavt ent)
       (seq)
       (sort-by :a)
       (partition-by :a)
       (map (partial obj2-attr db maxcount))))    

(defn xref-obj2-attr [db ent attr maxcount]
  (let [attr-ns    (namespace attr)
        attr-name  (name attr)
        revattr    (keyword attr-ns
                            (str "_" attr-name))
        obj-ref    (q '[:find ?o .
                        :in $ ?a
                        :where [?x :pace.xref/attribute ?a]
                               [?x :pace.xref/obj-ref ?oi]
                               [?oi :db/ident ?o]]
                      db attr)
        val-datoms (seq (d/datoms db :vaet ent attr))
        [a1 a2]    (str/split attr-ns #"\.")
        follow     (if a2
                     (keyword a1 (str "_" a2)))]
    (when (and val-datoms
               (not (.startsWith attr-ns "2")))
      {:key      revattr
       :type     :db.type/ref
       :comp     false        ; for now...
       :count    (count val-datoms)
       :values
       (if (or (not maxcount)
               (< (count val-datoms) maxcount))
         (for [[val _ _ txn] val-datoms]
           {:txn txn
            :val (let [ve (entity db val)]
                   [obj-ref (obj-ref (if follow
                                       (follow ve)
                                       ve))])}))})))

(defn xref-obj2 [db clid ent maxcount]
  (for [xref (:pace/xref (entity db clid))
        :let [attr       (:pace.xref/attribute xref)
              vm         (xref-obj2-attr db ent attr maxcount)]
        :when vm]
    vm))           

(defn find-txids [props]
  (mapcat
   (fn [{:keys [key values comp]}]
     (mapcat 
      (fn [v]
        (let [txn [(:txn v)]]
          (if comp
            (concat txn (find-txids (:val v)))
            txn)))
      values))
   props))

(defn get-raw-txns [ddb txids]
  (for [t txids
        :let [te (as-> (entity ddb t) $
                       (touch $)
                       (into {} $)
                       (assoc $ :db/id t))]]
    (if-let [curator (:wormbase/curator te)]
      (assoc te :wormbase/curator [:person/id (:person/id curator)])
      te)))

(defn get-raw-obj2 [class id max-out max-in txns?]
  (let [ddb   (db con)
        clid  (keyword class "id")
        entid (->> [clid id]
                   (entity ddb)
                   (:db/id))]
    (if entid
      (let [props (concat (obj2 ddb entid max-out)
                          (xref-obj2 ddb clid entid max-in))
            txids (set (find-txids props))]
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body (pr-str {:props  props
                        :id     (str entid)
                        :txns   (if txns?
                                  (get-raw-txns ddb txids))})})
      {:status 404
       :body "Not found"})))

(defn get-raw-attr2-out [ddb entid attr txns?]
  (let [prop (obj2-attr ddb nil (seq (d/datoms ddb :eavt entid attr)))
        txids (set (find-txids [prop]))]
   {:status 200
    :headers {"Content-Type" "text/plain"}
    :body (pr-str (assoc
                    prop
                    :txns (if txns? (get-raw-txns ddb txids))))}))

(defn get-raw-attr2-in [ddb entid attr txns?]
  (let [prop (xref-obj2-attr ddb entid attr nil)
        txids (set (find-txids [prop]))]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str (assoc
                     prop
                     :txns (if txns? (get-raw-txns ddb txids))))}))
  

(defn get-raw-attr2 [ddb entid attr-name txns?]
  (let [attr (keyword (.substring attr-name 1))]
    (if (.startsWith (name attr) "_")
      (get-raw-attr2-in ddb entid (keyword (namespace attr)
                                            (.substring (name attr) 1))
                        txns?)
      (get-raw-attr2-out ddb entid attr txns?))))

(defn get-raw-history2 [db entid attr]
  (let [hdb (d/history db)
        datoms (sort-by :tx (seq (d/datoms hdb :eavt entid attr)))]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str {:datoms (map (fn [[e a v tx a]] 
                                   {:e e :a a :v v :txid tx :added? a})
                                 datoms)
                    :endid entid
                    :attr attr
                    :txns (get-raw-txns db (set (map :tx datoms)))})}))

(defn get-raw-ent [id]
  (let [ddb   (db con)]
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str (touch (entity ddb id)))}))

(def ^:private rules 
  '[[[gene-name ?g ?n] [?g :gene/public-name ?n]]
    [[gene-name ?g ?n] [?c :gene.cgc-name/text ?n] [?g :gene/cgc-name ?c]]
    [[gene-name ?g ?n] [?g :gene/molecular-name ?n]]
    [[gene-name ?g ?n] [?g :gene/sequence-name ?n]]
    [[gene-name ?g ?n] [?o :gene.other-name/text ?n] [?g :gene/other-name ?o]]])

(defn get-gene-by-name [name]
  (let [ddb   (db con)
        genes (q '[:find ?gid
                   :in $ % ?name
                   :where (gene-name ?g ?name)
                          [?g :gene/id ?gid]]
                 ddb rules name)
        oldmems (q '[:find ?gcid
                     :in $ ?name
                     :where [?gc :gene-class/old-member ?name]
                            [?gc :gene-class/id ?gcid]]
                   ddb name)]
    (html
     [:h1 "Matches for " name]
     [:ul
      (for [[gid] genes]
        [:li
         [:a {:href (str "/view/gene/" gid)} gid]])]
     (when-let [o (seq oldmems)]
       [:div
        [:h1 "Old member of..."]
        [:ul
         (for [[gcid] o]
           [:a {:href (str "/view/gene-class/" gcid)} gcid])]]))))
                   

(def cljs-symbol (re-pattern "^[:]?([^0-9/]*/)?([^0-9/][^/]*)$"))

(defn get-schema-classes [db]
  (->> (q '[:find ?cid
            :where [?cid :pace/identifies-class _]]
          db)
       (map (fn [[cid]]
              (let [ent (into {} (touch (entity db cid)))]
                (assoc ent :pace/xref
                  (for [x (:pace/xref ent)
                        :let [x (touch x)]     
                        :when (re-matches cljs-symbol (str (:pace.xref/attribute x)))]
                    x)))))
       (filter (fn [attr]
                 (re-matches cljs-symbol (str (:db/ident attr)))))))

(defn get-schema-attributes [db]
  (->> (q '[:find ?attr
            :where [?attr :pace/tags _]]
          db)
       (map (fn [[attr]]
              (touch (entity db attr))))
       (filter (fn [attr]
                 (re-matches cljs-symbol (str (:db/ident attr)))))))

(defn get-schema []
  (let [ddb (db con)]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str {:classes    (get-schema-classes ddb)
                    :attributes (get-schema-attributes ddb)})}))

(defn imissing? [db ent attr]
  (empty? (d/datoms db :vaet ent attr)))

(defn- viewer-page [req]
  (html
   [:html
    [:head]
    [:body
     [:div.container-fluid
      [:div#tree]]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css"}]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css"}]
     [:link {:rel "stylesheet"
             :href "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"}]
     [:link {:rel "stylesheet"
             :href "/css/trace.css"}]
     [:script {:src "/js/out/goog/base.js"
               :type "text/javascript"}]
     [:script {:src "/js/main.js"
               :type "text/javascript"}]
     [:script {:type "text/javascript"}
      (str "/* " (friend/current-authentication req) " */")
      (if-let [id (friend/identity req)]
        (if (:wbperson (friend/current-authentication req))
          (str "trace_logged_in = '" (:current id) "';")
          "trace_logged_in = null;")
        "trace_logged_in = null;")
      (str "trace_token = '" *anti-forgery-token* "';")
      "goog.require('trace.core');"]]]))
  

(defn- transact [req]
  (try
    @(d/transact
      con
      (conj (postwalk
              (fn [x]
                (if (and (coll? x)
                         (= (count x) 2)
                         (= (first x) :db/id))
                   (let [v (second x)]
                     (if (string? v)
                       (Long/parseLong v)
                       v))
                   x))
               (:tx (:edn-params req)))
            {:db/id (d/tempid :db.part/tx)
             :db/doc (str "Write test by " (:current (friend/identity req)))
             :wormbase/curator [:person/id (:wbperson (friend/current-authentication req))]}))
    {:status 200
     :body "OK"}
    (catch Exception e {:status 500
                        :body (.getMessage e)})))

(defn get-prefix-search [db class prefix]
  (let [names (->> (d/seek-datoms db :avet (keyword class "id") prefix)
                   (map :v)
                   (take-while (fn [^String s]
                                 (.startsWith s prefix))))]
  {:status 200
   :headers {"Content-Type" "text/plain"}  ; for now
   :body (pr-str 
          {:count (count names)
           :names (take 10 names)})}))

(defn get-raw-txns2 [db ids]
  {:status 200
   :header {"Content-Type" "text/plain"}
   :body (pr-str
          {:txns (get-raw-txns db ids)})})

(defn parse-int-if [s]
  (if s
    (Integer/parseInt s)))
        

(defroutes routes
  (GET "/raw/:class/:id" {params :params}
       (get-raw-obj (:class params) (:id params)))
  (GET "/raw2/:class/:id" {params :params}
       (get-raw-obj2
        (:class params)
        (:id params)
        (parse-int-if (params "max-out"))
        (parse-int-if (params "max-in"))
        (= (params "txns") "true")))
  (GET "/attr2/:entid/:attrns/:attrname" {params :params}
       (get-raw-attr2
        (db con)
        (Long/parseLong (:entid params))
        (str (:attrns params) "/" (:attrname params))
        (= (params "txns") "true")))
  (GET "/txns" {params :params}
       (get-raw-txns2
        (db con)
        (let [ids (params "id")]
          (if (string? ids)
            [(Long/parseLong ids)]
            (map #(Long/parseLong %) ids)))))
  (GET "/history2/:entid/:attrns/:attrname" {params :params}
       (get-raw-history2
        (db con)
        (Long/parseLong (:entid params))
        (keyword (.substring (:attrns params) 1) (:attrname params))))
  (GET "/ent/:id" {params :params}
       (get-raw-ent (Long/parseLong (:id params))))
  (GET "/view/:class/:id" req (viewer-page req))
  (GET "/gene-by-name/:name" {params :params}
       (get-gene-by-name (:name params)))
  
  (GET "/gene-phenotypes/:id" {params :params}
       (gene-phenotypes-widget (db con) (:id params)))
  (GET "/gene-genetics/:id" {params :params}
       (gene-genetics-widget (db con) (:id params)))

  (GET "/rest/widget/gene/:id/overview" {params :params}
       (gene-overview (db con) (:id params)))
  (GET "/rest/widget/gene/:id/history" {params :params}
       (gene-history (db con) (:id params)))
  (GET "/rest/widget/gene/:id/phenotype" {params :params}
       (gene-phenotype-rest (db con) (:id params)))
  (GET "/rest/widget/gene/:id/interactions" {params :params}
       (get-interactions "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/interaction_details" {params :params}
       (get-interaction-details "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/mapping_data" {params :params}
       (gene-mapping-data-rest (db con) (:id params)))
  (GET "/rest/widget/gene/:id/human_diseases" {params :params}
       (gene-human-diseases-rest (db con) (:id params)))
  (GET "/rest/widget/gene/:id/references" {params :params}
       (get-references "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/reagents" {params :params}
       (gene-reagents (db con) (:id params)))
  (GET "/rest/widget/gene/:id/gene_ontology" {params :params}
       (gene-ontology (db con) (:id params)))
  (GET "/rest/widget/gene/:id/expression" {params :params}
       (gene-expression (db con) (:id params)))
  (GET "/rest/widget/gene/:id/homology" {params :params}
       (gene-homology (db con) (:id params)))
  (GET "/rest/widget/gene/:id/sequences" {params :params}
       (gene-sequences (db con) (:id params)))
  (GET "/rest/widget/gene/:id/feature" {params :params}
       (gene-features (db con) (:id params)))
  (GET "/rest/widget/gene/:id/genetics" {params :params}
       (gene-genetics (db con) (:id params)))
  
  (GET "/prefix-search" {params :params}
       (get-prefix-search (db con) (params "class") (params "prefix")))
  (GET "/schema" [] (get-schema))
  (GET "/rest/auth" [] "hello")
  (GET "/seekrit" req (friend/authorize #{::user}
                                        (str "Hello authenticated user!! "
                                             (friend/identity req))))
  (POST "/transact" req
        (friend/authorize #{::user}
          (transact req)))
  (context "/colonnade" req (friend/authorize #{::user}
                              (colonnade (db con))))                               
                                       
  (route/files "/" {:root "resources/public"}))

(defn users [username]
  (if-let [u (entity (db con) [:user/name username])]
    {:username (:user/name u)
     :password (:user/bcrypt-passwd u)
     :wbperson (->> (:user/wbperson u)
                    (:person/id))
     :roles    #{::user}}))


(def secure-app
  (-> routes
      wrap-anti-forgery
      (friend/authenticate {:allow-anon? true
                            :unauthenticated-handler #(workflows/http-basic-deny "Demo" %)
                            :workflows [(workflows/http-basic
                                         :credential-fn (partial creds/bcrypt-credential-fn users)
                                         :realm "Demo")]})
      wrap-edn-params-2
      wrap-params
      wrap-stacktrace
      wrap-session
      wrap-gzip))
      

(defonce server (run-jetty #'secure-app {:port 8120
                                         :join? false}))
