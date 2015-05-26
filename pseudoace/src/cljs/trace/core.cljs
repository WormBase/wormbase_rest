(ns trace.core
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]
            [trace.utils :refer (edn-xhr edn-xhr-post conj-if process-schema)]
            [goog.dom :as gdom]
            [cljs-time.format :as time]
            [cljs-time.coerce :as tc]))

(enable-console-print!)

(def app-state (atom {:props [] 
                      :mode {:loading true
                             :editing false
                             :txnData false}}))

(def time-formatter (time/formatter "yyyy-MM-dd-HH:mm:ss"))
  
(defn mode
  "Get a ref-cursor to a map of application mode flags"
  []
  (om/ref-cursor (:mode (om/root-cursor app-state))))

(defn schema
  "Get a ref-cursor to a map of schema data, if loaded"
  []
  (om/ref-cursor (:schema (om/root-cursor app-state))))

(defn txns
  "Get a ref-cursor to a map of transactions"
  []
  (om/ref-cursor (:txns (om/root-cursor app-state))))

(def added-id (atom 0))

(defn- fetch-missing-txns [app]
  (let [txns (or (:txns @app) {})]
    (letfn [(scan-missing [missing {:keys [txn val]}]
              (let [missing 
                    (if (sequential? val)
                      (reduce scan-missing missing (mapcat :values val))
                      missing)]
                (if (txns txn)
                  missing
                  (conj missing txn))))]
      (let [missing (reduce scan-missing #{} (mapcat :values (:props @app)))]
        (if (seq missing)
          (edn-xhr
           (str "/txns?"
                (str/join "&"
                  (for [i missing]
                    (str "id=" i))))
           (fn [resp]
             (om/transact! app :txns #(merge %  (->> (for [t (:txns resp)]
                                                       [(:db/id t) t])
                                                     (into {})))))))))))
                           
(defn- props->state [props]
  (vec (for [p props :let [v (:values p)]]
         (assoc p
           :collapsed (> (count v 1))
           :values (vec
                    (for [{:keys [txn val id]} v]
                      {:txn txn
                       :id id
                       :key (:key p)
                       :val (if (:comp p)
                              (props->state val)
                              val)}))))))

(defn resp->state [resp]
  (let [txns (->> (for [t (:txns resp)]
                    [(:db/id t) t])
                  (into {}))]
    (props->state (:props resp))))

(deftype TempIDObj [part id]
  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (-write writer "#db/id[")
    (-pr-writer part writer opts)
    (-write writer " ")
    (-write writer (str id))
    (-write writer "]")))
      

(def tempid
  "Client-side analog of the Datomic tempid function.  Each call returns a unique 
   object which prints as a #db/id tagged literal."
  (let [seed (atom -1000)]
    (fn [part]
      (TempIDObj. part (swap! seed dec)))))

(declare tree-view)

(defn display [show]
  (if show
    {}
    {:display "none"}))

(defn text-edit [vh owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})
    
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [val (or (:edit vh) (:val vh))]
        (dom/span
         (dom/span {:style (display (not editing))
                    :on-double-click #(when js/trace_logged_in
                                        (om/set-state! owner :editing true))}
                   val)
         (dom/input
          {:style (display editing)
           :class "text-editable"
           :value val
           :on-change (fn [e]
                        (om/update! vh :edit (.. e -target -value)))
           :on-blur #(om/set-state! owner :editing false)
           :on-key-press (fn [e]
                           (when (== (.-charCode e) 13)
                             (om/set-state! owner :editing false)))}))))))


(defn int-edit [vh owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IRenderState 
    (render-state [_ {:keys [editing]}]
      (let [val (or (:edit vh) (:val vh))]   ;; works because 0 is truthy here.
       (dom/span
        (dom/span {:style (display (not editing))
                   :on-double-click #(when js/trace_logged_in
                                       (om/set-state! owner :editing true))}
                  val)
        (dom/input
         {:style (display editing)
          :value val
          :on-change (fn [e]
                       (let [ns (.. e -target -value)]
                         (if (re-matches #"\d+" ns)
                           (om/update! vh :edit (js/parseInt ns)))))
          :on-blur #(om/set-state! owner :editing false)
          :on-key-press (fn [e]
                          (when (== (.-charCode e) 13)
                            (om/set-state! owner :editing false)))}))))))

(defn boolean-edit [vh owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [val (if (nil? (:edit vh))
                  (:val vh)
                  (:edit vh))]
        (dom/span
         (dom/span {:style (display (not editing))
                    :on-double-click #(om/set-state! owner :editing true)}
                   (str val))
         (dom/select
          {:style (display editing)
           :value (str val)
           :on-change (fn [e]
                        (om/update! vh :edit (= (.. e -target -value) "true"))
                        (om/set-state! owner :editing false))}
          (dom/option {:value "false"} "false")
          (dom/option {:value "true"} "true")))))))

(defn enum-edit [vh owner {:keys [tns]}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [val (or (:edit vh) (:val vh))
            schema (om/observe owner (schema))
            enum-values ((:attrs schema) tns)]
        (dom/span
         (dom/span {:style (display (not editing))
                    :on-double-click #(om/set-state! owner :editing true)}
                   (str val))
         (dom/select
          {:style (display editing)
           :value (str (namespace val) "/" (name val))
           :on-change (fn [e]
                        (om/update! vh :edit (keyword (.. e -target -value)))
                        (om/set-state! owner :editing false))}
          (for [ev enum-values
                :let [id (:db/ident ev)]]
            (dom/option {:value (str (namespace id) "/" (name id))}
                        (str id)))))))))

(defn ref-edit [vh owner {:keys [class]}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false
       :ncand -1
       :candidates nil})

    om/IRenderState
    (render-state [_ {:keys [editing ncand candidates checked]}]
      (let [val (or (:edit vh) (:val vh))]
        (letfn [(update-cands [prefix]
                  (when (not= checked prefix)
                    (edn-xhr
                     (str "/prefix-search?class=" (namespace class) "&prefix=" prefix)
                     (fn [{ncnt :count names :names}]
                       (om/set-state! owner :checked prefix)
                       (om/set-state! owner :ncand ncnt)
                       (om/set-state! owner :candidates names)))))]
        (dom/span
         (dom/span {:style (display (not editing))
                    :on-double-click (fn [_]
                                       (update-cands (second val))
                                       (om/set-state! owner :editing true))}
                   (str (second val)))
         (dom/input
          {:style (display editing)
           :value (second val)
           :on-change (fn [e]
                        (let [p (.. e -target -value)]
                          (update-cands p)
                          (om/update! vh :edit [class p])))
           :on-blur #(om/set-state! owner :editing false)
           :on-key-press (fn [e]
                           (when (== (.-charCode e) 13)
                             (om/set-state! owner :editing false)))})

         (dom/span
          {:style (assoc (display (and (= checked (second val))
                                       (not= (first candidates) (second val))))
                    :color "red")}
          " Doesn't exist (TBD: add option to create)")
         
         (dom/div
          {:style (display editing)
           :class "candidate-list"}
          (for [c candidates]
            (dom/div {:class "candidate-item"} c))
          (if (> ncand (count candidates))
            (dom/div (dom/em (str "And " (- ncand (count candidates)) " more")))))))))))
      
(defn txn-view [{:keys [txn] :as val-holder} owner {:keys [entid key]}]
  (reify
    om/IInitState
    (init-state [_]
      {:history false})

    om/IRenderState
    (render-state [_ {:keys [history hdata]}]
     (let [txn-map (om/observe owner (txns))]
      (dom/span 
       {:class "txn"
        :on-click (fn [_]
                    (when (and (not history)
                             (not hdata))
                      (edn-xhr
                       (str "/history2/" entid "/" key)
                       (fn [resp]
                         (om/set-state! owner :hdata resp))))
                    (om/update-state! owner :history not))}
       (if history
         (dom/div {:class "history-box-holder"}
            (dom/div {:class "history-box"} 
               (if hdata
                 (let [txmap (->> (map (juxt :db/id identity) (:txns hdata))
                                  (into {}))]
                   (dom/table {:class "history-table table table-striped"}
                    (dom/thead 
                     (dom/tr
                      (for [c ["Date" "Action" "Value" "Who?"]]
                        (dom/th c))))
                    (dom/tbody
                     (for [datoms (->> (sort-by :txid (:datoms hdata))
                                       (partition-by :txid))
                           :let [{added true retracted false}
                                   (group-by :added? datoms)
                                 txn (txmap (:txid (first datoms)))
                                 time (->> (:db/txInstant txn)
                                           (tc/from-date)
                                           (time/unparse time-formatter))
                                 who (if-let [c (:wormbase/curator txn)]
                                       (second c)
                                       (:importer/ts-name txn))]]
                       (if (= (count added) (count retracted) 1)
                         (dom/tr
                          (dom/td time)
                          (dom/td "changed")
                          (dom/td (:v (first added)))
                          (dom/td who))
                         (concat
                          (for [d retracted]
                            (dom/tr
                             (dom/td time)
                             (dom/td "retracted")
                             (dom/td (:v d))
                             (dom/td who)))
                          (for [d added]
                            (dom/tr
                             (dom/td time)
                             (dom/td "added")
                             (dom/td (:v d))
                             (dom/td who)))))))))
                 (dom/img {:src "/img/spinner_24.gif"})))))
        (if txn
          (if-let [txn-data (txn-map txn)]
            (str (time/unparse time-formatter (tc/from-date (:db/txInstant txn-data)))
                 (if-let [c (:wormbase/curator txn-data)]
                   (str " (" (second c) ")")
                   (if-let [d (:importer/ts-name txn-data)]
                     (str " (" d ")"))))
            (str txn))
          "NEW"))))))

(defn item-view [{:keys [val edit txn remove added] :as val-holder} 
                 owner 
                 {:keys [key type entid comp?]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (if (= added @added-id)
        (.scrollIntoView (om/get-node owner))))

    om/IRender
    (render [_]
     (let [mode      (om/observe owner (mode))
           txnData   (:txnData mode)
           edit-mode (:editing mode)]
      (dom/div 
       {:class (if edit 
                  "trace-item edited"
                  "trace-item")}

       (if (and edit-mode (not comp?))
         (dom/button
          {:on-click #(om/transact! val-holder :remove not)}
          (dom/i {:class "fa fa-eraser"})))
       
       (if txnData
         (om/build txn-view val-holder {:opts {:key key :entid entid}}))
       
       (dom/span
        {:class "trace-item-content"
         :style (if remove
                  {:text-decoration "line-through"
                   :text-decoration-color "red"})}
        (cond
         comp?
         (om/build tree-view val-holder)
         
         (or (sequential? val)
             (sequential? edit))
         (if edit-mode
           (om/build ref-edit val-holder
                     {:opts {:class (:pace/obj-ref ((:attrs-by-ident (schema)) key))}})
           (let [id (second val)
                 uri (str "/view/" (namespace (first val)) "/" (second val))]
             (dom/a {:href uri
                     :onClick (fn [e]
                                (.preventDefault e)
                                (.stopPropagation e)
                                (.pushState js/window.history
                                            #js {:url uri}
                                            id
                                            uri)
                                (secretary/dispatch! uri))}
                    (str id))))

         (= type :db.type/long)
         (if edit-mode
           (om/build int-edit val-holder)
           (dom/span (str val)))
         
         (= type :db.type/boolean)
         (if edit-mode
           (om/build boolean-edit val-holder)
           (dom/span (str val)))

         (and (= type :db.type/ref)
              (keyword? val))
         (if edit-mode
           (om/build enum-edit val-holder {:opts {:tns (str (namespace key) "." (name key))}})
           (dom/span (str val)))

         (= type :db.type/string)
         (if edit-mode
           (om/build text-edit val-holder)
           (dom/span (str val)))

         :default
         (dom/span (str val)))))))))
      

(defn list-view [data owner {:keys [entid]}]
  (reify
    om/IRender
    (render [_]
      (let [mode (om/observe owner (mode))
            vcnt (or (:count data)
                     (count (:values data)))]
        (if (> vcnt 1)
          (dom/span {:className "values-holder"}
                    (dom/button 
                     {:onClick (fn [_]
                                 (om/transact! data :collapsed not)
                                 (if (and (not (:collapsed @data))
                                          (empty? (:values @data)))
                                   (edn-xhr
                                    (str "/attr2/" entid "/" (:key data))
                                    (fn [resp]
                                      (let [txn-map (->> (for [t (:txns resp)]
                                                        [(:db/id t) t])
                                                      (into {}))]
                                        (om/transact!
                                         (txns)
                                         #(merge % txn-map))
                                        (om/update!
                                         data
                                         :values
                                         (vec
                                          (for [v (:values resp)]
                                            (assoc v
                                              :txn (:txn v)
                                              :val (if (:comp resp)
                                                     (props->state (:val v))
                                                     (:val v))))))
                                        (if (:txnData @mode)
                                          (fetch-missing-txns (om/root-cursor app-state))))))))
                      :className "collapse-button"}
                     (if (:collapsed data)
                       "+"
                       "-"))
                    (cond
                     (:collapsed data)
                     (dom/div nil (str vcnt " nodes"))

                     (empty? (:values data))
                     (dom/img {:src "/img/spinner_24.gif"})
                     
                     :default
                     (dom/div nil
                              (for [v (:values data)]
                                (om/build item-view v
                                  {:opts {:key   (:key data)
                                          :type  (:type data)
                                          :comp? (:comp data)
                                          :entid entid}})))))
          (let [[v] (:values data)]
            (om/build item-view v 
              {:opts {:key     (:key data)
                      :type    (:type data)
                      :comp?   (:comp data)
                      :entid   entid}})))))))

(defn dummy-item [item]
  (case (:db/valueType item)
    :db.type/string
    "foo"

    :db.type/long
    1234

    :db.type/float
    12.34

    :db.type/double
    12.34

    :db.type/boolean
    true

    :db.type/instant
    #inst "1977-10-29"
    
    :db.type/ref
    (cond
     (:db/isComponent item)
     []    ;; empty prop-list

     (:pace/obj-ref item)
     [(:pace/obj-ref item) "dummy"]

     :default
     :something.sensible/for-enum?)))

(defn add-item [data item]
  (om/transact! (or (:props data)
                    (:edit data)
                    (:val data))
                (fn [props]
                  (let [dummy          {:key   (:db/ident item)
                                        :added (swap! added-id inc)
                                        :edit  (dummy-item item)}
                        attrs          (get-in @app-state [:schema :attrs-by-ident])
                        [[idx holder]] (keep-indexed #(if (= (:key %2) (:db/ident item)) [%1 %2]) props)]
                    (if holder
                      (assoc props idx (assoc holder
                                         :values (conj (:values holder) dummy)
                                         :collapsed false))
                      (->> (conj props {:key (:db/ident item)
                                        :type (:db/valueType item)
                                        :collapsed false
                                        :comp (:db/isComponent item)
                                        :values [dummy]})
                           (sort-by 
                            (fn [{:keys [key]}]
                              (:db/id (attrs key))))))))))
                                         

(defn add-button [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:open false})
    om/IRenderState
    (render-state [_ {:keys [open]}]
      (let [schema (om/observe owner (schema))
            tns (cond
                 (:ident data)
                 [(namespace (first (:ident data)))]

                 (:key data)
                 (conj
                  (:pace/use-ns ((:attrs-by-ident schema) (:key data)))
                  (str (namespace (:key data)) "." (name (:key data)))))
            attrs (mapcat (:attrs schema) tns)
            used-props (->> (or (:props data)
                                (:val data)
                                (:edit data))
                            (map :key)
                            (set))]
        (dom/span
         (dom/button
          {:on-click #(om/set-state! owner :open (not open))
           :on-blur (fn [_]   ;; delay hiding menu so we don't prevent item clicks.
                      (js/setTimeout #(om/set-state! owner :open false) 200))}
          "Add")
         (when open
           (dom/div
            {:class "add-list"}
            (for [a attrs]
              (dom/div
               {:class (if (and (= (:db/cardinality a) :db.cardinality/one)
                                (used-props (:db/ident a)))
                         "add-item add-item-disabled"
                         "add-item")
                :on-click #(add-item data a)}
               (str (:db/ident a)))))))))))
       

(defn tree-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [mode (om/observe owner (mode))]
        (dom/div
         (when (:editing mode) 
           (om/build add-button data))
         (dom/table {:border "1"
                     :className "trace-tree table table-striped table-condensed"}
            (dom/tbody nil
                (for [prop (or (:props data)
                               (:edit data)
                               (:val data))]
                (dom/tr nil 
                        (dom/td nil (str (:key prop)))
                        (dom/td nil (om/build list-view prop 
                                              {:opts 
                                               {:entid (:id data)}})))))))))))

(defn- pack-id [id]
  (if (string? id)
    [:db/id id]
    id))

(defn gather-txdata [id props]
  (reduce
    (fn [txlist prop]
      (if (:comp prop)
        (reduce
         (fn [txlist val]
           (if (:edit val)
             (let [t (tempid :db.part/user)]
               (concat txlist
                       [[:db/add (pack-id id) (:key prop) t]]
                       (gather-txdata t (:edit val))))
             (concat txlist (gather-txdata (:id val) (:val val)))))
         txlist
         (:values prop))
        
        (reduce
         (fn [txlist {:keys [edit val remove]}]
           (let [is-edit (not (nil? edit))]
             (conj-if
              txlist
              (if (or (and is-edit val)
                      remove)
                [:db/retract (pack-id id) (:key prop) val])
              (if (and is-edit (not remove))
                [:db/add (pack-id id) (:key prop) edit]))))
         txlist
         (:values prop))))
    [] props))
        
(defn submit [app]
  (edn-xhr-post
   "/transact"
   {:tx (gather-txdata (:id @app-state) (:props @app-state))}
   (fn [{:keys [status responseText]}]
     (if (= status 200)
       (secretary/dispatch! (.-pathname js/window.location))
       (om/update! app :err responseText)))))

(defn trace-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (defroute "/view/:class/:id" {c :class i :id}
        (om/update! app [:mode :loading] true)
        (edn-xhr 
         (str "/raw2/" c "/" i "?max-in=5&max-out=10&txns=false")
         (fn [resp]
           (om/transact! app (fn [app]
                               (assoc app 
                                      :mode {:loading false
                                             :editing false
                                             :txnData false}
                                      :props   (props->state (:props resp))
                                      :txns    (->> (for [t (:txns resp)]
                                                      [(:db/id t) t])
                                                    (into {}))
                                      :id      (:id resp)
                                      :ident [(keyword c "id") i]))))))
      (secretary/dispatch! (.-pathname js/window.location))
      (.addEventListener js/window
                         "popstate"
                         (fn [e]
                           (secretary/dispatch! (.-pathname js/window.location)))))
    om/IRender
    (render [_]
      (let [mode (:mode app)]
        (dom/div
         nil
         (dom/div {:class "trace-header"}
                  (dom/h1 nil
                          (str (or (om/value (:ident app))
                                   "TrACeView"))
                          (if-let [uid js/trace_logged_in]
                            (str " - " uid)))
                  
                  (dom/label "Timestamps")
                  (dom/input {:type "checkbox"
                              :checked (:txnData (:mode app))
                              :on-click (fn [_]
                                          (if (:txnData (:mode @(om/transact! app [:mode :txnData] not)))
                                            (fetch-missing-txns app)))})
                  
                  (when (:fetching-schema mode)
                    (dom/span "Fetching schema..."))

                  (when-not (:fetching-schema mode)
                    [
                    (when (and js/trace_logged_in
                               (not (:editing mode)))
                      (dom/button {:on-click (fn [ev]
                                               (if (:schema app)
                                                 (om/update! app [:mode :editing] true)
                                                 (do
                                                   (om/update! app [:mode :fetching-schema] true)
                                                   (edn-xhr 
                                                    "/schema"
                                                    (fn [resp]
                                                      (om/update! app :schema (process-schema resp))
                                                      (om/update! app [:mode :fetching-schema] false)
                                                      (om/update! app [:mode :editing] true))))))}
                                   "Edit"))

                      (when (and js/trace_logged_in
                                 (:editing mode))
                        (dom/button {:on-click #(submit app)
                                     :disabled (empty? (gather-txdata (:id @app-state) (:props @app-state)))}
                                    "Submit"))
          
                      (when (and js/trace_logged_in
                                 (:editing mode))
                        (dom/button {:on-click #(secretary/dispatch! (.-pathname js/window.location))}
                                    "Cancel"))])

                  (dom/span {:style {:color "red"}} (:err app)))

         (dom/div {:class "trace-body"}
                  (if (:loading mode)
                    (dom/img {:src "/img/spinner_192.gif"})
                    (om/build tree-view app))))))))
                           

(om/root trace-view app-state {:target (gdom/getElement "tree")})
