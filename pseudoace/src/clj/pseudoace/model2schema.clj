(ns pseudoace.model2schema
  (:use pseudoace.utils)
  (:require [datomic.api :as d :refer (tempid q touch entity entity-db)]
            [pseudoace.model :as model]
            [clojure.string :as str])
  (:import [pseudoace.model ModelNode]))

(defn- datomize-name [^String n]
  (let [dn (-> (.toLowerCase n)
               (str/replace #"[?#]" "")
               (str/replace #"_" "-"))]
    (if (= dn "id")   ; id is reserved for the object name.
      "xid"
      dn)))

(declare node->schema)
(declare attr->model)

(defn simple-tag? [node]
  (and (= (:type node) :tag)
       (empty? (:children node))))

(defn- flatten-children [node]
  (loop [children (:children node)
         path     []]
    (case (count children)
      0 path
      1 (recur (:children (first children)) (conj path (first children)))
      (except "Cannot flatten multiple chidren at " children))))


(def modeltype-to-datomic
  {:text     :db.type/string
   :int      :db.type/long
   :float    :db.type/double
   :date     :db.type/instant
   :ref      :db.type/ref})

(defn tuple-member-names [items attr]
  (let [basenames (map (comp datomize-name (some-fn :alt-name :name)) items)
        dups (->> (frequencies basenames)
                  (filter (fn [[k count]]
                               (> count 1)))
                  (map first)
                  (set))]
    (when (seq dups)
      (println "WARNING: using synthetic names for" attr basenames))
    (map-indexed
     (fn [index bn]
       (if (dups bn)
         (str bn "-" (char (+ (int \a) index)))
         bn))
     basenames)))
         

(defn tag->schema [mns tagpath node]
 (let [fchild (first (:children node))]
   (cond
   ;; "plain tag" case
   (empty? (:children node))
   (let [attribute  (keyword mns (datomize-name (last tagpath)))]
     [(vmap
       :db/id               (tempid :db.part/db)
       :db/ident            attribute
       :db/valueType        :db.type/boolean
       :db/cardinality      :db.cardinality/one
       :db.install/_attribute :db.part/db
       :pace/tags           (str/join " " tagpath))])
    
   ;; "enum" case
   (every? simple-tag? (:children node))
   (let      [attribute (keyword mns (datomize-name (last tagpath)))
              vns       (str mns "." (datomize-name (last tagpath)))]
     (conj
      (for [c (:children node)]
        (vmap
         :db/id         (tempid :db.part/user)
         :db/ident      (keyword vns (datomize-name (:name c)))
         :pace/tags     (:name c)))

      (vmap
       :db/id              (tempid :db.part/db)
       :db/ident           attribute
       :db/valueType       :db.type/ref
       :db/cardinality     (if (:unique? node)
                             :db.cardinality/one
                             :db.cardinality/many)
       :db.install/_attribute :db.part/db
       :pace/tags       (str/join " " tagpath))))
   
   
   (and (= (count (:children node)) 1)
        (#{:int :float :text :ref :date :hash} (:type fchild)))
   (if (and (empty? (:children fchild))
            (not= (:type fchild) :hash))
     ;; "simple datum" case
     (when (not (:suppress-xref fchild))
       (let [cname       (:name fchild)
             attribute   (keyword mns (datomize-name (last tagpath)))
             type        (modeltype-to-datomic  (:type fchild))
             obj-ref     (if (= type :db.type/ref)
                           (keyword (datomize-name cname) "id"))
             schema [(vmap
                      :db/id           (tempid :db.part/db)
                      :db/ident        attribute
                      :db/valueType    type
                      :db/cardinality  (if (:unique? node)
                                         :db.cardinality/one
                                         :db.cardinality/many)
                      :db.install/_attribute :db.part/db
                      :pace/tags       (str/join " " tagpath)
                      :pace/obj-ref    (if obj-ref
                                         {:db/id (tempid :db.part/db)
                                          :db/ident obj-ref})
                      :db/index      (if (= type :db.type/string)
                                         (.startsWith cname "?"))
                      :pace/fill-default (or (:fill-default fchild) nil))]]
         (if-let [x (:xref fchild)]
           (conj schema
                 {:db/id          (tempid :db.part/db)
                  :db/ident       obj-ref
                  :pace/xref      {:db/id                (tempid :db.part/user)
                                   :pace.xref/tags       x
                                   :pace.xref/attribute  {:db/id    (tempid :db.part/db)
                                                          :db/ident attribute}
                                   :pace.xref/obj-ref    {:db/id    (tempid :db.part/db)
                                                          :db/ident (keyword mns "id")}}})
           schema)))

     ;; "compound datum" case
     (let [fc         (->> (flatten-children node)
                           (take-while (complement :suppress-xref)))
           hashes     (filter #(= (:type %) :hash) fc)
           concretes  (filter #(not= (:type %) :hash) fc)
           attribute  (keyword mns (datomize-name (last tagpath)))
           cns        (str mns "." (datomize-name (last tagpath)))]

       (when (seq fc)
           (concat
            [(vmap
                    :db/id           (tempid :db.part/db)
                    :db/ident        attribute
                    :db/valueType    :db.type/ref
                    :db/cardinality  (if (and (:unique? node)
                                              (every? :unique? (butlast concretes)))
                                       :db.cardinality/one
                                       :db.cardinality/many)
                    :db/isComponent  true
                    :db.install/_attribute :db.part/db
                    :pace/use-ns     (for [h hashes]
                                       (datomize-name (:name h)))
                    :pace/tags       (str/join " " tagpath))]

            (mapcat
             (fn [[i c] mname]
               (let [type     (modeltype-to-datomic (:type c))
                     obj-ref  (if (= type :db.type/ref)
                                (keyword (datomize-name (:name c)) "id"))
                     cattr    (keyword cns mname)
                     schema [(vmap
                              :db/id           (tempid :db.part/db)
                              :db/ident        cattr
                              :db/valueType    type
                              :db/cardinality  :db.cardinality/one
                              :db.install/_attribute :db.part/db
                              :pace/tags       ""
                              :pace/order      i
                              :db/index      (if (= type :db.type/string)
                                                 (.startsWith (:name c) "?"))
                              :pace/fill-default (or (:fill-default c) nil)
                              :pace/obj-ref    (if obj-ref
                                                 {:db/id (tempid :db.part/db)
                                                  :db/ident obj-ref}))]]
                 (if-let [x (:xref c)]
                   (conj schema
                         {:db/id          (tempid :db.part/db)
                          :db/ident       obj-ref
                          :pace/xref      {:db/id                (tempid :db.part/user)
                                           :pace.xref/tags       x
                                           :pace.xref/attribute  {:db/id    (tempid :db.part/db)
                                                                  :db/ident cattr}
                                           :pace.xref/obj-ref    {:db/id    (tempid :db.part/db)
                                                                  :db/ident (keyword mns "id")}}})
                   schema)))
             (indexed concretes)
             (tuple-member-names concretes attribute))))))

   :default 
   (mapcat (partial node->schema mns tagpath) (:children node)))))

(defn node->schema [mns tagpath node]
  (if (= (:type node) :tag)
    (tag->schema mns (conj tagpath (:name node)) node)))


(defn model->schema [model]
  (let [name (.substring (:name model) 1)
        mns (datomize-name (:name model))]
    (conj
     (mapcat (partial node->schema mns []) (:children model))

     {:db/id          (tempid :db.part/db)
      :db/ident       (keyword mns "id")
      :db/valueType   :db.type/string
      :db/unique      :db.unique/identity
      :db/cardinality :db.cardinality/one
      :db.install/_attribute :db.part/db
      :pace/identifies-class name
      :pace/is-hash (.startsWith (:name model) "#")})))

(defn conj-in-tagpath [root tagpath nodes]
  (if (empty? tagpath)
    (if (seq nodes)
      (assoc
        (if (:unique? (meta nodes))
          (assoc root :unique? true)
          root)
        :children (if-let [c (:children root)]
                    (into c nodes)
                    (vec nodes)))
      root)   ; Special case to allow easy insertion of tags.
    (let [children (vec (:children root))
          index    (find-index #(= (:name %) (first tagpath)) children)]
      (assoc root :children
             (if (nil? index)
               (conj children (conj-in-tagpath (ModelNode. :tag (first tagpath) false false nil nil) (rest tagpath) nodes))
               (assoc children index
                      (conj-in-tagpath (nth children index) (rest tagpath) nodes)))))))

(defn pace-items-for-ns [db ns]
  (->> (q '[:find ?t
            :in $ ?ns
            :where [?t :db/ident ?ti]
            [(namespace ?ti) ?tins]
            [(= ?tins ?ns)]
            [?t :pace/tags _]]
          db ns)
       (map #(entity db (first %)))))

(defn attr->model* [ti]
  (case (:db/valueType ti)
    :db.type/long
    [(ModelNode. :int "Int" false false nil nil)]

    :db.type/float
    [(ModelNode. :float "Float" false false nil nil)]

    :db.type/double
    [(ModelNode. :float "Float" false false nil nil)]

    :db.type/instant
    [(ModelNode. :datetype "DateType" false false nil nil)]

    :db.type/string
    [(ModelNode. :text (if (:db/index ti)
                         "?Text"
                         "Text")
                 false false nil nil)]

    :db.type/boolean
    nil        ; All the nodes we need will be created
               ; by conj-in-tagpath

    :db.type/ref
    (cond
     (:db/isComponent ti)
      (let [db     (entity-db ti)
            concs  (pace-items-for-ns
                    db
                    (str (namespace (:db/ident ti)) "." (name (:db/ident ti))))
            hashes (for [ns (:pace/use-ns ti)]
                     (entity db (keyword ns "id")))]
        [(reduce
          (fn [next conc]
            (assoc (first (attr->model conc))   ;; Could this ever return multiples?
              :children (if next [next])))
                  
          (reduce
           (fn [next hash]
             (ModelNode. :hash (str "#" (:pace/identifies-class hash)) false false nil (if next [next])))
           nil
           hashes)
          concs)])
      
     (:pace/obj-ref ti)
      [(ModelNode. :ref
                   (str "?" (:pace/identifies-class (entity (entity-db ti) (:pace/obj-ref ti))))
                   false
                   false
                   nil
                   nil)]

     :default
     (if-let [enums (seq
                       (pace-items-for-ns
                          (entity-db ti)
                          (str (namespace (:db/ident ti)) "." (name (:db/ident ti)))))]
       (for [e enums]
         (ModelNode. :tag
                     (:pace/tags e)
                     false
                     false
                     nil
                     nil))
       
     
       (ModelNode. :ref
                   "?Funny"
                   false
                   false
                   nil
                   nil)))))

(defn attr->model [ti]
  (if-let [m (attr->model* ti)]
    (with-meta
      m
      {:unique? (= (:db/cardinality ti)
                   :db.cardinality/one)})))
    

(defn schema->model [db ident]
  (let [root  (entity db ident)
        props (pace-items-for-ns db (namespace (:db/ident root)))]
    (reduce
     (fn [model ti]
       (conj-in-tagpath model
                        (str/split (:pace/tags ti) #" ")
                        (attr->model ti)))
     (ModelNode. :ref
                 (str "?" (:pace/identifies-class root))   ; what about hashes?
                 false
                 false
                 nil
                 nil)
     props)))

(defn flatten-model* [prefix model]
  (let [np        (conj prefix model)
        children  (seq (:children model))]
    (if children
      (mapcat (partial flatten-model* np) children)
      [np])))

(defn flatten-model [model]
  (flatten-model* [] model))
  

(defn print-model [model]
  (loop [[line & lines] (flatten-model model)
         last-line      []
         old-tab-stops  [0]]
    (when line
      (recur lines
             line
             (loop [[node & nodes]     line
                    [llnode & llnodes] last-line
                    [tab & tabs]       old-tab-stops
                    cur                -2
                    our-tabs           []]
               (if node
                 (if (not= node llnode)
                   (let [ns    (if (:unique? node)
                                 (str (:name node) " UNIQUE")
                                 (:name node))
                         pos   (max (or tab 0) (+ cur 2))]
                     (print (apply str (repeat (- pos (max cur 0)) \space)))
                     (print ns)
                     (recur nodes
                            nil    ; Once we've hit a unique node, stop trying to dedup.
                            nil
                            (+ pos (count ns))
                            (conj our-tabs pos)))
                   (recur nodes llnodes tabs cur (conj our-tabs tab)))
                 (do
                   (println)
                   our-tabs)))))))  ; pass the final tab-stops on to the next line
