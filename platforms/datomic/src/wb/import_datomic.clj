(ns wb.import-datomic
  (:use acedb.acefile wb.utils)
  (:require [clojure.core.match :refer (match)]
            [clojure.string :as str]
            [datomic.api :as d]
            [clojure.instant :as inst]))

;
; "version 2" Datomic importer, for more credible ACeDB models.
; Note that this needs to be used with the wb.schema2/wb2 schema.
;

(defn- holder [key value]
  (when value
    {:db/id   (d/tempid :db.part/user)
     key      value}))

(defn- str-single [obj & path]
  (ffirst (select obj path)))

(defn- str-many [obj & path]
  (seq (map first (select obj path))))

(defn- ustr-single [obj & path]
  (when-let [t (ffirst (select obj path))]
    (unescape t)))

(defn- ustr-many [obj & path]
  (seq (map #(unescape (first %)) (select obj path))))

(defn- link-many [obj type & path]
  (seq (for [[k] (select obj path)]
         (holder type k))))

(defn- link-single [obj type & path]
  (when-let [t (ffirst (select obj path))]
    (holder type t)))

(defn- evidence-to-datomic [evseq]
  (conj-into {}
    (for [e evseq]
      (match (vec e)
        []                             nil   ; For now, no complains about missing evidence.
        ["Inferred_automatically" s]   [:evidence/automatic s]
        ["Paper_evidence" p]           [:evidence/paper (holder :paper/id p)]
        ["Person_evidence" s]          [:evidence/person (holder :person/id s)]
        ["Curator_confirmed" s]        [:evidence/curator (holder :person/id s)]
        ["Author_evidence" s text]     [:evidence/author
                                        {:evidence.author/author (holder :thing/id s)
                                         :evidence.author/note   text}]
        ["Accession_evidence" db id]   [:evidence/accession
                                        {:evidence.accession/database (holder :thing/id db)
                                         :evidence.accession/accession id}]
        ["Protein_id_evidence" s]      [:evidence/protein-id]
        ["GO_term_evidence" g]         [:evidence/go-term (holder :go/id g)]
        ["Expr_pattern_evidence" e]    [:evidence/expr-patern (holder :thing/id e)]
        ["Microarray_results_evidence"
         m]                            [:evidence/microarray-results (holder :thing/id m)]
        ["RNAi_evidence" s]            [:evidence/rnai (holder :rnai/id s)]
        ["CGC_data_submission"]        [:evidence/cgc-submission true]
        ["Curator_confirmed" p]        [:evidence/curator (holder :person/id p)]
        ["Feature_evidence" f]         [:evidence/feature (holder :feature/id f)]
        ["Laboratory_evidence" l]      [:evidence/laboratory (holder :laboratory/id l)]
        ["From_analysis" a]            [:evidence/analysis (holder :analysis/id a)]
        ["Variation_evidence" v]       [:evidence/variation (holder :variation/id v)]
        ["Mass_spec_evidence" m]       [:evidence/mass-spec (holder :thing/id m)]
        ["Sequence_evidence" s]        [:evidence/sequence (holder :sequence/id s)]
        ["Remark" r]                   [:evidence/remark r]
        ["Date_last_updated" _]        nil    ; This should be handled by transactions...
                                        ; :else (throw (Exception. (str "Don't understand evidence " (vec e))))))))
        :else (println "Don't understand evidence " (vec e))))))
  

(defn- elinks-to-datomic 
  "Datomize evidenced links, with a target as a placeholder entity using
   `holder-rel` to link to its key."
  [elinks holder-rel]
  (seq (for [[[k] evidence] (group-by-prefix 1 elinks)]
         (assoc (evidence-to-datomic evidence)
           :evidence/link (holder holder-rel k)))))

(defn- enotes-to-datomic 
  "Datomize evidenced notes."
  [enotes]
  (seq (for [[[note] evidence] (group-by-prefix 1 enotes)]
         (assoc (evidence-to-datomic evidence)
           :evidence/note note))))

(defmulti ace-to-datomic :class)

(defmethod ace-to-datomic "LongText"
  [obj]
  (let [{:keys [id text]} obj]
    [{:db/id           (d/tempid :db.part/user)
      :longtext/id     id
      :longtext/text   (unescape text)}]))

(defn- paper-type-to-datomic [lines]
  (seq
   (for [[t evidence] (group-by-prefix 1 lines)]
     (assoc (evidence-to-datomic evidence)
       :paper.type/type (or ({"Journal_article"       :paper.type.type/journal-article
                             "Review"                :paper.type.type/review
                             "Comment"               :paper.type.type/comment
                             "News"                  :paper.type.type/news
                             "Letter"                :paper.type.type/letter
                             "Editorial"             :paper.type.type/editorial
                             "Congresses"            :paper.type.type/congresses
                             "Historical_article"    :paper.type.type/historical
                             "Biography"             :paper.type.type/biography
                             "Interview"             :paper.type.type/interview
                             "Lectures"              :paper.type.type/lectures
                             "Interactive_tutorial"  :paper.type.type/interactive
                             "Retracted_publication" :paper.type.type/retracted
                             "Techical_report"       :paper.type.type/technical-report
                             "Directory"             :paper.type.type/directory
                             "Monograph"             :paper.type.type/monograph
                             "Published_erratum"     :paper.type.type/erratum
                             "Meeting_abstract"      :paper.type.type/meeting-abstract
                             "Gazette_article"       :paper.type.type/gazette-article
                             "Book_chapter"          :paper.type.type/book-chapter
                             "Book"                  :paper.type.type/book
                             "Email"                 :paper.type.type/email
                             "WormBook"              :paper.type.type/wormbook
                             "Other"                 :paper.type.type/other} t)
                            (except "Bad paper type " t))))))

(defmethod ace-to-datomic "Paper"
  [obj]
 [(vmap :db/id                  (d/tempid :db.part/user)
        :paper/id               (:id obj)
        :paper/legacy-name      (str-many obj "Name")
        :paper/status           (when-let [[[status]] (select obj ["Status"])]
                                  (or ({"Valid"    :paper.status/valid
                                        "Invalid"  :paper.status/invalid} status)
                                      (except "Bad paper status " status)))
        :paper/erratum-for      (link-many :paper/id obj "Erratum_for")
        
        :paper/author           (seq
                                 (map-indexed
                                  (fn [index [author ptag person]]
                                    (vmap
                                     :paper.author/ordinal (inc index)
                                     :paper.author/author (holder :author/id author)
                                     :paper.author/person (when (= ptag "Person")
                                                            (holder :person/id person))))
                                  (select obj ["Author"])))
        :paper/person           (elinks-to-datomic (select obj ["Person"]) :person/id)
        :paper/not-person       (elinks-to-datomic (select obj ["Not_person"]) :person/id)

        :paper/affiliation            nil ;; FIXME
                                 
        :paper/describes        (link-many :analysis/id obj "Describes_analysis")
        :paper/brief.citation   (ustr-single obj "Brief_citation")
        :paper/url              (str-single obj "URL")
        :paper/type             (paper-type-to-datomic (select obj "Type"))

        :paper/ref.title        (ustr-single obj "Reference" "Title")
        :paper/ref.journal      (str-single obj "Reference" "Journal")
        :paper/ref.publusher    (str-single obj "Reference" "Publisher")
        :paper/ref.editor       (str-many obj "Reference" "Editor")
        :paper/ref.page         (str-single obj "Reference" "Page")
        :paper/ref.volume       (str-single obj "Reference" "Volume")
        :paper/ref.date         (str-single obj "Reference" "Publication_date")
        :paper/ref.contained-in (link-many :paper/id obj "Reference" "Contained_in")

        ; :paper/abstract is never defined here because it comes via longtext

        :paper/curation-pipeline (when-let [_ (select obj ["Curation_pipeline" "Phenotype2GO"])]
                                   :paper.curation-pipeline/phenotype2go)
        :paper/keyword          (link-many :keyword/id obj "Keyword")
        :paper/remark           (seq (for [[a & evidence] (select obj ["Remark"])]
                                      (assoc (evidence-to-datomic [evidence])
                                        :evidence/note a))))])





(defn- gene-rnai-to-datomic [[rnai & evidence]]
  (assoc (evidence-to-datomic [evidence])
         :evidence/link {:rnai/id rnai}))

(defn- gene-desc-to-datomic [concise]
  (when (seq concise)
    (assoc (evidence-to-datomic (map rest concise))
      :gene.desc/concise (unescape (ffirst concise)))))

(defn- gene-status-to-datomic [status]
  (when (seq status)
    (assoc (evidence-to-datomic (map rest status))
      :gene.status/status ({"Live"        :gene.status.status/live
                            "Suppressed"  :gene.status.status/suppressed
                            "Dead"        :gene.status.status/dead} (ffirst status)))))

(defn- laboratory-to-datomic [lab]
  (when lab
    {:db/id           (d/tempid :db.part/user)
     :laboratory/id   lab}))


(defn- disease-model-to-datomic [dm]
  (seq (for [[[do-term species] evidence] (group-by-prefix 2 dm)]
         (assoc (evidence-to-datomic evidence)
           :gene.disease-model/do-term (holder :do/id do-term)
           :gene.disease-model/species (holder :species/id species)))))



(defmethod ace-to-datomic "Gene"
  [obj]
  (let [[evidence]          (select obj ["Evidence"])
        [[cgc_name]]        (select obj ["Identity" "Name" "CGC_name"])
        [[seq_name]]        (select obj ["Identity" "Name" "Sequence_name"])
        [[pub_name]]        (select obj ["Identity" "Name" "Public_name"])
        [[mol_name]]        (select obj ["Identity" "Name" "Molecular_name"])
        other-names         (select obj ["Identity" "Name" "Other_name"])
        status              (select obj ["Identity" "Status"])
        rnais               (select obj ["Experimental_info" "RNAi_result"])
        concise             (select obj ["Structured_description" "Concise_description"])
        [[gene-class]]      (select obj ["Gene_info" "Gene_class"])
        [[laboratory]]      (select obj ["Gene_info" "Laboratory"])]
    [(vmap :db/id                   (d/tempid :db.part/user)
           :gene/id                 (:id obj)
           :gene/name.cgc           cgc_name
           :gene/name.sequence      seq_name
           :gene/name.public        pub_name
           :gene/name.molecular     mol_name
           :gene/name.other         (seq (map first other-names))

           :gene/db-info            (seq (for [[db db-field acc] (select obj ["Identity" "DB_info" "Database"])]
                                           {:gene.db-info/db          (holder :database/id db)
                                            :gene.db-info/field       (holder :database-field/id db-field)
                                            :gene.db-info/accession   acc}))

           :gene/species            (holder :species/id (ffirst (select obj ["Identity" "Species"])))
           
           :gene/status             (gene-status-to-datomic status)

           :gene/class              (holder :thing/id gene-class)
           :gene/laboratory         (laboratory-to-datomic laboratory)
           :gene/cloned-by          (when-let [cbe (seq (select obj ["Gene_info" "Cloned_by"]))]
                                      (evidence-to-datomic cbe))
           :gene/ref-allele         (seq (for [[a & evidence] (select obj ["Gene_info" "Reference_allele"])]
                                      (assoc (evidence-to-datomic [evidence])
                                        :evidence/link (holder :variation/id a))))
           :gene/allele             (seq (for [[a & evidence] (select obj ["Gene_info" "Allele"])]
                                           (assoc (evidence-to-datomic [evidence])
                                             :evidence/link (holder :variation/id a))))
           :gene/possibly-affected  (seq (for [[a & evidence] (select obj ["Gene_info" "Possibly_affected_by"])]
                                      (assoc (evidence-to-datomic [evidence])
                                        :evidence/link (holder :variation/id a))))
           :gene/legacy-info        (seq (for [[a & evidence] (select obj ["Gene_info Legacy_information"])]
                                      (assoc (evidence-to-datomic [evidence])
                                        :evidence/note a)))
           :gene/complementation    (seq (map first (select obj ["Gene_info" "Complementation_data"])))
           :gene/strain             (seq (for [[s] (select obj ["Gene_info" "Strain"])]
                                           (holder :strain/id s)))

           :gene/in-cluster         (seq (for [[c] (select obj ["Gene_info" "In_cluster"])]
                                           (holder :thing/id c)))
           :gene/rnaseq             (seq (for [[[life-stage fpkm] evidence] (->> (select obj ["Gene_info" "RNASeq_FPKM"])
                                                                                 (group-by-prefix 2))]
                                           (assoc (evidence-to-datomic evidence)
                                             :gene.rnaseq/lifestage (holder :lifestage/id life-stage)
                                             :gene.rnaseq/fpkm (parse-float fpkm))))
           :gene/go                 (seq (for [[go-term go-code & evidence] (select obj ["Gene_info" "GO_term"])]
                                           (assoc (evidence-to-datomic [evidence])
                                             :gene.go/term (holder :go/id go-term)
                                             :gene.go/code (holder :thing/id go-code))))
           :gene/operon             (when-let [[[o]] (select obj ["Gene_info" "Contained_in_operon"])]
                                      (holder :operon/id o))

           :gene/ortholog           (seq (for [[[gene species] evidence] (->> (select obj ["Gene_info" "Ortholog"])
                                                                              (group-by-prefix 2))]
                                           (vassoc (evidence-to-datomic evidence)
                                             :gene.relation/gene (holder :gene/id gene)
                                             :gene.relation/species (holder :species/id species))))
           :gene/paralog            (seq (for [[[gene species] evidence] (->> (select obj ["Gene_info" "Ortholog"])
                                                                              (group-by (partial take 2)))]
                                           (vassoc (evidence-to-datomic (map (partial drop 2) evidence))
                                             :gene.relation/gene (holder :gene/id gene)
                                             :gene.relation/species (holder :species/id species))))
           :gene/ortholog-other     (seq (for [[[protein] evidence] (->> (select obj ["Gene_info" "Ortholog_other"])
                                                                         (group-by-prefix 1))]
                                           (assoc (evidence-to-datomic evidence)
                                             :evidence/link (holder :protein/id protein))))

           :gene/expt-model         (disease-model-to-datomic (select obj ["Disease_info" "Experimental_model"]))
           :gene/potential-model    (disease-model-to-datomic (select obj ["Disease_info" "Potential_model"]))
           :gene/disease-relevance  (seq (for [[[text species] evidence] (->> (select obj ["Disease_info" "Disease_relevance"])
                                                                              (group-by-prefix 2))]
                                           (assoc (evidence-to-datomic evidence)
                                             :gene.disease-relevance/note (unescape text)
                                             :gene.disease-relevance/species (holder :species/id species))))

           :gene/cds                (elinks-to-datomic (select obj ["Molecular_info" "Corresponding_CDS"]) :cds/id)
           :gene/transcript         (elinks-to-datomic (select obj ["Molecular_info" "Corresponding_transcript"]) :transcript/id)
           :gene/pseudogene         (elinks-to-datomic (select obj ["Molecular_info" "Corresponding_pseudogene"]) :pseudogene/id)
           :gene/transposon         (elinks-to-datomic (select obj ["Molecular_info" "Corresponding_transposon"]) :transposon/id)
           :gene/other-seq          (elinks-to-datomic (select obj ["Molecular_info" "Other_sequence"]) :sequence/id)
           :gene/associated-feature (elinks-to-datomic (select obj ["Molecular_info" "Associated_feature"]) :feature/id)
           :gene/product-binds      (seq (for [[f] (select obj ["Molecular_info" "Gene_product_binds"])]
                                           (holder :feature/id f)))
           :gene/transcription-factor (seq (for [[f] (select obj ["Molecular_info" "Transcription_factor"])]
                                             (holder :txn-factor/id f)))


           ; Most of these seem to belong on the experiment, not on the gene ????
           
           ; :gene/rnai           (seq (map gene-rnai-to-datomic rnais))
           ; :gene/expr-pattern ...
           ; :gene/drives-transgene ...
           ; :gene/transgene-product ... 
           ; :gene/regulate-expr-cluster ..
           ; :gene/antibody ..
           ; :gene/microarray-results ..
           ; :gene/expr-cluster ..
           ; :gene/sage-tag ..
           ; :gene/3d-data ..
           ; :gene/interaction ..
           ; :gene/anatomy-function ..
           ; :gene/product-binds-matrix ..
           ; :gene/process ..

           :gene/desc               (gene-desc-to-datomic concise)
           
           :gene/reference          (elinks-to-datomic (select obj ["Reference"])
                                                       :paper/id)
           :gene/remark             (seq (for [[[text] evidence] (->> (select obj ["Remark"])
                                                                      (group-by-prefix 1))]
                                           (assoc (evidence-to-datomic evidence)
                                             :evidence/note text)))
           :gene/method             (when-let [[[method]] (select obj ["Method"])]
                                      (holder :method/id method))
           )]))



(defn- addr-to-datomic [lines]
  (when (seq lines)
    (let [obj {:lines lines}]
      (vmap
       :address/street          (str-many   obj "Street_address")
       :address/country         (str-single obj "Country")
       :address/institution     (str-single obj "Institution")
       :address/email           (str-many   obj "Email")
       :address/phone.main      (str-many   obj "Main_phone")
       :address/phone.lab       (str-many   obj "Lab_phone")
       :address/phone.office    (str-many   obj "Office_phone")
       :address/phone.other     (seq (for [[phone note] (select obj ["Other_phone"])]
                                       (vmap :address.phone.other/phone phone
                                             :address.phone.other/note note)))
       :address/fax             (str-many   obj "Fax")
       :address/web-page        (str-many   obj "Web_page")))))

(defn- person-lineage-to-datomic [lines]
  (seq
   (for [[p role from to] lines]
     (vmap
       :person.lineage/person
         (holder :person/id p)
       :person.lineage/role
         (when role
           (or ({"Assistant_professor"       :person.lineage.role/assistant-professor
                 "Phd"                       :person.lineage.role/phd
                 "Postdoc"                   :person.lineage.role/postdoc
                 "Masters"                   :person.lineage.role/masters
                 "Undergrad"                 :person.lineage.role/undergrad
                 "Highschool"                :person.lineage.role/highschool
                 "Sabbatical"                :person.lineage.role/sabbatical
                 "Lab_visitor"               :person.lineage.role/lab-visitor
                 "Collaborated"              :person.lineage.role/collaborated
                 "Research_staff"            :person.lineage.role/research-staff
                 "Unknown"                   :person.lineage.role/unknown} role)      ; Maybe better to leave blank?
               (throw (Exception. (str "Unknown role " role)))))
       :person.lineage/date-from
         (when from
           (inst/read-instant-date from))
       :person.lineage/date-to
         (when to
           (inst/read-instant-date to))))))

(defmethod ace-to-datomic "Person"
  [obj]
  [(vmap :db/id                        (d/tempid :db.part/user)
         :person/id                    (:id obj)
         :person/name.first            (str-single obj "Name" "First_name")
         :person/name.middle           (str-many   obj "Name" "Middle_name")
         :person/name.last             (str-single obj "Name" "Last_name")
         :person/name.standard         (str-single obj "Name" "Standard_name")
         :person/name.full             (str-single obj "Name" "Full_name")
         :person/alias                 (str-many   obj "Name" "Also_known_as")

         :person/cgc.rep               (link-many :laboratory/id obj "CGC_representative_for")
         :person/laboratory            (link-many :laboratory/id obj "Laboratory")

         :person/address               (addr-to-datomic (select obj ["Address"]))

         :person/supervised            (person-lineage-to-datomic
                                        (select obj ["Lineage" "Supervised"])) 
         :person/worked-with           (person-lineage-to-datomic
                                        (select obj ["Lineage" "Worked_with"]))
         
         :person/comment               (str-many obj "Comment")
         :person/publishes-as          (link-many :author/id obj "Publication" "Publishes_as")
         :person/possibly-publishes-as (link-many :author/id obj "Publication" "Possibly_publishes_as"))])
         
         

(defmethod ace-to-datomic "Laboratory"
  [obj]
  [(vmap
    :db/id                            (d/tempid :db.part/user)
    :laboratory/id                    (:id obj)
    :laboratory/addr.mail             (ustr-many obj "Address" "Mail")
    :laboratory/addr.phone            (ustr-many obj "Address" "Phone")
    :laboratory/addr.email            (ustr-many obj "Address" "E_mail")
    :laboratory/addr.fax              (ustr-many obj "Address" "Fax")
    :laboratory/addr.url              (ustr-many obj "Address" "URL")

    :laboratory/cgc.strain-designation   (str-single obj "CGC" "Strain_designation")
    :laboratory/cgc.allele-designation   (str-single obj "CGC" "Allele_designation")
    :laboratory/cgc.alleles              (link-many :variant/id obj "CGC" "Alleles")

    :laboratory/remark                   (enotes-to-datomic (select obj ["Remark"])))]) 
         

(def ^:private rnai-delivery-to-datomic
  {"Bacterial_feeding"       :rnai.delivery/feeding
   "Injection"               :rnai.delivery/injection
   "Soaking"                 :rnai.delivery/soaking
   "Transgene_expression"    :rnai.delivery/transgene})

(defmethod ace-to-datomic "RNAi"
  [obj]
  (let [[[delivery]]        (select obj ["Experiment" "Delivered_by"])
        [[strain]]          (select obj ["Experiment" "Strain"])
        phenotypes          (map first (select obj ["Phenotype"]))
        not_phenotypes      (map first (select obj ["Phenotype_not_observed"]))
        refs                (map first (select obj ["Reference"]))]
    [(vmap :db/id                (d/tempid :db.part/user)
           :rnai/id              (:id obj)
           :rnai/expt.strain     strain
           :rnai/expt.delivery   (rnai-delivery-to-datomic delivery)
           :rnai/phenotype       (for [p phenotypes]
                                   {:db/id        (d/tempid :db.part/user)
                                    :phenotype/id p})
           :rnai/not.phenotype   (for [p not_phenotypes]
                                   {:db/id        (d/tempid :db.part/user)
                                    :phenotype/id p})
           :rnai/reference       (for [r refs]
                                   {:db/id        (d/tempid :db.part/user)
                                    :paper/id     r}))]))

(defmethod ace-to-datomic "Phenotype"
  [obj]
  (let [[[desc]]      (select obj ["Description"])
        [[name]]      (select obj ["Name" "Primary_name"])]
    [(vmap :db/id                  (d/tempid :db.part/user)
           :phenotype/id           (:id obj)
           :phenotype/name         name
           :phenotype/description  desc)]))

(defmethod ace-to-datomic "Protein"
  [obj]
  [(vmap
    :db/id                  (d/tempid :db.part/user)
    :protein/id             (:id obj)
    :protein/species        (link-single obj :species/id "Origin" "Species")
    :protein/db-info        (seq (for [[db db-field acc] (select obj ["DB_info" "Database"])]
                                   {:db-info/db          (holder :database/id db)
                                    :db-info/field       (holder :database-field/id db-field)
                                    :db-info/accession   acc})))])
   

(defmethod ace-to-datomic :default
  [obj]
  [])   ; Make no assertions about AceDB objects we don't understand

(defn import-acefile 
  "Open `file` as a .ace file.  Convert objects we understand into
  Datomic transactions"
  [file]
  (->> (ace-reader file)
       (ace-seq)
       (partition-by :class)
       (map #(mapcat ace-to-datomic %))
       (filter seq)))    ; Discard any empty transactions
