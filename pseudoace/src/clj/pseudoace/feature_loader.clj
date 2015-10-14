(ns pseudoace.feature-loader
  (use pseudoace.utils
       clojure.instant
       clojure.java.io)
  (require [datomic.api :as d :refer (db q entity touch tempid)]
           [acetyl.parser :as ace]
           [clojure.string :as str]
           [clojure.edn :as edn])
  (import java.io.FileInputStream
          java.io.PushbackReader
          java.util.zip.GZIPInputStream))

(defrecord FeatureLink [sequence start end])

(defmulti import-features :class)

(defmethod import-features "Feature_data"
  [obj {:keys [sequence start end]}]
  (mapcat
   (fn [line]
     (case (first line)
       "Feature"
       (let [[_ method fstart fend fscore note] line]
         [(vmap
           :db/id          (tempid :db.part/user)
           :fdata/sequence [:sequence/id sequence]
           :fdata/method   [:method/id method]
           :fdata/start    (dec (+ start (parse-int fstart)))
           :fdata/end      (dec (+ start (parse-int fend)))
           :fdata/score    (parse-double fscore)
           :fdata/note     note)])
       "Splices"
       (cond
        (= (second line) "Confirmed_intron")
        (let [[_ _ fstart fend conf-type & conf-data] line
              fdat   {:db/id          (tempid :db.part/user)
                      :fdata/sequence [:sequence/id sequence]
                      :fdata/start    (dec (+ start (parse-int fstart)))
                      :fdata/end      (dec (+ start (parse-int fend)))}]
          [(case conf-type
             "cDNA"
             (assoc fdat :splice-confirm/cdna [:sequence/id (first conf-data)])

             "EST"
             (assoc fdat :splice-confirm/est [:sequence/id (first conf-data)])

             "OST"
             (assoc fdat :splice-confirm/ost [:sequence/id (first conf-data)])

             "RST"
             (assoc fdat :splice-confirm/rst [:sequence/id (first conf-data)])

             "RNASeq"
             (assoc fdat :splice-confirm/rnaseq
                         {:splice-confirm.rnaseq/analysis [:analysis/id (first conf-data)]
                          :splice-confirm.rnaseq/count (parse-int (second conf-data))})

             "Mass_spec"
             (assoc fdat :splice-confirm/mass-spec [:mass-spec-peptide/id (first conf-data)])

             "mRNA"
             (assoc fdat :splice-confirm/mrna [:sequence/id (first conf-data)])

             "Homology"
             (assoc fdat :splice-confirm/homology (first conf-data))

             "UTR"
             (assoc fdat :splice-confirm/utr [:sequence/id (first conf-data)])

             "False"
             (assoc fdat :splice-confirm/false-splice [:sequence/id (first conf-data)])

             "Inconsistent"
             (assoc fdat :splice-confirm/inconsistent [:sequence/id (first conf-data)])

             ;;default
             (throw (Exception. (str "Bad splice confirmation" conf-type))))])

        :default
        (let [[_ site method fstart fend fscore] line]
          [(vmap
            :db/id          (tempid :db.part/user)
            :fdata/sequence [:sequence/id sequence]
            :fdata/method   [:method/id method]
            :splice/site    (case site
                              "Predicted_5" :splice.site/five-prime
                              "Predicted_3" :splice.site/three-prime)
            :fdata/start    (dec (+ start (parse-int fstart)))
            :fdata/end      (dec (+ start (parse-int fend)))
            :fdata/score    (parse-double fscore))]))
                    
       ;;default
       []))
   (:lines obj)))

(defmethod import-features "Homol_data"
  [obj {:keys [sequence start end]}]
  (mapcat
   (fn [line]
     (case (first line)
       "Homol"
       (let [[_ type target method score fstart fend hstart hend & homol-info] line
             ent (vmap
                  :db/id           (tempid :db.part/user)
                  :fdata/sequence  [:sequence/id sequence]
                  :fdata/method    [:method/id method]
                  :fdata/start     (if fstart
                                     (dec (+ start (parse-int fstart))))
                  :fdata/end       (if fend
                                     (dec (+ start (parse-int fend))))
                  :fdata/score     (parse-double score)
                  :homol/start     (parse-int hstart)
                  :homol/end       (parse-int hend)
                  :homol/id        (if (= (first homol-info) "Align_id")
                                     (second homol-info)))]
         [(case type
            "DNA_homol"
            (assoc ent :homol/dna [:sequence/id target])

            "Pep_homol"
            (assoc ent :homol/peptide [:protein/id target])

            "Motif_homol"
            (assoc ent :homol/motif [:motif/id target])

            "Homol_homol"  ;; WTF?
            ent

            "RNAi_homol"
            (assoc ent :homol/rnai [:rnai/id target])

            "Oligo_set_homol"
            (assoc ent :homol/oligo-set [:oligo-set/id target])

            "Structure_homol"
            (assoc ent :homol/structure [:structure/id target])

            "Expr_homol"
            (assoc ent :homol/expr-pattern [:expr-pattern/id target])

            "MSPeptide_homol"
            (assoc ent :homol/ms-peptide [:mass-spec-peptide/id target])

            "SAGE_homol"
            (assoc ent :homol/sage [:sage-tag/id target]))])
        

       ;;default
       []))
   (:lines obj)))

(defmethod import-features :default
  [obj link]
  []) 

(defn load-feature-map [f]
  (with-open [r (PushbackReader. (reader f))]
    (->> (edn/read r)
         (map (fn [[seq fid start end]]
                [fid (FeatureLink. seq start end)]))
         (into {}))))

(defn import-acefile-features
  [f feature-map]
  (doall
   (->> (ace/ace-reader f)
        (ace/ace-seq)
        (mapcat (fn [f]
                  (if-let [fm (feature-map (:id f))]
                    (import-features f fm)
                    (println "Coudln't find" (:id f))))))))
  
