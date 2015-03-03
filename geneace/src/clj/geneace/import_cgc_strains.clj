(ns geneace.import-cgc-strains
  (:use clojure.java.io
        geneace.utils)
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [datomic.api :as d])
  (:import org.apache.poi.xwpf.extractor.XWPFWordExtractor
           org.apache.poi.xwpf.usermodel.XWPFDocument))

(defn docx-text [doc]
  (->> (input-stream doc)
       (XWPFDocument.)
       (XWPFWordExtractor.)
       (.getText)))


(def strain-parser
  (insta/parser
   "<strain-file> = header strains
    header = <#' +'> text <newline> <#' *=+'> <newline>
    
    strains = strain*
    strain = field* <divider>

    field = <#' {0,12}'> name <':'> (<' '> value | <newline>)
    name = #'[^ :][^:]*'
    value = text <newline> (<'               '> #'-{0,19}[^-].*' <newline>)* 

    divider = '               --------------------\\n'

    <text> = #'[^\\n]+'
    <newline> = '\\n'"))

(defn strain-parse-xfrm [text]
  (->> 
   (insta/parse strain-parser text :optimize :memory)
   (insta/transform
    {:strain #(into {} %&)
     :field (fn 
             ([name] [name nil])
             ([name value] [name value]))
     :name #(-> (str/replace % #" " "-")
                (str/lower-case)
                (keyword))
     :value #(str/join " " %&)})))

(defn norm-space
  "Normalize any multiple spaces in `s`"
  [s]
  (if s
    (str/replace s #"\s{2,}" " ")))

(def geneace-con (d/connect "datomic:free://localhost:4334/ga1"))

(defn find-person [name]
  (d/q '[:find [?person-id ...]
         :in $ ?name
         :where (or [?p :person/standard-name ?name]
                    [?p :person/full-name ?name]
                    [?p :person/also-known-as ?name])
                [?p :person/id ?person-id]]
       (d/db geneace-con)
       name))

(defn get-variation-id [public-name]
  (d/q '[:find ?var-id .
         :in $ ?name
         :where [?v :variation/public-name ?name]
                [?v :variation/id ?var-id]]
       (d/db geneace-con)
       public-name))

(defn get-gene-id [public-name]
  (d/q '[:find ?gene-id .
         :in $ ?name
         :where (or-join [?g ?name]
                  [?g :gene/public-name ?name]
                  [?g :gene/sequence-name ?name]
                  (and [?cgc :gene.cgc-name/text ?name]
                       [?g :gene/cgc-name ?cgc]))
                [?g :gene/id ?gene-id]]
       (d/db geneace-con)
       public-name))

(defn get-transgene-id [public-name]
  (d/q '[:find ?tg-id .
         :in $ ?name
         :where [?tg :transgene/public-name ?name]
                [?tg :transgene/id ?tg-id]]
       (d/db geneace-con)
       public-name))

(defn check-details [gene allele strain species strain-tid]
  (if-let [vid (get-variation-id allele)]
   [(vmap
     :db/id [:variation/id vid]
     :variation/strain {
       :variation.strain/strain strain-tid
     }
     :variation/gene 
       (if gene
         (if-let [gid (get-gene-id gene)]
           {:variation.gene/gene [:gene/id gid]})))]))
    
     

(defn process-genotype [genotype strain species strain-tid]
  (let [re-to-fn 
        (array-map 
         ;; find simple locus allele combinations e.g. spt-3(hc184)
         #"^([Ca-z\-]{3,6}\-\d+\.{0,1}\d*)\(([a-z]{1,2}\d+)\)"
         (fn [[_ gene allele]]
           (check-details gene allele strain species strain-tid))

         ;; find transposon insertions
         #"([a-z]+(Si|Ti)\d+)"
         (fn [[_ allele]]
           (check-details nil allele strain species strain-tid))

         ;; find chromosomal aberrations e.g. szT1
         #"\s*([a-z]{1,3}(Dp|Df|In|T|C)\d+)"
         (fn [[_ rearrangement]]
           [{:db/id [:rearrangement/id rearrangement]
             :rearrangement/strain strain-tid}])

         ;; find transgenes e.g. zhEx11
         #"\s*([a-z]{1,3}(Ex|Is)\d+)"
         (fn [[_ transgene]]
           (if-let [tgid (get-transgene-id transgene)]
             ;; this doesn't seem to work because transgenes don't have public-names in geneace?
             [{:db/id [:transgene/id tgid]
               :transgene/strain strain-tid}]))

         ;; find double barrelled alleles (revertants) e.g. daf-12(rh61rh412) 
         #"([Ca-z\-]{3,6}\-\d+\.{0,1}\d*)\(([a-z]{1,2}\d+)([a-z]{1,2}\d+)\)"
         (fn [[_ gene allele-1 allele-2]]
           (concat
            (check-details gene allele-1 strain species strain-tid)
            (check-details gene allele-2 strain species strain-tid)))

         ;;alleles affecting 2 genes like arf-1.1&F45E4.7(ok1840)
         #"([\w\.\-]+)&([\w\.\-]+)\(([a-z]{1,2}\d+)\)"
         (fn [[_ gene-1 gene-2 allele]]
           (concat
            (check-details gene-1 allele strain species strain-tid)
            (check-details gene-2 allele strain species strain-tid)))

         ;; find alleles attached to non-approved, or unusual gene names e.g. let-?(h661)
         #"([\w\.\-\?]+)\(([a-z]{1,2}\d+)\)"
         (fn [[_ gene allele]]
           (check-details gene allele strain species))

         ;; find any skulking gene names missed by steps above, these are often where there is no allele name
         ;;or the allele name is wild-type, e.g. unc-24(+)
         #"([a-z]{3,4}\-\d+)/"
         (fn [[_ gene]]
           (if-let [gid (get-gene-id gene)]
             {:db/id [:gene/id gid]
              :gene/strain strain-tid})))]

    ;; Loop through re-to-fn in order, removing any portions of `genotype`
    ;; which match the regexp and collecting any datoms returned by the 
    ;; associated function.
    (loop [genotype                         genotype
           datoms                           []
           [[re fn] & rest-fns :as fn-list] (seq re-to-fn)]
      (if-let [match (re-find re genotype)]
        (recur
          (str/replace genotype re "")
          (concat datoms (fn match))
          fn-list)
        (if (seq rest-fns)
          (recur genotype datoms rest-fns)
          [genotype datoms])))))
          
           

(defn import-strain [{:keys [strain species genotype made-by
                             description mutagen outcrossed] 
                      :as data}]
  (if strain
    (let [made-by-person (find-person made-by)
          strain-tid (d/tempid :db.part/user)]
      (vmap
       :db/id 
         (d/tempid :db.part/user)
       :strain/id 
         strain
       :strain/species 
         (if species [:species/id species])
       :strain/genotype 
         (norm-space genotype)
       :strain/mutagen
         (norm-space mutagen)
       :strain/outcrossed
         (norm-space outcrossed)
       :strain/made-by
         (if (= (count made-by-person) 1)
           [:person/id (first made-by-person)])
        
       :strain/remark
         (those
          (if (not= (count made-by-person) 1)
            {:strain.remark/text (str "Made_by: " made-by)
             :evidence/cgc-data-submission true})
          (if-let [description (norm-space description)]
           {:strain.remark/text
              (str/replace description #"\"" "")   ; do we really need to unquote here?
                                                    ; have removed URL-mangling.
              :evidence/inferred-automatically "From CGC strain data"}))

       :strain/location
         {:strain.location/laboratory [:laboratory/id "CGC"]}

       :extras 
         (process-simple-locus genotype strain species strain-tid))
             
             )))
                           
     
     
