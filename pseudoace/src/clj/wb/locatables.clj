(ns wb.locatables
  (:use pseudoace.utils
        wb.binning)
  (:require [datomic.api :as d :refer (q entity)]))

;;
;; Don't pay too much attention to the details here.  Binning scheme and method
;; handling are likely to change in wb248-imp2
;;

(defn root-segment
  ([entity]
     (root-segment (:locatable/parent entity)
                   (:locatable/min entity)
                   (:locatable/max entity)))
  ([parent start end]
  (if-let [ss (:locatable/assembly-parent parent)]
    (recur ss
           (+ start (:locatable/min parent))
           (+ end   (:locatable/min parent)))
    [parent start end])))

(def ^:private child-rule  
  '[[(child ?parent ?min ?max ?c ?cmin ?cmax) [?parent :sequence/id ?seq-name]
                                              [(wb.binning/bins ?seq-name ?min ?max) [?bin ...]]
                                              [?c :locatable/murmur-bin ?bin]
                                              [?c :locatable/parent ?parent]
                                              [?c :locatable/min ?cmin]
                                              [?c :locatable/max ?cmax]
                                              [(<= ?cmin ?max)]
                                              [(>= ?cmax ?min)]]])


(defmulti features (fn [_ type _ _ _] type))

(defmethod features "transcript"
  [db type pid min max]
  (q '[:find ?f ?fmin ?fmax
       :in $ % ?seq ?min ?max 
       :where (or-join [?seq ?min ?max ?f ?fmin ?fmax]
                          (and
                           [?ss-seq :locatable/assembly-parent ?seq]
                           [?ss-seq :locatable/min ?ss-min]
                           [?ss-seq :locatable/max ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [(- ?min ?ss-min) ?rel-min]
                           [(- ?max ?ss-min) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min) ?fmin]
                           [(+ ?rel-fmax ?ss-min) ?fmax])
                          (child ?seq ?min ?max ?f ?fmin ?fmax))
              [?f :transcript/id _]]
     db
     child-rule
     pid min max))

(defmethod features "variation"
  [db type pid min max]
  (q '[:find ?f ?fmin ?fmax
       :in $ % ?seq ?min ?max 
       :where (or-join [?seq ?min ?max ?f ?fmin ?fmax]
                          (and
                           [?ss-seq :locatable/assembly-parent ?seq]
                           [?ss-seq :locatable/min ?ss-min]
                           [?ss-seq :locatable/max ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [(- ?min ?ss-min) ?rel-min]
                           [(- ?max ?ss-min) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min) ?fmin]
                           [(+ ?rel-fmax ?ss-min) ?fmax])
                          (child ?seq ?min ?max ?f ?fmin ?fmax))
              [?f :variation/id _]]
     db
     child-rule
     pid min max))

(defmethod features :default
  [db type pid min max]
  (q '[:find ?f ?fmin ?fmax
       :in $ % ?seq ?min ?max ?meth-name
       :where [?method :method/id ?meth-name]
              (or-join [?seq ?min ?max ?f ?fmin ?fmax]
                          (and
                           [?ss-seq :locatable/assembly-parent ?seq]
                           [?ss-seq :locatable/min ?ss-min]
                           [?ss-seq :locatable/max ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [(- ?min ?ss-min) ?rel-min]
                           [(- ?max ?ss-min) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min) ?fmin]
                           [(+ ?rel-fmax ?ss-min) ?fmax])
                          (child ?seq ?min ?max ?f ?fmin ?fmax))
               [?f :locatable/method ?method]]
     db
     child-rule
     pid min max type))
