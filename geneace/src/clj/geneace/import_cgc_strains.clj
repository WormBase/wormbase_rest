(ns geneace.import-cgc-strains
  (:use clojure.java.io
        clojure.tools.logging
        clj-logging-config.log4j
        geneace.utils
        clojure.pprint)
  (:require [instaparse.core :as insta]
            [datomic.api :as d]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clj-time.format :as tf]
            [clj-time.coerce :refer [to-date]]
            [clj-http.client :as http]
            [clojure.edn :as edn])
  (:import org.apache.poi.xwpf.extractor.XWPFWordExtractor
           org.apache.poi.xwpf.usermodel.XWPFDocument))

(set-loggers! :root {:level :warn})

(def ^:dynamic *geneace-rest-endpoint* "http://localhost:4664")
(def ^:dynamic *geneace-db-alias* "ace/ga1")

(defn geneace-q 
  "Query Datomic via the REST API.  A suitable DB alias is inserted as the first parameter."
  [q & params]
  (->> (http/post
        (str *geneace-rest-endpoint* "/api/query")
        {:body 
         (pr-str 
          {:q q
           :args (cons {:db/alias *geneace-db-alias*}
                       params)})
         :content-type "application/edn"})
       (:body)
       (edn/read-string)))

(defn docx-text [doc]
  (->> (input-stream doc)
       (XWPFDocument.)
       (XWPFWordExtractor.)
       (.getText)))

(defn is-zip? [is]
  (.mark is 4)
  (let [ba (byte-array 4)]
    (.read is ba 0 4)
    (.reset is)
    (= (vec ba) [80 75 3 4])))

(defn read-text [doc]
  (with-open [is (input-stream doc)]
    (if (is-zip? is)
      (docx-text is)
      (slurp is))))

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

(defn find-person [name]
  (debugf "Looking up person %s" name)
  (geneace-q
   '[:find [?person-id ...]
     :in $ ?name
     :where (or [?p :person/standard-name ?name]
                [?p :person/full-name ?name]
                [?p :person/also-known-as ?name])
            [?p :person/id ?person-id]]
   name))

(defn get-variation-id [public-name]
 (ffirst
  (geneace-q 
   '[:find ?var-id 
     :in $ ?name
     :where [?v :variation/public-name ?name]
            [?v :variation/id ?var-id]]
   public-name)))

(defn get-gene-id [public-name]
 (ffirst 
  (geneace-q
   '[:find ?gene-id
     :in $ ?name
     :where (or-join [?g ?name]
              [?g :gene/public-name ?name]
              [?g :gene/sequence-name ?name]
              (and [?cgc :gene.cgc-name/text ?name]
                   [?g :gene/cgc-name ?cgc]))
            [?g :gene/id ?gene-id]]
   public-name)))

(defn get-transgene-id [public-name]
 (ffirst
  (geneace-q
   '[:find ?tg-id 
     :in $ ?name
     :where [?tg :transgene/public-name ?name]
            [?tg :transgene/id ?tg-id]]
   public-name)))

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
         (fn [[match gene allele]]
           (infof "simple combination: %s" match)
           (check-details gene allele strain species strain-tid))

         ;; find transposon insertions
         #"([a-z]+(Si|Ti)\d+)"
         (fn [[match allele]]
           (infof "transposon: %s" match)
           (check-details nil allele strain species strain-tid))

         ;; find chromosomal aberrations e.g. szT1
         #"\s*([a-z]{1,3}(Dp|Df|In|T|C)\d+)"
         (fn [[match rearrangement]]
           (infof "rearrangement: %s" match)
           [{:db/id [:rearrangement/id rearrangement]
             :rearrangement/strain strain-tid}])

         ;; find transgenes e.g. zhEx11
         #"\s*([a-z]{1,3}(Ex|Is)\d+)"
         (fn [[match transgene]]
           (infof "transgene: %s" match)
           (if-let [tgid (get-transgene-id transgene)]
             ;; this doesn't seem to work because transgenes don't have public-names in geneace?
             [{:db/id [:transgene/id tgid]
               :transgene/strain strain-tid}]))

         ;; find double barrelled alleles (revertants) e.g. daf-12(rh61rh412) 
         #"([Ca-z\-]{3,6}\-\d+\.{0,1}\d*)\(([a-z]{1,2}\d+)([a-z]{1,2}\d+)\)"
         (fn [[match gene allele-1 allele-2]]
           (infof "double-barrelled: %s" match)
           (concat
            (check-details gene allele-1 strain species strain-tid)
            (check-details gene allele-2 strain species strain-tid)))

         ;; alleles affecting 2 genes like arf-1.1&F45E4.7(ok1840)
         #"([\w\.\-]+)&([\w\.\-]+)\(([a-z]{1,2}\d+)\)"
         (fn [[match gene-1 gene-2 allele]]
           (infof "two-gene allele: %s" match)
           (concat
            (check-details gene-1 allele strain species strain-tid)
            (check-details gene-2 allele strain species strain-tid)))

         ;; find alleles attached to non-approved, or unusual gene names e.g. let-?(h661)
         #"([\w\.\-\?]+)\(([a-z]{1,2}\d+)\)"
         (fn [[match gene allele]]
           (infof "Unusual gene name: %s" match)
           (check-details gene allele strain species strain-tid))

         ;; find any skulking gene names missed by steps above, these are often where there is no allele name
         ;;or the allele name is wild-type, e.g. unc-24(+)
         #"([a-z]{3,4}\-\d+)/"
         (fn [[match gene]]
           (infof "Odd gene name: %s" match)
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
          datoms)))))   ; return the final datom list.
          

(def cgc-date-format (tf/formatter "MM/dd/yy"))           

(defn import-strain [{:keys [strain species genotype made-by received
                             description mutagen outcrossed] 
                      :as data}]
  (when strain
    (infof "Processing %s" strain)
    (let [made-by-person (if made-by
                           (find-person made-by))
          strain-tid (d/tempid :db.part/user)]
     (cons
      (vmap
       :db/id 
         strain-tid
       :strain/id 
         (str "test-" strain)
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
           [[:person/id (first made-by-person)]])
       :strain/cgc-received
         (if received
           (if-let [rd (re-find #"\d+/\d+/\d+" received)]
             (to-date (tf/parse cgc-date-format rd))))
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
         {:strain.location/laboratory [:laboratory/id "CGC"]})

      (process-genotype genotype strain species strain-tid))
             
             )))

(def cli-options
  [["-d" "--debug"]
   ["-v" "--verbose"]
   [nil  "--cgc-file FILE" "File to parse (text or .docx)"]
   [nil  "--db-uri URI" "Database endpoint URI"]
   [nil  "--db-alias ALIAS" "Database alias"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options arguments summary errors] :as o}
        (cli/parse-opts args cli-options)]
    (when errors
      (println errors)
      (System/exit 1))
    (when (:help options)
      (println summary)
      (System/exit 0))

    (println options)

    (set-loggers! 
     "geneace.import-cgc-strain" 
     {:level (cond
              (:debug options)   :debug
              (:verbose options) :verbose
              :default           :warn)}

     (binding [*geneace-rest-endpoint*
               (or (:db-uri options)
                   *geneace-rest-endpoint*)
               *geneace-db-alias*
               (or (:db-alias options)
                   *geneace-db-alias*)]
       (pprint (mapcat import-strain (rest (second (strain-parse-xfrm (read-text (:cgc-file options))))))))
     (System/exit 0))))
     
