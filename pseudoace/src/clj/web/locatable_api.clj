(ns web.locatable-api
  (:use pseudoace.utils
        web.rest.object
        wb.locatables
        wb.sequence)
  (:require pseudoace.binning
            [datomic.api :as d :refer (q entity)]
            [cheshire.core :as json]
            [compojure.core :refer (routes GET)]))

(defn- feature-method [f]
  (if-let [method-key (first (filter #(= (name %) "method") (keys f)))]
    (method-key f)))

(defn- feature-strand [f]
  (case (:locatable/strand f)
    :locatable.strand/positive 1
    :locatable.strand/negative -1))

(defn- noncoding-transcript-structure [t tmin tmax]
  (->> (:transcript/source-exons t)
       (map
        (fn [{emin :transcript.source-exons/min
              emax :transcript.source-exons/max}]
          (let [[emin emax] (case (:locatable/strand t)
                              :locatable.strand/positive
                              [(+ tmin emin -1)
                               (+ tmin emax)]
                              
                              :locatable.strand/negative
                              [(- tmax emax)
                               (- tmax emin -1)])]
                {:type   "exon"
                 :start  emin
                 :end    emax
                 :strand (feature-strand t)})))
       (sort-by :start)))

(defn- coding-transcript-structure [t tmin tmax c cmin cmax]
  (let [strand (feature-strand t)]
    (->>
     (:transcript/source-exons t)
     (mapcat
      (fn [{emin :transcript.source-exons/min
            emax :transcript.source-exons/max}]
        (let [[emin emax] (case (:locatable/strand t)
                            :locatable.strand/positive
                            [(+ tmin emin -1)
                             (+ tmin emax)]

                            :locatable.strand/negative
                            [(- tmax emax)
                             (- tmax emin -1)])
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
         ))))))
        

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
              {:features (get-features db type parent min max)}
              {:pretty true})})))

(defn json-stats-global [db {:keys [type]}]
  {:status 200
   :content-type "text/plain"
   :headers {"access-control-allow-origin" "*"}    ;; Should be set elsewhere.
   :body (json/generate-string
          {}
          {:pretty true})})

(defn json-stats-region [db {:keys [type id] :strs [start end]}]
  (if-let [parent (entity db [:sequence/id id])]
    (let [start            (parse-int start)
          end              (parse-int end)
          [parent min max] (root-segment parent 
                                         (or start 1) 
                                         (or end (seq-length parent)))
          features         (get-features db type parent min max)]
      {:status 200
       :content-type "text/plain"
       :headers {"access-control-allow-origin" "*"}    ;; Should be set elsewhere.
       :body (json/generate-string
              {:featureDensity (float (/ (count features) (- max min)))
               :featureCount   (count features)}
              {:pretty true})})))

(defn json-densities [db {:keys [type id]
                          :strs [start end]
                          bin-size "basesPerBin"}]
  (if-let [parent (entity db [:sequence/id id])]
    (let [reg-start        (parse-int start)
          reg-end          (parse-int end)
          [parent rmin rmax] (root-segment parent 
                                         (or reg-start 1) 
                                         (or reg-end (seq-length parent)))
          bin-size         (or (parse-int bin-size)
                               (max 10 (quot (- rmax rmin) 50)))
          bin-count        (int (Math/ceil (/ (- rmax rmin) bin-size)))
          features         (get-features db type parent rmin rmax)
          counts           (reduce
                            (fn [cnts {:keys [start end]}]
                              (reduce
                               (fn [cnts bin]
                                 (update cnts bin inc))
                               cnts
                               (range (max 0 (quot (- start rmin) bin-size))
                                      (min bin-count (quot (- end rmin) bin-size)))))
                            (vec (repeat bin-count 0))
                            features)]
      {:status 200
       :content-type "text/plain"
       :headers {"access-control-allow-origin" "*"}    ;; Should be set elsewhere.
       :body (json/generate-string
              {:bins counts
               :stats {:max (reduce max counts)
                       :basesPerBin bin-size}}
              {:pretty true})})))
          
(defn feature-api [db]
  (routes
   (GET "/:type/features/:id" {params :params}
        (json-features db params))
   (GET "/:type/stats/global" {params :params}
        (json-stats-global db params))
   (GET "/:type/stats/region/:id" {params :params}
        (json-stats-region db params))
   (GET "/:type/stats/regionFeatureDensities/:id" {params :params}
        (json-densities db params))))
