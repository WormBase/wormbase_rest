(ns trace.core
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]
            [trace.utils :refer (edn-xhr edn-xhr-post conj-if process-schema)]
            [goog.dom :as gdom]
            [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]))

(enable-console-print!)

(def app-state (atom {:props [] 
                      :mode {:loading true
                             :editing false
                             :txnData false}}))

(def time-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn format-local [time]
  (->> (tc/from-date time)
       (time/to-default-time-zone)
       (tf/unparse-local-date time-formatter)))

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

(defn component-ns
  "Get the primary namespace for a component attribute"
  [ident]
  (str (namespace ident) "." (name ident)))

(def added-id (atom 0))

(def ^:private top-bar-height 100) ;; px

(defn scroll-into-view 
  "Attempt to make `element` visible.  Takes into account the height of the
   non-scrolling toolbar (which breaks Element.prototype.scrollIntoView)."
  [element]
  (let [body        (.-body js/document)
        scroll-top  (.-scrollTop body)
        scroll-bot  (+ (.-scrollTop body) (.-clientHeight body))
        rect        (.getBoundingClientRect element)
        element-top (.-top rect)
        element-bot (.-bottom rect)]
    (cond 
      (< element-top top-bar-height)
      (set! (.-scrollTop body) (max 0 (+ scroll-top (- element-top top-bar-height))))

      (> element-bot scroll-bot)
      (set! (.-scrollTop body) (max 0 (+ scroll-top (- element-bot scroll-bot)))))))

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
           :collapsed (> (or (:count p)
                             (count v)) 1)
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

(defn display
  [show]
  (if show
    {}
    {:display "none"}))

(defn text-edit [{:keys [added] :as vh} owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (if (= added @added-id)
        (.focus (om/get-node owner "input"))))
    
    om/IInitState
    (init-state [_]
      {:editing (= added @added-id)})
    
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [val (or (:edit vh) (:val vh))]
        (dom/span {:class "edit-root"}
         (dom/span {:class "edit-inactive"
                    :style (display (not editing))
                    :on-double-click (fn [event]
                                       (when js/trace_logged_in
                                         (.preventDefault event)
                                         (.stopPropagation event)
                                         (om/set-state! owner :editing true)))}
                   (if (= val :empty) "New value..." val))
         (dom/input
          {:ref "input"
           :style (display editing)
           :class "text-editable"
           :placeholder "Enter text..."
           :value (if (= val :empty) "" val )
           :on-change (fn [e]
                        (om/update! vh :edit (.. e -target -value)))
           :on-blur #(om/set-state! owner :editing false)
           :on-key-press (fn [e]
                           (when (== (.-charCode e) 13)
                             (om/set-state! owner :editing false)))}))))

    om/IDidUpdate
    (did-update [_ _ {:keys [editing]}]
      (if (and (not editing)
               (om/get-state owner :editing))
        (.focus (om/get-node owner "input"))))))
      


(defn int-edit [{:keys [added] :as vh} owner]
  (reify

    om/IDidMount
    (did-mount [_]
      (if (= added @added-id)
        (.focus (om/get-node owner "input"))))
    
    om/IInitState
    (init-state [_]
      {:editing (= added @added-id)})

    om/IRenderState 
    (render-state [_ {:keys [editing]}]
      (let [val (or (:edit vh) (:val vh))]   ;; works because 0 is truthy here.
       (dom/span {:class "edit-root"}
        (dom/span {:class "edit-inactive"
                   :style (display (not editing))
                   :on-double-click (fn [event]
                                       (when js/trace_logged_in
                                         (.preventDefault event)
                                         (.stopPropagation event)
                                         (om/set-state! owner :editing true)))}
                  (if (= val :empty) "New value..." (str val)))
        (dom/input
         {:ref "input"
          :style (display editing)
          :class "text-editable"
          :placeholder "Enter number"
          :value (if (= val :empty) "" (str val))
          :on-change (fn [e]
                       (let [ns (.. e -target -value)]
                         (if (re-matches #"\d+" ns)
                           (om/update! vh :edit (js/parseInt ns)))))
          :on-blur #(om/set-state! owner :editing false)
          :on-key-press (fn [e]
                          (when (== (.-charCode e) 13)
                            (om/set-state! owner :editing false)))}))))

    om/IDidUpdate
    (did-update [_ _ {:keys [editing]}]
      (if (and (not editing)
               (om/get-state owner :editing))
        (.focus (om/get-node owner "input"))))))

(defn boolean-edit [vh owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [val (if (nil? (:edit vh))
                  (:val vh)
                  (:edit vh))
            val (if (= val :empty) "New value..." val)]
        (dom/span {:class "edit-root"}
         (dom/span {:class "edit-inactive"
                    :style (display (not editing))
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
            val (if (= val :empty) "New value..." val)
            schema (om/observe owner (schema))
            enum-values ((:attrs schema) tns)]
        (dom/span {:class "edit-root"}
         (dom/span {:class "edit-inactive"
                    :style (display (not editing))
                    :on-double-click #(om/set-state! owner :editing true)}
                   (if (keyword? val)
                     (name val)
                     (str val)))
         (dom/select
          {:style (display editing)
           :value (if (keyword? val) (str (namespace val) "/" (name val)))
           :on-change (fn [e]
                        (om/update! vh :edit (keyword (.. e -target -value)))
                        (om/set-state! owner :editing false))}
          (for [ev enum-values
                :let [id (:db/ident ev)]]
            (dom/option {:value (str (namespace id) "/" (name id))}
                        (name id)))))))))

(defn ref-edit [{:keys [added] :as vh} owner {:keys [class]}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing (= added @added-id)
       :ncand -1
       :candidates nil})

    om/IDidMount
    (did-mount [_]
      (if (= added @added-id)
        (.focus (om/get-node owner "input"))))

    om/IDidUpdate
    (did-update [_ _ {:keys [editing]}]
      (if (and (not editing)
               (om/get-state owner :editing))
        (.focus (om/get-node owner "input"))))

    om/IRenderState
    (render-state [_ {:keys [editing ncand candidates checked]}]
      (let [val   (or (:edit vh) (:val vh))
            vname (if (not= val :empty)
                    (second val))
            create? (and (sequential? val) (= (last val) :create))]
       (letfn [(update-cands [prefix]
                  (when (not= checked prefix)
                    (edn-xhr
                     (str "/prefix-search?class=" (namespace class) "&prefix=" prefix)
                     (fn [{ncnt :count names :names}]
                       (om/set-state! owner :checked prefix)
                       (om/set-state! owner :ncand ncnt)
                       (om/set-state! owner :candidates names)))))]
        (dom/span {:class "edit-root" 
                   :tabIndex 0
                   :on-focus #(om/set-state! owner :editing true)}
         (dom/i {:class "fa fa-plus-circle"
                 :style (display create?)})
         (dom/span {:class "edit-inactive"
                    :style (display (not editing))
                    :on-double-click (fn [event]
                                       (update-cands vname)
                                       (.preventDefault event)
                                       (.stopPropagation event)
                                       (om/set-state! owner :editing true))}
                   (or vname "New reference..."))
         
         (dom/div
          {:style (display editing)
           :class "candidate-list"}
          (for [c candidates]
            (dom/div
             {:class "candidate-item"
              :on-click (fn [_]
                          (update-cands c)
                          (om/update! vh :edit [class c])
                          (om/set-state! owner :editing false))}
             c))
          (if (> ncand (count candidates))
            (dom/div (dom/em (str "And " (- ncand (count candidates)) " more")))))
         
         (dom/input
          {:ref "input"
           :style (display editing)
           :value vname
           :placeholder (str class " id...")
           :on-change (fn [e]
                        (let [p (.. e -target -value)]
                          (update-cands p)
                          (om/update! vh :edit [class p])))
           :on-blur (fn [_]
                      (js/setTimeout #(om/set-state! owner :editing false) 200))
           :on-key-press (fn [e]
                           (when (== (.-charCode e) 13)
                             (om/set-state! owner :editing false)))})



         (dom/span
          {:style {:visibility (if (and (not create?)
                                        (= checked vname)
                                        (not= (first candidates) vname))
                                 "visible"
                                 "hidden")}}
          (dom/span
           {:style {:color "red"}}
           " Doesn't exist ")
          (dom/button
           {:on-click (fn [_]
                        (om/update! vh :edit [class vname :create])
                        (om/set-state! owner :editing false))}
           "Create"))
         
            ))))))
      
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
                                           (format-local))
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
            (str (->> (:db/txInstant txn-data)
                      (format-local))
                 (if-let [c (:wormbase/curator txn-data)]
                   (str " (" (second c) ")")
                   (if-let [d (:importer/ts-name txn-data)]
                     (str " (" d ")"))))
            (str txn))
          "NEW"))))))

(defn item-view [{:keys [val edit txn remove added] :as val-holder} 
                 owner 
                 {:keys [key type entid class comp?]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (if (= added @added-id)
        (scroll-into-view (om/get-node owner))))

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
          {:tabIndex -1
           :on-click #(om/transact! val-holder :remove not)}
          (dom/i {:class "fa fa-eraser"})))
       
       
       
       (dom/span
        {:class "trace-item-content"
         :style (if remove
                  {:text-decoration "line-through"
                   :text-decoration-color "red"})}
        (cond
         comp?
         (om/build tree-view val-holder {:opts {:primary-ns (component-ns key)}})
         
         (or (sequential? val)
             (sequential? edit)
             class)
         (if edit-mode
           (om/build ref-edit val-holder
                     {:opts {:class class}})
           (let [[class id title] val
                 uri (str "/view/" (namespace class) "/" id)]
             (dom/a {:href uri
                     :onClick (fn [e]
                                (.preventDefault e)
                                (.stopPropagation e)
                                (.pushState js/window.history
                                            #js {:url uri}
                                            id
                                            uri)
                                (secretary/dispatch! uri))}
                    (str (or title id)))))

         (= type :db.type/long)
         (if edit-mode
           (om/build int-edit val-holder)
           (dom/span (str val)))
         
         (= type :db.type/boolean)
         (if edit-mode
           (om/build boolean-edit val-holder)
           (dom/span (str val)))

         (and (= type :db.type/ref))
         (if edit-mode
           (om/build enum-edit val-holder {:opts {:tns (str (namespace key) "." (name key))}})
           (dom/span (name val)))

         (= type :db.type/string)
         (if edit-mode
           (om/build text-edit val-holder)
           (dom/span (str val)))

         (= type :db.type/instant)
         (dom/span (format-local val))
         
         :default
         (dom/span (str val))))

       (if txnData
         (om/build txn-view val-holder {:opts {:key key :entid entid}}))
       )))))
      

(defn list-view [data owner {:keys [entid]}]
  (reify
    om/IRender
    (render [_]
      (let [mode (om/observe owner (mode))
            vcnt (or (:count data)
                     (count (:values data)))]
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
                      :className "collapse-button"
                      :style (display (> vcnt 1))}
                     (if (:collapsed data)
                       "+"
                       "-"))
                    (cond
                     (:collapsed data)
                     (dom/div nil (str vcnt " nodes"))

                     (empty? (:values data))
                     (dom/img {:src "/img/spinner_24.gif"})
                     
                     :default
                     (dom/div {:class "values-list"}
                              (for [v (:values data)]
                                (om/build item-view v
                                  {:opts {:key   (:key data)
                                          :type  (:type data)
                                          :class (:class data)
                                          :comp? (:comp data)
                                          :entid entid}})))))))))

(defn- dummy-item* [item]
  (if (:db/isComponent item)
    (let [ident (:db/ident item)
          cns   (str (namespace ident) "." (name ident))
          attrs (->> (get-in @app-state [:schema :attrs cns])
                     (mapv (fn [item]
                             {:key (:db/ident item)
                              :type (:db/valueType item)
                              :collapsed false
                              :comp (:db/isComponent item)
                              :class (:pace/obj-ref item)
                              :values [(dummy-item* item)]})))]
      {:key     (:db/ident item)
       :edit    attrs})
     {:key   (:db/ident item)
      :edit  :empty}))

(defn- dummy-item [item]
  (let [dummy (dummy-item* item)
        added (swap! added-id inc)]
    (if (and (vector? (:edit dummy))
             (seq (:edit dummy)))
      (assoc-in dummy [:edit 0 :values 0 :added] added)
      (assoc dummy :added added))))

(defn add-item [data item]
  (om/transact! (or (:props data)
                    (:edit data)
                    (:val data))
                (fn [props]
                  (let [dummy          (dummy-item item)
                        attrs          (get-in @app-state [:schema :attrs-by-ident])
                        [[idx holder]] (keep-indexed #(if (= (:key %2) (:db/ident item)) [%1 %2]) props)]
                    (if holder
                      (assoc props idx (assoc holder
                                         :values (conj (:values holder) dummy)
                                         :collapsed false))
                      (->> (conj props {:key (:db/ident item)
                                        :group (if-let [tags (:pace/tags item)]
                                                 (first (str/split tags #"\s")))
                                        :type (:db/valueType item)
                                        :collapsed false
                                        :comp (:db/isComponent item)
                                        :class (:pace/obj-ref item)
                                        :values [dummy]})
                           (sort-by 
                            (fn [{:keys [key]}]
                              (if (= (.charAt (name key) 0) "_") 
                                1000000000000           ;; Keep inbound XREFs at end.  They should remain stable.
                                (:db/id (attrs key)))))
                           (vec)))))))
                                         

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
         {:class "add-button-holder"}
         (dom/button
          {:on-click #(om/set-state! owner :open (not open))
           :on-blur (fn [_]   ;; delay hiding menu so we don't prevent item clicks.
                      (js/setTimeout #(om/set-state! owner :open false) 200))}
          "Add")
         (when open
           (dom/div
            {:class "add-list dropdown-menu"
             :style {:display "block"}}
            (for [a attrs]
              (dom/div
               {:class (if (and (= (:db/cardinality a) :db.cardinality/one)
                                (used-props (:db/ident a)))
                         "add-item add-item-disabled"
                         "add-item")
                :on-click (fn [_]
                            (om/set-state! owner :open false)
                            (add-item data a))}
               (str (:db/ident a)))))))))))
       

(defn- group-props [props]
  (loop [grouped         (group-by :group props)
         [prop & props]  props
         group-seq       []]
    (if prop
      (let [g (:group prop)]
        (if-let [gg (grouped g)]
          (recur (dissoc grouped g)
                 props
                 (conj group-seq [g gg]))
          (recur grouped props group-seq)))
      group-seq)))

(defn tree-view [data owner {:keys [primary-ns group?]}]
  (reify
    om/IInitState
    (init-state [_]
      {})
    
    om/IRenderState
    (render-state [_ state]
      (let [mode  (om/observe owner (mode))
            props (or (:props data)
                      (:edit data)
                      (:val data))
            grouped-props (if group?
                            (group-props props)
                            [[nil props]])]
        (dom/div
         (when (:editing mode) 
           (om/build add-button data))
         (dom/table {:border "1"
                     :className "trace-tree table table-striped table-condensed"}
          (dom/tbody nil
           (for [[group-label props] grouped-props]
            (list
              (if group-label
                (dom/tr {:style {:background "darkgray"}
                         :on-click #(om/update-state! owner group-label not)}
                        (dom/td {:colSpan 2}
                                (str "[" (if (state group-label) "+" "-") "] "
                                     group-label))))
              (for [prop props]
                (dom/tr {:style (display (not (state group-label)))}
                        (dom/td {:class "prop-name"}
                           (let [key (:key prop)]
                             (if (= (namespace key) primary-ns)
                               (name key)
                               (str key))))
                        (dom/td {:class "prop-val"}
                           (om/build list-view prop 
                                     {:key :key      ;; Need to explicitly provide a react key
                                                     ;; here other wise some very silly element
                                                     ;; recycling can occur when a new property
                                                     ;; gets inserted.
                                      :opts 
                                      {:entid (:id data)}})))))))))))))

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
               (if-let [comp-tx (seq (gather-txdata t (:edit val)))]
                 (concat txlist
                         [[:db/add (pack-id id) (:key prop) t]]
                         comp-tx)
                 txlist))  ;; Don't add component-link datom if children are empty.
             (concat txlist (gather-txdata (:id val) (:val val)))))
         txlist
         (:values prop))
        
        (reduce
         (fn [txlist {:keys [edit val remove]}]
           (let [is-edit (not (nil? edit))]
             (concat
              txlist
              (if (and (or (and is-edit val)
                           remove)
                       val)       ;; Don't explicitly retract nils
                [[:db/retract (pack-id id) (:key prop) val]])
              (if (and is-edit (not remove) (not= edit :empty))
                (if (and (sequential? edit)
                         (= (last edit) :create))
                  (let [[class new-id] edit
                        t              (tempid :db.part/user)]
                    [[:db/add t class new-id]
                     [:db/add (pack-id id) (:key prop) t]])
                  [[:db/add (pack-id id) (:key prop) edit]])))))
         txlist
         (:values prop))))
    [] props))
        
(defn submit [app]
  (edn-xhr-post
   "/transact"
   {:tx (gather-txdata (:id @app-state) (:props @app-state))}
   (fn [{:keys [status responseText]}]
     (if (= status 200)
       (do
         (om/update! app :err nil)
         (secretary/dispatch! (.-pathname js/window.location)))
       (om/update! app :err responseText)))))

(defn trace-title [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/span
       (str (or (om/value (:ident app))
                                   "TrACeView"))))))

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
      (dom/div {:class "trace-body"}
               (if (:loading mode)
                 (dom/img {:src "/img/spinner_192.gif"})
                 (om/build tree-view app {:opts {:group? true
                                                 :primary-ns (some-> (first (:ident app))
                                                                     (namespace))}}))))))
                           

(defn trace-tools [app owner]
  (reify
    om/IRender
    (render [_]
      (let [mode (:mode app)]
        (dom/div
         nil
         (dom/div {}
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

                      #_(when (and js/trace_logged_in
                                 (:editing mode))
                        (dom/button {:on-click #(println (gather-txdata (:id @app-state) (:props @app-state)))}
                                    "Preview"))

                      (when (and js/trace_logged_in
                                 (:editing mode))
                        (dom/button {:on-click #(submit app)
                                     :disabled (empty? (gather-txdata (:id @app-state) (:props @app-state)))}
                                    "Save"))
          
                      (when (and js/trace_logged_in
                                 (:editing mode))
                        (dom/button {:on-click #(secretary/dispatch! (.-pathname js/window.location))}
                                    "Cancel"))])

                  (dom/span {:style {:color "red"}} (:err app))))))))

(defn init-trace []
  (om/root trace-view    app-state {:target (gdom/getElement "tree")})
  (om/root trace-title   app-state {:target (gdom/getElement "page-title")})
  (om/root trace-tools   app-state {:target (gdom/getElement "header-content")})

  ;; Non-OM code, needs to explicitly deref the app-state atom to
  ;; see if we're currently editing.

  (.addEventListener
   js/window
   "beforeunload"
   (fn [e]
     (when (and (:editing (:mode @app-state))
                (seq (gather-txdata (:id @app-state) (:props @app-state))))
       (set! (.-returnValue e) "Currently editing this entity")
       "Currently editing this entity"))))
