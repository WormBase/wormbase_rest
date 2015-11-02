(ns wb.curation-schema
  (:use datomic-schema.schema)
  (:require [datomic.api :as d]))

(def curation-schema
  (concat 
   (generate-schema [
     
    (schema id-generator
     (fields
      [identifier :ref :unique-identity]
      [template   :string]
      [last-id    :long :nohistory]))

    (schema wormbase
     (fields
      [system-name    :string]))        

    (schema wormbase.title
     (fields
      [user       :ref :indexed]
      [class      :ref :indexed]
      [attribute  :ref]))
    ])


   [

    ;;
    ;; Entity which can be used as the subject of system-wide predicates
    ;; ("global variables").
    ;;
    
    {:db/id          #db/id[:db.part/user]
     :db/ident       :wormbase/system}

    ;; Timestamp
    {:db/id          #db/id[:db.part/tx]
     :db/txInstant   #inst "1970-01-01T00:00:01"}
   ]))


(def curation-init
  [
    ;; Bootstrap ID generators for the curation interface

    {:db/id                     #db/id[:db.part/user]
     :id-generator/identifier   :gene/id
     :id-generator/template     "WBGene%08d"
     :id-generator/last-id      301000}

    {:db/id                     #db/id[:db.part/user]
     :id-generator/identifier   :variation/id
     :id-generator/template     "WBVar%08d"
     :id-generator/last-id      2200000}

    {:db/id                     #db/id[:db.part/user]
     :id-generator/identifier   :feature/id
     :id-generator/template     "WBsf%08d"
     :id-generator/last-id      1000000}

    ;; Timestamp
    {:db/id          #db/id[:db.part/tx]
     :db/txInstant   #inst "1970-01-01T00:00:01"}
   ])

(def curation-fns
  [{:db/id       #db/id[:db.part/user]
    :db/ident    :wb/mint-identifier
    :db/doc      "Mint a new value of identity-attribute `attr` for each of
                  the entities in `tids`, which should be a sequence of (presumably
                  temporary) entity IDs.  An id-generator must be installed for
                  `attr`."
    :db/fn (d/function
            {:lang :clojure
             :params '[db attr tids]
             :code '(let [[gen last-id template]
                          (q '[:find [?gen ?last ?template]
                               :in $ ?attr-id 
                               :where [?attr :db/ident ?attr-id]
                                      [?gen  :id-generator/identifier ?attr]
                                      [?gen  :id-generator/template   ?template]
                                      [?gen  :id-generator/last-id    ?last]]
                             db attr)]
                      (when-not gen
                        (throw (Exception. (str "No id-generator for " attr))))
                      (conj
                       (map-indexed
                        (fn [idx tid]
                          [:db/add tid attr (format template (+ last-id idx 1))])
                        tids)
                       [:db.fn/cas gen :id-generator/last-id last-id (+ last-id (count tids))]))})}

    ;; Timestamp
    {:db/id          #db/id[:db.part/tx]
     :db/txInstant   #inst "1970-01-01T00:00:01"}

   ])
                           
                       
            
