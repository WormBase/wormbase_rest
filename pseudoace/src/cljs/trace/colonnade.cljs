(ns trace.colonnade
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [secretary.core :as secretary :include-macros true :refer-macros (defroute)]
            [goog.dom :as gdom]
            [goog.events :as events]
            [trace.utils :refer (edn-xhr edn-xhr-post conj-if process-schema)])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def app-state (atom {}))

(defn schema []
  (om/ref-cursor (:schema (om/root-cursor app-state))))

(defn columns []
  (om/ref-cursor (:columns (om/root-cursor app-state))))

(def column-template
  {:name "Column 1"
   :attribute nil
   :from nil
   :required true
   :visible true})
   

(defn input-text [data owner {:keys [key]}]
  (reify
    om/IRender
    (render [_]
      (dom/input
       {:type "text"
        :class "form-control"
        :value (key data)
        :on-change (fn [e]
                    (om/update! data key (.. e -target -value)))}))))

(defn input-checkbox [data owner {:keys [key]}]
  (reify
    om/IRender
    (render [_]
      (dom/input
       {:type "checkbox"
        :class "form-control"
        :checked (key data)
        :on-change (fn [e]
                    (om/update! data key (.. e -target -checked)))}))))

(defn class-menu [col owner]
  (reify
    om/IRender
    (render [_]
      (if (:via col)
        (dom/span nil (str (:attribute col)))
        (let [schema (om/observe owner (schema))]
          (dom/select
           {:value (str (:attribute col))
            :class "form-control"
            :on-change (fn [e]
                         (om/update! col :attribute (reader/read-string (.. e -target -value))))}
           (dom/option nil)
           (concat
            #_(for [type [:db.type/string :db.type/long :db.type/instant :db.type/ref :db.type/float :db.type/double :db.type/boolean]]
              (dom/option #js {:value type} (str type)))
            (for [ci (:classes schema) :when (not (:pace/is-hash ci))]
              (dom/option #js {:value (:db/ident ci)} (:pace/identifies-class ci))))))))))
  

(defn from-menu [col owner]
  (reify
    om/IRender
    (render [_]
      (let [columns (om/observe owner (columns))]
        (dom/select
               {:value (:from col)
                :class "form-control"
                :on-change (fn [e]
                             (let [col-name (.. e -target -value)]
                               (om/update! col :from col-name)))}
               (dom/option nil)
               (for [[k c] columns :when (not= c col)]
                 (dom/option #js {:value k} (:name c))))))))

(defn via-menu [col owner]
  (reify
    om/IRender
    (render [_]
      (let [schema (:schema @app-state)
            columns (om/observe owner (columns))
            from (if-let [f (:from col)]
                   (columns f))
            attr (:attribute from)
            nss (if attr
                  (if (= (name attr) "id")
                    [(namespace attr)]
                    (cons 
                     (str (namespace attr) "." (name attr))
                     (:pace/use-ns ((:attrs-by-ident schema) attr)))))
            ns-attrs (doall
                      (concat
                       (mapcat (:attrs schema) nss)
                       (if-let [ci (first (filter #(= (:db/ident %) attr) (:classes schema)))]
                         (for [x   (:pace/xref ci)
                               :let [xa  (:pace.xref/attribute x)]]
                           {:db/ident (keyword (namespace xa) (str "_" (name xa)))
                            :is-xref true
                            :db/valueType :db.type/ref
                            :pace/obj-ref (:pace.xref/obj-ref x)}))))]
                           
                             
        (dom/select {:value (:via col)
                     :class "form-control"
                     :on-change (fn [e]
                                  (let [via-name (.. e -target -value)
                                        via (first (filter #(= (str (:db/ident %)) via-name) ns-attrs))]   ;; (mapcat (:attrs @schema) nss)))]
                                    (om/update! col :via (:db/ident via))
                                    (om/update!
                                     col :attribute
                                     (if (= (:db/valueType via) :db.type/ref)
                                       (cond
                                        (:db/isComponent via)
                                        (:db/ident via)
                                        
                                        (:pace/obj-ref via)
                                        (:pace/obj-ref via)

                                        :default
                                        :enum)
                                     
                                       (:db/valueType via)))))}
               (dom/option nil)
               (for [a ns-attrs
                     :let [i (:db/ident a)]]
                     (dom/option #js {:value (str i)}
                                 (str i))))))))
                 

(defn enum-constraint [{:keys [via] :as col} owner]
  (reify
    om/IRender
    (render [_]
      (let [schema      (om/observe owner (schema))
            tns         (str (namespace via) "." (name via))
            enum-values ((:attrs schema) tns)]
        (dom/select
         {:value (str (:constrain col))
          :class "form-control"
          :on-change (fn [e]
                       (om/update! col :constrain (reader/read-string (.. e -target -value))))}
         (dom/option nil)
         (for [ev enum-values
               :let [id (:db/ident ev)]]
           (dom/option {:value (str id)}
                       (str id))))))))

(defn constraint-editor [{:keys [attribute] :as col}]
  (cond
   (nil? attribute)
   nil

   (= attribute :db.type/string)
   (om/build input-text col {:opts {:key :constrain}})
   
   (= (name attribute) "id")
   (om/build input-text col {:opts {:key :constrain}})

   (= attribute :enum)
   (om/build enum-constraint col)

   ))

(defn column-def-view [col owner]
  (reify
    om/IRender
    (render [_]
      (dom/div {:class "column-holder"}
       (dom/button {:on-click (fn [ev]
                                (.preventDefault ev)
                                (let [cid (some (fn [[k cd]] (if (= cd @col)
                                                               k))
                                                (:columns @app-state))]
                                  (om/transact! (om/root-cursor app-state)
                                                :columns #(dissoc % cid))))
                    :class "remove"}
                   "x")
       (dom/form {:class "form-inline"}
         (dom/div {:class "form-group"}
           (dom/label "Name: ")
           (om/build input-text col {:opts {:key :name}}))

         (if (not (:root col))
           (dom/div {:class "form-group"}
             (dom/label "From: ")
             (om/build from-menu col)
             (dom/label "Via: ")
             (om/build via-menu col)))

         (dom/div {:class "form-group"}
           (dom/label "Class: ")
           (om/build class-menu col))
       
         (dom/br nil)

         (dom/div {:class "form-group"}
           (dom/label "Required: ")
           (om/build input-checkbox col {:opts {:key :required}}))

         (dom/div {:class "form-group"}
           (dom/label "Visible: ")
           (om/build input-checkbox col {:opts {:key :visible}}))

         (dom/div {:class "form-group"}
           (dom/label "Constrain: ")
           (constraint-editor col)))))))

(defn get-query [columns schema]
  (reduce
   (fn [{:keys [find where rules]} [k col]]
     (let [via      (:via col)
           via-xref (if via
                      (if (= (.indexOf (name via) "_") 0)
                        (keyword (namespace via) (.substring (name via) 1))))
           vs   ((:attrs-by-ident schema) via)
           attr (:attribute col)]
       {:find
        (conj-if find
                 (if (and (:visible col) attr)
                   (cond
                    (and (= (:db/valueType vs) :db.type/ref)
                         (nil? (:pace/obj-ref vs))
                         (not (:db/isComponent vs)))
                    (symbol (str "?" k "-ident"))
                    
                    (= (name attr) "id")
                    (symbol (str "?" k "-id"))

                    (:db/isComponent vs)
                    (list 'pull (symbol (str "?" k)) '[*])

                    :default
                    (symbol (str "?" k)))))

        :where
        (concat where
                 (if via
                   (if (:required col)
                     (if via-xref
                       (if (= (count (str/split (namespace via-xref) #"\.")) 2)
                         [[(symbol (str "?" k "-holder"))
                           via-xref
                           (symbol (str "?" (:from col)))]
                          [(symbol (str "?" k))
                           (let [[x y] (str/split (namespace via-xref) #"\.")]
                             (keyword x y))
                           (symbol (str "?" k "-holder"))]]
                         [[(symbol (str "?" k))
                           via-xref
                           (symbol (str "?" (:from col)))]])
                       [[(symbol (str "?" (:from col)))
                         via
                         (symbol (str "?" k))]])
                     (if (= (:db/cardinality vs) :db.cardinality/one)   ; vs not defined for xrefs
                       [[(list 'get-else
                              '$
                              (symbol (str "?" (:from col)))
                              (symbol via)
                              :none)
                        (symbol (str "?" k))]]
                       [(list (symbol (str "maybe-" k))
                             (symbol (str "?" (:from col)))
                             (symbol (str "?" k)))])))
                 
                 (if (and attr (= (name attr) "id"))
                   (if (:required col)
                     [[(symbol (str "?" k))
                       attr
                       (symbol (str "?" k "-id"))]]
                     [[(list 'get-else
                             '$
                             (symbol (str "?" k))
                             attr
                             :none)
                       (symbol (str "?" k "-id"))]]))

                 (if (and (= (:db/valueType vs) :db.type/ref)
                          (nil? (:pace/obj-ref vs))
                          (not (:db/isComponent vs)))
                   (if (:required col)
                     [[(symbol (str "?" k))
                       :db/ident
                       (symbol (str "?" k "-ident"))]]
                     [[(list 'get-else
                             '$
                             (symbol (str "?" k))
                             :db/ident
                             :none)
                       (symbol (str "?" k "-ident"))]]))

                 (if-let [con (:constrain col)]
                  (cond
                   (string? con) 
                   (if (not (empty? con))                     
                     (let [cleaned-con (str/replace con                 ;; escape all regex-special characters
                                                    #"[\[\].*+?{}()]"   ;; except *, which becomes ".*".
                                                    (fn [p]
                                                      (if (= p "*")
                                                        ".*"
                                                        (str \\ p))))]
                       (if (>= (.indexOf cleaned-con ".*") 0)
                         [[(list 're-pattern cleaned-con) (symbol (str "?" k "-regex"))]
                          [(list 're-matches
                                 (symbol (str "?" k "-regex"))
                                 (if (= (name attr) "id")
                                   (symbol (str "?" k "-id"))
                                   (symbol (str "?" k))))]]
                         [[(list 'ground con)
                           (if (= (name attr) "id")
                             (symbol (str "?" k "-id"))
                             (symbol (str "?" k)))]])))

                   (keyword? con)
                   [[(symbol (str "?" k)) :db/ident con]])))
                  
                        

        :rules
        (concat rules
                (if (and via (not (:required col)))
                  (cond
                   (= (:db/cardinality vs) :db.cardinality/many)
                   (let [rule (symbol (str "maybe-" k))
                         ?a (symbol "?a")
                         ?b (symbol "?b")]
                     [[(list rule ?a ?b)
                       [?a via ?b]]
                      [(list rule ?a ?b)
                       [(list 'missing? '$ ?a via)]
                       [(list 'ground (if (= (:db/valueType vs) :db.type/ref) -1 :none)) ?b]]])

                   via-xref
                   (if (= (count (str/split (namespace via-xref) #"\.")) 2)
                     (let [[x y] (str/split (namespace via-xref) #"\.")
                           rule  (symbol (str "maybe-" k))
                           ?a (symbol "?a")
                           ?b (symbol "?b")
                           ?h (symbol "?h")]
                       [[(list rule ?a ?b)
                         [?h via-xref ?a]
                         [?b (keyword x y) ?h]]
                        [(list rule ?a ?b)
                         [(list 'web.trace/imissing? '$ ?a via-xref)]
                         [(list 'ground -1) ?b]]])
                           
                     (let [rule (symbol (str "maybe-" k))
                           ?a (symbol "?a")
                           ?b (symbol "?b")]
                       [[(list rule ?a ?b)
                         [?b via-xref ?a]]
                        [(list rule ?a ?b)
                         [(list 'web.trace/imissing? '$ ?a via-xref)]
                         [(list 'ground -1) ?b]]])))))}))
                   
   {:find [] :where [] :rules []}
   columns))

(defn query-list [q]
  (vec
   (if (seq (:rules q))
     (concat [:find] (:find q) [:in '$ '%] [:where] (:where q))
     (concat [:find] (:find q) [:where] (:where q)))))

(defn query-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [q (get-query (:columns app) (:schema app))]
        (dom/pre nil
          (pr-str (query-list q))
          (if (seq (:rules q))
            (str "\nwith rules:\n" (pr-str (vec (:rules q))))))))))

(defn results-view [results owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/p nil "Showing " (count (:results results)) " of " (:count results))
        (let [vizcols (filter :visible (for [[cid col] (:columns results)] col))]
          (dom/table #js {:className "table table-striped"}
            (dom/thead nil
              (dom/tr nil                    
                (map (fn [col] (dom/td nil (:name col))) vizcols)))
            (dom/tbody nil
              (for [r (:results results)]
                (dom/tr nil   
                  (map
                   (fn [c col]
                     (dom/td nil
                       (if (not= c :none)      
                         (if (= (name (:attribute col)) "id")
                           (dom/a #js {:href (str "/view/" (namespace (:attribute col)) "/" c)} c)
                           (str c)))))
                    r vizcols))))))))))
                
(def col-seed (atom 1))

(defn colonnade-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr "/schema"
        (fn [resp]
          (om/update! app {:schema (process-schema resp)
                           :columns (array-map
                                     "col1" (assoc column-template :root true))}))))
    
    om/IRender
    (render [_]
      (dom/div
        nil
        (if (not (:schema app))
          (dom/h2 nil "Loading schema...")
          (dom/div nil
            (dom/div nil
              (for [[k col] (:columns app)]
                (om/build column-def-view col)))
            (dom/button
             #js {:onClick (fn [e]
                             (om/transact! app :columns
                                           (fn [cols]
                                             (let [cid (swap! col-seed inc)]
                                               (assoc cols
                                                 (str "col" cid)
                                                 (assoc column-template
                                                   :name (str "Column " cid)
                                                   :from "col1"))))))}
                                                 
             "New column")
            (dom/button
             {:disabled (= (:queryStatus app) :running)
              :on-click (fn [e]
                          (let [cols   (:columns @app)
                                schema (:schema @app)
                                q (get-query cols schema)]
                            (om/update! app :queryStatus :running)
                            (edn-xhr-post
                             "/colonnade/query"
                             {:query (query-list q)
                              :rules (vec (:rules q))
                              :max-rows 100}
                             (fn [resp]
                               (om/update! app
                                           :queryStatus (:status resp))
                               (om/update! app
                                           :results (if-let [r (:results resp)]
                                                      (assoc r :columns cols)))))))}
             
             "Run query")
                                                         
            (dom/h2 nil "Query")
            (om/build query-view app)

            (dom/h2 nil "Results")
            (case (:queryStatus app)
              nil
              nil

              :running
              (dom/img {:src "img/spinner_192.gif"})
              
              200
              (om/build results-view (:results app))

              ;;default
              (dom/div nil "Error " (:queryStatus app)))))))))
            

(om/root colonnade-view app-state {:target (gdom/getElement "table-maker")})
