(ns wb.liberal-txns
  (:require [datomic.api :as d :refer (q tempid entity)]
            [pseudoace.utils :refer (vassoc)]))

;;
;; The "liberal" transaction format is a superset of the standard Datomic transaction format,
;; with some Wormbase-specific extensions.
;;
;;   1. :db/id is option on map-formatted transaction data.  If it is omitted, a tempid
;;      will be generated.  The resolved will attempt to identify the correct partition
;;      based on any :<class>/id attribute included.
;;
;;   2. :<class>/id attributes map optionally have a special value of :allocate.  In this
;;      case the system will attempt to mint a new identifier (so long as an ID generator
;;      is installed for this class.
;;


(defn- get-clids
  "Find class-identify attributes in `db`."
  [db]
  (->> (q '[:find [?c ...]
            :where [?c :pace/identifies-class _]]
          db)
       (map (partial entity db))
       (map (juxt :db/ident identity))
       (into {})))

(defn resolve-liberal-tx [db tx]
  "Take a sequence of `tx` data in liberal-tx format, and return a sequence of resolved
   (transactable) data."
  (let [clids
            (get-clids db)
        
        resolve-txitem
            (fn [{:keys [tx ids]} item]
              (if (map? item)
                (let [clide (some clids (keys item))
                      clid  (:db/ident clide)
                      clidv (item clid)
                      idv   (or (:db/id item)
                                (tempid (or (:pace/prefer-part clide)
                                            :db.part/user)))]
                  {:tx (conj tx (cond-> item
                                  (not= idv (:db/id item))
                                  (assoc :db/id idv)

                                  (= clidv :allocate)
                                  (dissoc clid)))
                   :ids (cond-> ids
                          (= clidv :allocate)
                          (update clid conj idv))})
                
                ;; vector case
                (let [[cmd e a v] item]
                  (if (and (= cmd :db/add)
                           (clids a)
                           (= v :allocate))
                    {:tx  tx
                     :ids (update ids (:db/ident (clids a)) conj e)}
                    {:tx (conj tx item)
                     :ids ids}))))

        {:keys [tx ids]}
            (reduce resolve-txitem {} tx)]
    (concat
     tx
     (for [[attr tids] ids]
       [:wb/mint-identifier attr tids]))))
