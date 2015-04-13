(ns web.rest.gene
  (:use cheshire.core
        web.rest.object)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap cond-let]]))

(def q-gene-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-gene-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype-not-observed ?ph]
    [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(def q-gene-var-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype ?ph]
            [?ph :variation.phenotype/phenotype ?pheno]])
 
(def q-gene-var-not-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype-not-observed ?ph]
     [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

(defn- author-lastname [author-holder]
  (or
   (->> (:affiliation/person author-holder)
        (first)
        (:person/last-name))
   (-> (:paper.author/author author-holder)
       (:author/id)
       (str/split #"\s+")
       (last))))

(defn- author-list [paper]
  (let [authors (->> (:paper/author paper)
                     (sort-by :ordered/index))]
    (cond
     (= (count authors) 1)
     (author-lastname (first authors))

     (< (count authors) 6)
     (let [names (map author-lastname authors)]
       (str (str/join ", " (butlast names)) " & " (last names)))

     :default
     (str (author-lastname (first authors)) " et al."))))


(defn- evidence-paper [paper]
  {:class "paper"
   :id (:paper/id paper)
   :taxonomy "all"
   :label (str (author-list paper) ", " (:paper/publication-date paper))})

(defn- phenotype-table [db id not?]
  (let [var-phenos (into {} (q (if not?
                                 q-gene-var-not-pheno
                                 q-gene-var-pheno)
                               db id))
        rnai-phenos (into {} (q (if not?
                                  q-gene-rnai-not-pheno
                                  q-gene-rnai-pheno)
                                db id))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->>
     (for [pid phenos :let [pheno (entity db pid)]]
       [(:phenotype/id pheno)
        {:object
         {:class "phenotype"
          :id (:phenotype/id pheno)
          :label (:phenotype.primary-name/text (:phenotype/primary-name pheno))
          :taxonomy "all"}
         :evidence
         (vmap
          "Allele:"
          (if-let [vp (seq (var-phenos pid))]
            (for [v vp
                  :let [holder (entity db v)
                        var ((if not?
                               :variation/_phenotype-not-observed
                               :variation/_phenotype)
                             holder)]]
              {:text
               {:class "variation"
                :id (:variation/id var)
                :label (:variation/public-name var)
                :style (if (= (:variation/seqstatus var)
                              :variation.seqstatus/sequence)
                         "font-weight: bold"
                         0)
                :taxonomy "c_elegans"}
               :evidence
               (vmap
                :Person_evidence
                (seq
                 (for [person (:phenotype-info/person-evidence holder)]
                   {:class "person"
                    :id (:person/id person)
                    :label (:person/standard-name person)
                    :taxonomy "all"}))

                :Anatomy_term
                (seq
                 (for [anatomy (:phenotype-info/anatomy-term holder)]
                   (pack-obj "anatomy-term" (:phenotype-info.anatomy-term/anatomy-term anatomy))))

                :Curator_confirmed
                (seq
                 (for [person (:phenotype-info/curator-confirmed holder)]
                   {:class "person"
                    :id (:person/id person)
                    :label (:person/standard-name person)
                    :taxonomy "all"}))

                :Paper_evidence
                (seq
                 (for [paper (:phenotype-info/paper-evidence holder)]
                   (evidence-paper paper)))

                :Remark
                (seq
                 (map :phenotype-info.remark/text
                      (:phenotype-info/remark holder))))}))
          
          "RNAi:"
          (if-let [rp (seq (rnai-phenos pid))]
            (for [r rp
                  :let [holder (entity db r)
                          rnai ((if not?
                                  :rnai/_phenotype-not-observed
                                  :rnai/_phenotype)
                                holder)]]
              {:text
               {:class "rnai"
                :id (:rnai/id rnai)
                :label (:rnai/id rnai)
                :style 0
                :taxonomy "c_elegans"}
               :evidence
               (vmap
                :Genotype (:rnai/genotype rnai)
                :Remark (seq (map :phenotype-info.remark/text
                                  (:phenotype-info/remark holder)))
                :Strain (:strain/id (:rnai/strain rnai))
                :Paper (if-let [paper (:rnai/reference rnai)]
                         (evidence-paper paper)))})))}])
     (into {}))))
      

(defn- gene-phenotype [db id]
  (let [gids (q '[:find ?g :in $ ?gid :where [?g :gene/id ?gid]] db id)]
    (if-let [[gid] (first gids)]
      {:name id
       :class "gene"
       :uri "whatevs"
       :fields
       {
        :name
        {:data 
         {:id id
          :label (:gene/public-name (entity db gid))
          :class "gene"
          :taxonomy "c_elegans"}
         :description (format "The name and WormBase internal ID of %s" id)}

        :phenotype
        {:data
         {:Phenotype              (phenotype-table db id false)
          :Phenotype_not_observed (phenotype-table db id true)}
         :description
         "The phenotype summary of the gene"}}})))
         
        
(defn gene-phenotype-rest [db id]
  {:status 200
   :content-type "application/json"
   :body (generate-string (gene-phenotype db id) {:pretty false})})

;; Needs better support for non-gene things.

(defn gene-mapping-twopt
  [db id]
  (->> (q '[:find [?tp ...]
            :in $ ?gene
            :where (or-join [?gene ?tp]
                     (and [?tpg1 :two-point-data.gene-1/gene ?gene]
                          [?tp :two-point-data/gene-1 ?tpg1])
                     (and [?tpg2 :two-point-data.gene-2/gene ?gene]
                          [?tp :two-point-data/gene-2 ?tpg2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [tp]
          {:mapper     (pack-obj "author" (first (:two-point-data/mapper tp)))
           :date       (:two-point-data/date tp)
           :raw_data   (:two-point-data/results tp)
           :genotype   (:two-point-data/genotype tp)
           :comment    (str/join "<br>" (map :two-point-data.remark/text (:two-point-data/remark tp)))
           :distance   (format "%s (%s-%s)" (or (:two-point-data/calc-distance tp) "0.0")
                                            (:two-point-data/calc-lower-conf tp)
                                            (:two-point-data/calc-upper-conf tp))
           :point_1    (let [p1 (:two-point-data/gene-1 tp)]
                         [(pack-obj "gene" (:two-point-data.gene-1/gene p1))
                          (pack-obj "variation" (:two-point-data.gene-1/variation p1))])
           :point_2    (let [p2 (:two-point-data/gene-2 tp)]
                         [(pack-obj "gene" (:two-point-data.gene-2/gene p2))
                          (pack-obj "variation" (:two-point-data.gene-2/variation p2))])}
          ))))

(defn gene-mapping-posneg
  [db id]
  (->> (q '[:find [?pn ...]
            :in $ ?gene
            :where (or-join [?gene ?pn]
                      (and [?png1 :pos-neg-data.gene-1/gene ?gene]
                           [?pn :pos-neg-data/gene-1 ?png1])
                      (and [?png2 :pos-neg-data.gene-2/gene ?gene]
                           [?pn :pos-neg-data/gene-2 ?png2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [pn]
          (let [items (->> [(pack-obj ((some-fn (comp :pos-neg-data.gene-1/gene :pos-neg-data/gene-1)
                                                (comp :pos-neg-data.locus-1/locus :pos-neg-data/locus-1)
                                                :pos-neg-data/allele-1
                                                :pos-neg-data/clone-1
                                                :pos-neg-data/rearrangement-1)
                                       pn))
                            (pack-obj ((some-fn (comp :pos-neg-data.gene-2/gene :pos-neg-data/gene-2)
                                                (comp :pos-neg-data.locus-2/locus :pos-neg-data/locus-2)
                                                :pos-neg-data/allele-2
                                                :pos-neg-data/clone-2
                                                :pos-neg-data/rearrangement-2)
                                       pn))]
                           (map (juxt :label identity))
                           (into {}))
                result (str/split (:pos-neg-data/results pn) #"\s+")]
          {:mapper    (pack-obj "author" (first (:pos-neg-data/mapper pn)))
           :date      (:pos-neg-data/date pn)
           :result    (map #(or (items (str/replace % #"\." ""))
                                (str " " % " "))
                           result)})
           
          ))))
          
(defn gene-mapping-multipt
  [db id]
  (->> (q '[:find [?mp ...]
            :in $ % ?gene
            :where (mc-obj ?mc ?gene)
            (or
             [?mp :multi-pt-data/a-non-b ?mc]
             [?mp :multi-pt-data/b-non-a ?mc]
             [?mp :multi-pt-data/combined ?mc])]
          db
          '[[(mc-obj ?mc ?gene) [?mcg :multi-counts.gene/gene ?gene]
                                [?mc :multi-counts/gene ?mcg]]
            [(mc-obj ?mc ?gene) (mc-obj ?mc2 ?gene)
                                [?mc :multi-counts/gene ?mc2]]]
          id)
       (map (partial entity db))
       (map
        (fn [mp]
          {:comment  (->> mp
                          :multi-pt-data/remark
                          first
                          :multi-pt-data.remark/text)
           :mapper   (pack-obj "author" (first (:multi-pt-data/mapper mp)))
           :date     (:multi-pt-data/date mp)
           :genotype (:multi-pt-data/genotype mp)
           :result   (let [res (loop [node (first (:multi-pt-data/combined mp))
                                      res  []]
                                 (cond
                                  (:multi-counts/gene node)
                                  (let [obj (:multi-counts/gene node)]
                                    (recur obj (conj res [(:multi-counts.gene/gene obj)
                                                          (:multi-counts.gene/int obj)])))
                                  
                                  :default res))
                           tot (->> (map second res)
                                    (filter identity)
                                    (reduce +))]
                       (->>
                        (mapcat
                         (fn [[obj count]]
                           [(pack-obj obj)
                            (if count
                              (str " (" count "/" tot ") "))])
                         res)
                        (filter identity)))}
          

          ))))
                        
    

(defn gene-mapping-data
  "Retrieve a map representing a mapping_data API response"
  [db id]
  (if-let [gid (q '[:find ?g .
                    :in $ ?gid
                    :where [?g :gene/id ?gid]]
                  db id)]
    {:name id
     :class "gene"
     :uri "whatevs"
     :fields
     {:name
      {:data (pack-obj "gene" (entity db gid))
       :description (format "The name and Wormbase internal ID of %s" id)}

      :two_pt_data
      {:data (seq (gene-mapping-twopt db gid))
       :description "Two point mapping data for this gene"}

      :pos_neg_data
      {:data (seq (gene-mapping-posneg db gid))
       :description "Positive/Negative mapping data for this gene"}

      :multi_pt_data
      {:data (seq (gene-mapping-multipt db gid))
       :description "Multi point mapping data for this gene"}}}))
 

(defn gene-mapping-data-rest [db id]
  {:status 200
   :content-type "application/json"
   :body (generate-string (gene-mapping-data db id) {:pretty true})})


(defn gene-human-diseases
  [db id]
  (if-let [gene (entity db [:gene/id id])]
    {:name id
     :class "gene"
     :uri "whatevs"
     :fields
     {:name
      {:data (pack-obj "gene" gene)
       :description (format "The name and Wormbase internal ID of %s" id)}

      :human_diseases
      {:data
       {:potential_model
        (seq
         (for [d (:gene/disease-potential-model gene)]
           (assoc (pack-obj (:gene.disease-potential-model/do-term d))
             :ev (get-evidence d))))

        :experimental_model
        (seq
         (for [d (:gene/disease-experimental-model gene)]
           (assoc (pack-obj (:gene.disease-experimental-model/do-term d))
             :ev (get-evidence d))))
        
        :gene
        (seq 
         (q '[:find [?o ...]
              :in $ ?gene
              :where [?gene :gene/database ?dbent]
                     [?omim :database/id "OMIM"]
                     [?field :database-field/id "gene"]
                     [?dbent :gene.database/database ?omim]
                     [?dbent :gene.database/field ?field]
                     [?dbent :gene.database/accession ?o]]
            db (:db/id gene)))

        :disease
        (seq 
         (q '[:find [?o ...]
              :in $ ?gene
              :where [?gene :gene/database ?dbent]
                     [?omim :database/id "OMIM"]
                     [?field :database-field/id "disease"]
                     [?dbent :gene.database/database ?omim]
                     [?dbent :gene.database/field ?field]
                     [?dbent :gene.database/accession ?o]]
            db (:db/id gene)))
        }}}}))
       

(defn gene-human-diseases-rest [db id]
  {:status 200
   :content-type "application/json"
   :body (generate-string (gene-human-diseases db id) {:pretty true})})

;;
;; Assembly-twiddling stuff (should be in own namespace?)
;;

(defn locatable-root-segment [loc]
  (loop [parent (:locatable/parent loc)
         min    (:locatable/min    loc)
         max    (:locatable/max    loc)]
    (if-let [ss (first (:sequence.subsequence/_sequence parent))]
      (recur (:sequence/_subsequence ss)
             (+ min (:sequence.subsequence/start ss) -1)
             (+ max (:sequence.subsequence/start ss) -1))
      {:sequence (:sequence/id parent)
       :seq-id   (:db/id parent)
       :min      min
       :max      max})))
      

;;
;; Reagents widget
;;

(defn- construct-labs [construct]
  (seq (map #(pack-obj "laboratory" (:construct.laboratory/laboratory %))
            (:construct/laboratory construct))))

(defn- transgene-labs [tg]
  (seq (map #(pack-obj "laboratory" (:transgene.laboratory/laboratory %))
            (:transgene/laboratory tg))))
  

(defn- transgene-record [construct]
  (let [base {:construct (pack-obj "construct" construct)
              :used_in   (pack-obj "transgene" (first (:construct/transgene-construct construct)))
              :use_summary (first (:construct/summary construct))}]
    (cond-let [use]
      (:construct/transgene-construct construct)
      (for [t use]
        (assoc base :used_in_type "Transgene construct"
                    :use_summary (:transgene/summary t)
                    :used_in     (pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct))))

      (:construct/transgene-coinjection construct)
      (for [t use]
        (assoc base :used_in_type "Transgene coinjection"
                    :use_summary (:transgene/summary t)
                    :used_in     (pack-obj "transgene" t)
                    :use_lab     (or (transgene-labs t)
                                     (construct-labs construct))))

      (:construct/engineered-variation construct)
      (for [v use]
        (assoc base :used_in_type "Engineered variation"
                    :used_in      (pack-obj "variation" v)
                    :use_lab      (construct-labs construct))))))

(defn- transgenes [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cons ...]
               :in $ ?gene
               :where [?cbg :construct.driven-by-gene/gene ?gene]
                      [?cons :construct/driven-by-gene ?cbg]]
             db (:db/id gene))
          (map (partial entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes expressed by this gene"}))
                      
(defn- transgene-products [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cons ...]
               :in $ ?gene
               :where [?cg :construct.gene/gene ?gene]
                      [?cons :construct/gene ?cg]]
             db (:db/id gene))
          (map (partial entity db))
          (mapcat transgene-record)
          (seq))
     :description "transgenes that express this gene"}))

(def ^:private probe-types
  {:oligo-set.type/affymetrix-microarray-probe "Affymetrix"
   :oligo-set.type/washu-gsc-microarray-probe  "GSC"
   :oligo-set.type/agilent-microarray-probe    "Agilent"})

(defn- microarray-probes [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?oligo ...]
               :in $ ?gene [?type ...]
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?ocds :oligo-set.overlaps-cds/cds ?cds]
                      [?oligo :oligo-set/overlaps-cds ?ocds]
                      [?oligo :oligo-set/type ?type]]
             db (:db/id gene) (keys probe-types))
          (map (fn [oid]
                 (let [oligo (entity db oid)]
                   (assoc (pack-obj "oligo-set" oligo)
                     :class "pcr_oligo"
                     :label (format
                             "%s [%s]"
                             (:oligo-set/id oligo)
                             (some probe-types (:oligo-set/type oligo)))))))
          (seq))
     :description "microarray probes"}))

(defn- matching-cdnas [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?cdna ...]
               :in $ ?gene
               :where [?gene :gene/corresponding-cds ?gcds]
                      [?gcds :gene.corresponding-cds/cds ?cds]
                      [?cds :cds/matching-cdna ?mcdna]
                      [?mcdna :cds.matching-cdna/sequence ?cdna]]
             db (:db/id gene))
          (map #(pack-obj "sequence" (entity db %)))
          (seq))
     :description "cDNAs matching this gene"}))
             
(defn- antibodies [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (q '[:find [?ab ...]
               :in $ ?gene
               :where [?gab :antibody.gene/gene ?gene]
                      [?ab :antibody/gene ?gab]]
             db (:db/id gene))
          (map
           (fn [abid]
             (let [ab (entity db abid)]
               {:antibody (pack-obj "antibody" ab)
                :summary (:antibody.summary/text (first (:antibody/summary ab)))
                :laboratory (map (partial pack-obj "laboratory") (:antibody/location ab))})))
          (seq))
     :description "antibodies generated against protein products or gene fusions"}))
                
(defn- orfeome-primers [gene]
  (let [db  (d/entity-db gene)
        seg (locatable-root-segment gene)]
    {:data
     ;;
     ;; Big assembly-navigation query should probably be factored out somewhere
     ;; once we're a bit more solid about how this stuff should work.
     ;;
     (->> (q '[:find [?p ...] 
               :in $ % ?seq ?min ?max 
               :where [?method :method/id "Orfeome"] 
                      (or-join [?seq ?min ?max ?method ?p]
                        (and         
                          [?seq :sequence/subsequence ?ss]
                          [?ss :sequence.subsequence/start ?ss-min]
                          [?ss :sequence.subsequence/end ?ss-max]
                          [(<= ?ss-min ?max)]
                          [(>= ?ss-max ?min)]
                          [?ss :sequence.subsequence/sequence ?ss-seq]
                          [(- ?min ?ss-min -1) ?rel-min]
                          [(- ?max ?ss-min -1) ?rel-max]
                          (child ?ss-seq ?rel-min ?rel-max ?method ?p))
                        (child ?seq ?min ?max ?method ?p))]
             db
             '[[(child ?parent ?min ?max ?method ?c) [?c :locatable/parent ?parent]
                                                     [?c :pcr-product/method ?method]
                                                     [?c :locatable/min ?cmin]
                                                     [?c :locatable/max ?cmax]
                                                     [(<= ?cmin ?max)]
                                                     [(>= ?cmax ?min)]]]
             (:seq-id seg) (:min seg) (:max seg))
          (map
           (fn [ppid]
             (let [pp (entity db ppid)]
               {:id    (:pcr-product/id pp)
                :class "pcr_oligo"
                :label (:pcr-product/id pp)})))
          (seq))
     :description "ORFeome Project primers and sequences"}))

(defn- primer-pairs [gene]
  (let [db  (d/entity-db gene)
        seg (locatable-root-segment gene)]
    {:data
     (->> (q '[:find [?p ...] 
               :in $ % ?seq ?min ?max 
               :where [?method :method/id "GenePairs"] 
                      (or-join [?seq ?min ?max ?method ?p]
                        (and         
                          [?seq :sequence/subsequence ?ss]
                          [?ss :sequence.subsequence/start ?ss-min]
                          [?ss :sequence.subsequence/end ?ss-max]
                          [(<= ?ss-min ?max)]
                          [(>= ?ss-max ?min)]
                          [?ss :sequence.subsequence/sequence ?ss-seq]
                          [(- ?min ?ss-min -1) ?rel-min]
                          [(- ?max ?ss-min -1) ?rel-max]
                          (child ?ss-seq ?rel-min ?rel-max ?method ?p))
                        (child ?seq ?min ?max ?method ?p))]
             db
             '[[(child ?parent ?min ?max ?method ?c) [?c :locatable/parent ?parent]
                                                     [?c :pcr-product/method ?method]
                                                     [?c :locatable/min ?cmin]
                                                     [?c :locatable/max ?cmax]
                                                     [(<= ?cmin ?max)]
                                                     [(>= ?cmax ?min)]]]
             (:seq-id seg) (:min seg) (:max seg))
          (map
           (fn [ppid]
             (let [pp (entity db ppid)]
               {:id    (:pcr-product/id pp)
                :class "pcr_oligo"
                :label (:pcr-product/id pp)})))
          (seq))
     :description "Primer pairs"}))

(defn- sage-tags [gene]
  {:data
   (seq (map #(pack-obj "sage-tag" (:sage-tag/_gene %)) (:sage-tag.gene/_gene gene)))

   :description
   "SAGE tags identified"})

(defn gene-reagents [db id]
  (if-let [gene (entity db [:gene/id id])]
    {:status 200
     :content-type "application/json"
     :body (generate-string
            {:class "gene"
             :name  id
             :fields
             {:name {:data (pack-obj "gene" gene)
                     :description (format "The name and WormBase internal ID of %s" id)}
              :transgenes         (transgenes gene)
              :transgene_products (transgene-products gene)
              :microarray_probes  (microarray-probes gene)
              :matching_cdnas     (matching-cdnas gene)
              :antibodies         (antibodies gene)
              :orfeome_primers    (orfeome-primers gene)
              :primer_pairs       (primer-pairs gene)
              :sage_tags          (sage-tags gene)}})}))   
