(ns web.locatable-api
  (:use pseudoace.utils
        web.rest.object)
  (:require pseudoace.binning
            [datomic.api :as d :refer (q entity)]
            [cheshire.core :as json]))

(defn root-segment [parent start end]
  (if-let [ss (first (:sequence.subsequence/_sequence parent))]
    (recur (:sequence/_subsequence ss)
           (+ start (:sequence.subsequence/start ss) -1)
           (+ end (:sequence.subsequence/start ss) -1))
    [parent start end]))

(def ^:private child-rule  '[[(child ?parent ?min ?max ?c ?cmin ?cmax) [(pseudoace.binning/reg2bins ?min ?max) [?bin ...]]
                                                                       [(pseudoace.binning/xbin ?parent ?bin) ?xbin]
                                                                       [?c :locatable/xbin ?xbin]
                                                                       [?c :locatable/parent ?parent]
                                                                       [?c :locatable/min ?cmin]
                                                                       [?c :locatable/max ?cmax]
                                                                       [(<= ?cmin ?max)]
                                                                       [(>= ?cmax ?min)]]])



(defn features [db pid min max]
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
                          (child ?seq ?min ?max ?f ?fmin ?fmax))]
     db
     child-rule
     pid min max))
  
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

(defn- transcript-structure [t tmin tmax]
  (->> (:transcript/source-exons t)
       (map (fn [{min :transcript.source-exons/min
                  max :transcript.source-exons/max}]
              {:type   "exon"
               :start  (+ tmin min -1)
               :end    (+ tmin max)
               :strand (case (:locatable/strand t)
                         :locatable.strand/positive 1
                         :locatable.strand/negative -1)}))
       (sort-by :start)))
  

(defn get-features [db parent min max]
  {:features 
   (->> 
    (features db (:db/id parent) min max)
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
        

(defn json-features [db {:keys [id] :strs [start end type]}]
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
              (get-features db parent min max)
              {:pretty true})})))
        
    
    
