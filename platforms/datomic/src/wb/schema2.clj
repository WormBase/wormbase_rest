(ns wb.schema2
  (:use datomic-schema.schema)
  (:require [datomic.api :as d]))

(def wb2
  (concat
   (generate-schema d/tempid
                    
 [(schema longtext
    (fields
     [:id :string :unique-identity]
     [:text :string :fulltext]))


  (schema thing   ;something we haven't modelled yet.
   (fields
    [:id :string :unique-identity]))        
  
  (schema phenotype
    (fields
     [:id :string :unique-identity]
     [:description :string :fulltext]
     [:name :string]))
  
  (schema paper
    (fields
     [:id :string :unique-identity]
     [:author :ref :component :many]
     [:brief.citation :string]
     [:ref.title :string]
     [:ref.journal :string]
     [:ref.volume :string]
     [:ref.page :string]
     [:abstract :ref]))
  (schema paper.author
    (fields
     [:ordinal :long]
     [:name :string]))

  
  ;
  ; Person model and sub-tags
  ;

  (schema person
   (fields
    [:id :string :unique-identity]
    [:name.first :string]
    [:name.middle :string :many]
    [:name.last :string]
    [:name.standard :string]
    [:name.full :string]
    [:alias :string :many]

    [:cgc.rep :ref :many]

    [:laboratory :ref :many]

    [:affiliation :string :many]

    [:conducted :ref :many]

    [:address :ref :component]

                                        ; Elide tracking for now

    [:supervised :ref :many :component]
    [:worked-with :ref :many :component]
    
    [:comment :string :many]
    [:paper :ref :many]
    [:not.paper :ref :many]
    [:publishes-as :ref :many]
    [:possibly-publishes-as :ref :many]))

  (schema address
   (fields
    [:street :string :many]
    [:country :string]
    [:institution :string]
    [:email :string :many]
    [:phone.main :string :many]
    [:phone.lab :string :many]
    [:phone.office :string :many]
    [:phone.other :string :many]     ; Currently not modeling notes
    [:fax :string :many]
    [:web-page :string :many]))
    
  (schema person.lineage
   (fields
    [:person :ref]
    [:role :enum [:assistant-professor :phd :postdoc :masters :undergrad
                  :highschool :sabbatical :lab-visitor :collaborated
                  :research-staff :unknown]]))
    
    
  ;
  ; Gene model and sub-tags
  ;

  
  (schema gene
    (fields
     [:id :string :unique-identity]
     [:sequence :ref]
     
     [:name.cgc :string]
     [:name.sequence :string]
     [:name.public :string]
     [:name.molecular :string :many]
     [:name.other :string :many]

     [:db-info :ref :component :many]

     [:species :ref]

     ; Eliding history until we've worked out what can be done with txns

     [:status :ref :component]

     ; Gene_info
     [:class :ref]
     [:laboratory :ref]
     [:cloned-by :ref :component] ; "bare" evidence object
     [:ref-allele :ref :component]
     [:allele :ref :component]
     [:possibly-affected :ref :component]
     [:legacy-info :ref :component]
     [:complementation :string]
     [:strain :ref]
     [:in-cluster :ref]
     [:rnaseq :ref :component]
     [:go :ref :component]
     [:operon :ref]              ; Equivalent to "Contained_in", which doesn't seem terribly descriptive
     [:ortholog :ref :component :many]
     [:paralog :ref :component :many]
     [:ortholog-other :ref :component :many]

     ; Disease_info
     [:expt-model :ref :component :many]
     [:potential-model :ref :component :many]
     [:disease-relevance :ref :component :many]

     ; Molecular_info
     [:cds :ref :component :many]
     [:transcript :ref :component :many]
     [:pseudogene :ref :component :many]
     [:transposon :ref :component :many]
     [:other-seq :ref :component :many]
     [:associated-feature :ref :component :many]
     [:product-binds :ref :many]
     [:transcription-factor :ref]

     ; Experimental_info
     [:rnai :ref :many :component]
     [:expr-pattern :ref]
     [:drives-transgene :ref]
     [:transgene-product :ref]
     [:regulate-expr-cluster :ref]
     [:antibody :ref]
     [:microarray-results :ref]
     [:expr-cluster :ref :component :many]
     [:sage-tag :ref :component :many]
     [:3d-data :ref]
     [:interaction :ref :many]
     [:anatomy-function :ref :many]
     [:product-binds-matrix :ref :many]   ; ??? Shouldn't this go via TF?
     [:process :ref :component :many]

     ; Map_info (elide for now...)
     
     [:reference :ref :many]
     [:remark :ref :component :many]
     [:method :ref]))

  
  (schema gene.desc
    (fields
     ; Also has evidence
     [:concise :string :fulltext]))
  (schema gene.db-info
   (fields
    [:db :ref]
    [:field :ref]
    [:accession :string]))
  (schema gene.status
   (fields
    ; has evidence        
    [:status :enum [:life :suppressed :dead]]))
  (schema gene.rnaseq
   (fields
    ; has evidence        
    [:life-stage :ref]
    [:fpkm :float]))
  (schema gene.go
   (fields
    ; has evidence
    [:term :ref]
    [:code :ref]))  ; Kind-of an enum...
  (schema gene.disease-model
   (fields
    ; has evidence
    [:do-term :ref]
    [:species :ref]))
  (schema gene-diseaase-relevance
   (fields        
    ; has evidence
    [:note :string]
    [:species :ref]))



  (schema go
   (fields
    [:id :string :unique-identity]))

  (schema laboratory
   (fields
    [:id :string :unique-identity]))

  (schema analysis
   (fields
    [:id :string :unique-identity]))

  (schema variation
   (fields
    [:id :string :unique-identity]))

  (schema sequence
   (fields
    [:id :string :unique-identity]))
  
  (schema feature
   (fields
    [:id :string :unique-identity]))

  
  ;
  ; Evidence model and sub-tags
  ;
  
  (schema evidence
   (fields
     [:link :ref]             ; Not an evidence tag.  Can be used to link to another entity.
     [:note :string]          ; Not an evidence tag.
     [:paper :ref :many]
     [:published :string :many]
     [:person :string :many]
     [:author :ref :many]     ; link entity
     [:accession :ref :many]  ; link entity
     [:protein-id :string :many]
     [:go-term :ref :many]
     [:expr-pattern :ref :many]
     [:microarray-results :ref :many]
     [:rnai :ref :many]
     [:cgc-submission :boolean]
     [:curator :ref :many]
     [:automatic :string :many]
     [:feature :ref :many]
     [:laboratory :ref :many]
     [:analysis :ref :many]
     [:variation :ref :many]
     [:mass-spec :ref :many]
     [:sequence :ref :many]
     [:remark :string :many]))

  (schema evidence.author
   (fields
    [:author :ref]
    [:note :string]))

  (schema evidence.accession
   (fields
    [:database :ref]
    [:accession :string])) ; check


  
  (schema rnai
   (fields
    [:id :string :unique-identity]
    [:expt.strain :string]
    [:expt.delivery :ref]  ; could be [:delivery :enum [:feeding :injection :soaking :transgene]]
    [:phenotype :ref :many]
    [:not.phenotype :ref :many]
    [:reference :ref :many]))])

   ; These are done by hand because the :enum stuff in datomic-schma
   ; Can't quite match my original names.
 
   
   [{:db/id            #db/id[:db.part/user]
     :db/ident         :rnai.delivery/feeding}

    {:db/id            #db/id[:db.part/user]
     :db/ident         :rnai.delivery/injection}

    {:db/id            #db/id[:db.part/user]
     :db/ident         :rnai.delivery/soaking}

    {:db/id            #db/id[:db.part/user]
     :db/ident         :rnai.delivery/transgene}]))

    
  
