(ns wb.locatables
  (:use pseudoace.utils)
  (:require pseudoace.binning
            [datomic.api :as d :refer (q entity)]))

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
  (if-let [ss (first (:sequence.subsequence/_sequence parent))]
    (recur (:sequence/_subsequence ss)
           (+ start (:sequence.subsequence/start ss) -1)
           (+ end (:sequence.subsequence/start ss) -1))
    [parent start end])))

(def ^:private child-rule  
  '[[(child ?parent ?min ?max ?c ?cmin ?cmax) [(pseudoace.binning/reg2bins ?min ?max) [?bin ...]]
                                              [(pseudoace.binning/xbin ?parent ?bin) ?xbin]
                                              [?c :locatable/xbin ?xbin]
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
                           [?seq :sequence/subsequence ?ss]
                           [?ss :sequence.subsequence/start ?ss-min]
                           [?ss :sequence.subsequence/end ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [?ss :sequence.subsequence/sequence ?ss-seq]
                           [(- ?min ?ss-min -1) ?rel-min]
                           [(- ?max ?ss-min -1) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min -1) ?fmin]
                           [(+ ?rel-fmax ?ss-min -1) ?fmax])
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
                           [?seq :sequence/subsequence ?ss]
                           [?ss :sequence.subsequence/start ?ss-min]
                           [?ss :sequence.subsequence/end ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [?ss :sequence.subsequence/sequence ?ss-seq]
                           [(- ?min ?ss-min -1) ?rel-min]
                           [(- ?max ?ss-min -1) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min -1) ?fmin]
                           [(+ ?rel-fmax ?ss-min -1) ?fmax])
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
                           [?seq :sequence/subsequence ?ss]
                           [?ss :sequence.subsequence/start ?ss-min]
                           [?ss :sequence.subsequence/end ?ss-max]
                           [(<= ?ss-min ?max)]
                           [(>= ?ss-max ?min)]
                           [?ss :sequence.subsequence/sequence ?ss-seq]
                           [(- ?min ?ss-min -1) ?rel-min]
                           [(- ?max ?ss-min -1) ?rel-max]
                           (child ?ss-seq ?rel-min ?rel-max ?f ?rel-fmin ?rel-fmax)
                           [(+ ?rel-fmin ?ss-min -1) ?fmin]
                           [(+ ?rel-fmax ?ss-min -1) ?fmax])
                          (child ?seq ?min ?max ?f ?fmin ?fmax))
              (or
               [?f :locatable/method ?method]
               [?f :feature/method ?method])]
     db
     child-rule
     pid min max type))
