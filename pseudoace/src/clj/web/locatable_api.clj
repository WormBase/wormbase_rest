(ns web.locatable-api
  (:use pseudoace.utils
        web.rest.object)
  (:require pseudoace.binning
            [datomic.api :as d :refer (q entity)]
            [cheshire.core :as json]))

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

(def ^:private child-rule  '[[(child ?parent ?min ?max ?c ?cmin ?cmax) [(pseudoace.binning/reg2bins ?min ?max) [?bin ...]]
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
  
(defn seq-length [seq]
  (or
   (:sequence.dna/length (:sequence/dna seq))

   (count (:dna/sequence (:sequence.dna/dna (:sequence/dna seq))))

   (q '[:find (max ?ss-end) .
        :in $ ?seq
        :where [?seq :sequence/subsequence ?ss]
               [?ss  :sequence.subsequence/end ?ss-end]]
      (d/entity-db seq) (:db/id seq))))

(defn- feature-method [f]
  (if-let [method-key (first (filter #(= (name %) "method") (keys f)))]
    (method-key f)))

(defn- feature-strand [f]
  (case (:locatable/strand f)
    :locatable.strand/positive 1
    :locatable.strand/negative -1))

(defn- noncoding-transcript-structure [t tmin tmax]
  (->> (:transcript/source-exons t)
       (map (fn [{min :transcript.source-exons/min
                  max :transcript.source-exons/max}]
              {:type   "exon"
               :start  (+ tmin min -1)
               :end    (+ tmin max)
               :strand (feature-strand t)}))
       (sort-by :start)))

(defn- coding-transcript-structure [t tmin tmax c cmin cmax]
  (let [strand (feature-strand t)]
    (->>
     (:transcript/source-exons t)
     (mapcat
      (fn [{emin :transcript.source-exons/min
            emax :transcript.source-exons/max}]
        (let [emin (+ tmin emin -1)
              emax (+ tmin emax)
              coding-min (max emin cmin)
              coding-max (min emax cmax)]
          (those
           (if (< coding-min coding-max)
             {:type   "CDS"
              :start  coding-min
              :end    coding-max
              :strand strand})
           (if (< emin coding-min)
             {:type   (case strand
                        1  "five_prime_UTR"
                        -1 "three_prime_UTR")
              :start  emin
              :end    coding-min
              :strand strand})
           (if (> emax coding-max)
             {:type    (case strand
                         1   "three_prime_UTR"
                         -1  "five_prime_UTR")
              :start   (max emin coding-max)
              :end     emax
              :strand  strand})))))
     (sort-by :start))))

(defn- transcript-structure [t tmin tmax]
  (if-let [cds (:transcript.corresponding-cds/cds (:transcript/corresponding-cds t))]
    (let [[_ cds-min cds-max] (root-segment cds)]
      (coding-transcript-structure t tmin tmax cds cds-min cds-max))
    (noncoding-transcript-structure t tmin tmax)))

(defn get-features [db type parent min max]
  {:features 
   (->> 
    (features db type (:db/id parent) min max)
    (map
     (fn [[fid min max]]
       (let [feature (entity db fid)]
         (vmap
          :uniqueID (str fid)   ;; Check whether JBrowse *really* needs this...
          :name     (:label (pack-obj feature))
          :start    min
          :end      max
          :type     (:method/id (feature-method feature))
          :strand   (case (:locatable/strand feature)
                      :locatable.strand/positive 1
                      :locatable.strand/negative -1)
          :subfeatures (if (:transcript/id feature)
                         (transcript-structure feature min max))
          )))))})
        

(defn json-features [db {:keys [type id] :strs [start end]}]
  (if-let [parent (entity db [:sequence/id id])]
    (let [start            (parse-int start)
          end              (parse-int end)
          [parent min max] (root-segment parent 
                                         (or start 1) 
                                         (or end (seq-length parent)))]
      {:status 200
       :content-type "text/plain"
       :headers {"access-control-allow-origin" "*"}    ;; Should be set elsewhere.
       :body (json/generate-string
              (get-features db type parent min max)
              {:pretty true})})))
        
    
    
