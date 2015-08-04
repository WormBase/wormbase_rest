(ns trace.utils
  (:require [cljs.reader :as reader]))

(defn edn-xhr
  "Fetch `uri`.  Invoke `callback` with an edn-parsed response, or `nil` if the request fails."
  [uri callback]
  (let [x (js/XMLHttpRequest.)]
    (.open x "GET" uri true)
    (.addEventListener
       x
       "load" 
       (fn []
         (callback
          (if (= (.-status x) 200)
            (reader/read-string (.-responseText x)))))
       false)
    (.send x)))

(defn edn-xhr-post [uri body callback]
  (let [x (js/XMLHttpRequest.)]
    (.open x "POST" uri true)
    (.addEventListener
       x
       "load" 
       (fn []
         (callback {:status (.-status x)
                    :results (if (= (.-status x) 200)
                               (reader/read-string (.-responseText x)))
                    :responseText (if (not= (.-status x) 200)
                                    (.-responseText x))}))
       false)
    (.setRequestHeader x "Content-Type" "application/edn")
    (.setRequestHeader x "X-XSRF-Token" js/trace_token)
    (.send x (pr-str body))))

(defn conj-if
  "Conjoin any true values to coll."
  ([coll x] (if x (conj coll x) coll))
  ([coll x & xs]
     (if xs
       (recur (conj-if coll x) (first xs) (next xs))
       (conj-if coll x))))

(defn process-schema [{:keys [classes attributes]}]
  {:classes (sort-by :pace/identifies-class classes)
   :classes-by-ident (into {}
                       (map (juxt :db/ident identity) classes))
   :attrs (into
           {}
           (for [[ns attrs] (group-by #(namespace (:db/ident %)) attributes)]
             [ns (sort-by :db/id attrs)]))
   :attrs-by-ident (into {}
                     (map (juxt :db/ident identity) attributes))})
