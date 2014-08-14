(ns wb.schema2
  (:use datomic-schema.schema)
  (:require [datomic.api :as d]))

(def wb2
  (concat
   (generate-schema d/tempid
                    
  [

;
; Bits and pieces that are used in multiple places
;                  


   (schema db-info
    (fields      ; Would it make more sense to just reach the DB via field?
      [:db :ref]
      [:field :ref]
      [:accession :string]))
   

   (schema longtext
    (fields
     [:id :string :unique-identity]
     [:text :string :fulltext]))


;
; Placeholder for something we haven't modelled yet.  Should eventually die.
;
   
  (schema thing
   (fields
    [:id :string :unique-identity]))        
  
  (schema phenotype
    (fields
     [:id :string :unique-identity]
     [:description :string :fulltext]
     [:name :string]))

  ;
  ; Paper model and sub-tags
  ;
  
  (schema paper
    (fields
     [:id :string :unique-identity]
     [:legacy-name :string :many]
     [:db-info :ref :many :component]
     ; Eliding history for now.
     [:status :enum [:valid :invalid]]
     [:erratum-for :ref :many]
     
     
     [:author :ref :component :many]
     [:person :ref :component :many]
     [:not-person :ref :component :many]
     [:affiliation :ref :component :many]

     [:describes :ref :many]    ; --> :analysis
     
     [:brief.citation :string]
     [:abstract :string :fulltext]
     [:url :string]

     [:type :ref :many :component]
     
     
     [:ref.title :string]
     [:ref.journal :string]
     [:ref.publisher :string]
     [:ref.editor :string :many]
     [:ref.volume :string]
     [:ref.page :string]
     [:ref.date :string]        ; Following Ace models in not using :instant
     [:ref.contained-in :ref]

     [:abstract :ref]

     [:curation-pipeline :enum [:phenotype2go]]
     [:keyword :ref :many]
     [:remark :ref :many :component]))
  (schema paper.author
    (fields
     [:ordinal :long]
     [:author :ref]
     [:person :ref]
     [:address :string]))
  (schema paper.person
    (fields
     [:ordinal :long]
     [:person :ref]))
  (schema paper.type
    (fields
     ; has evidence     
     [:type :enum [:journal-article :review :comment :news :letter :editorial
                   :congresses :historical :biography :interview :lectures
                   :interactive-tutorial :retracted :techical-report :directory
                   :monograph :erratum :meeting-abstract :gazette-article
                   :book-chapter :book :email :wormbook :other]]))
     
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

    ; [:conducted :ref :many]           -- Link other way...

    [:address :ref :component]

                                        ; Elide tracking for now

    [:supervised :ref :many :component]
    [:worked-with :ref :many :component]
    
    [:comment :string :many]
    ; [:paper :ref :many]              -- Link from paper
    ; [:not.paper :ref :many]
    [:publishes-as :ref :many]
    [:possibly-publishes-as :ref :many]))

  (schema address
   (fields
    [:street :string :many]     ; FIXME: needs to be ordered.
    [:country :string]
    [:institution :string]
    [:email :string :many]
    [:phone.main :string :many]
    [:phone.lab :string :many]
    [:phone.office :string :many]
    [:phone.other :ref :many :component]
    [:fax :string :many]
    [:web-page :string :many]))
  (schema address.phone.other
   (fields
    [:phone :string]
    [:note :string]))
  
    
  (schema person.lineage
   (fields
    [:person :ref]
    [:role :enum [:assistant-professor :phd :postdoc :masters :undergrad
                  :highschool :sabbatical :lab-visitor :collaborated
                  :research-staff :unknown]]
    [:date-from :instant]
    [:date-to :instant]))
    


  ;
  ; Author model
  ;

  (schema author
   (fields
    [:id :string :unique-identity]
    [:full-name :string :many]
    [:alias :string :many]
    [:old-lab :ref :many]

    ; Why doesn't this use "real" address?
    [:addr.mail :string :many]
    [:addr.email :string :many]
    [:addr.phone :string :many]
    [:addr.fax :string :many]

    [:paper :ref :many]
    ; [:sequence :ref :many]
    [:keyword :ref :many]))
    
    
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
     [:ref-allele :ref :component :many]
     [:allele :ref :component :many]
     [:possibly-affected :ref :component :many]
     [:legacy-info :ref :component]
     [:complementation :string :many]
     [:strain :ref :many]
     [:in-cluster :ref :many]
     [:rnaseq :ref :component :many]
     [:go :ref :component :many]
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
     [:transcription-factor :ref :many]

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
     
     [:reference :ref :component :many]
     [:remark :ref :component :many]
     [:method :ref]))

  (schema gene.desc
    (fields
     ; Also has evidence
     [:concise :string :fulltext]))

  (schema gene.status
   (fields
    ; has evidence
    [:status :enum [:live :suppressed :dead]]))
  (schema gene.rnaseq
   (fields
    ; has evidence        
    [:lifestage :ref]
    [:fpkm :float]))
  (schema gene.go
   (fields
    ; has evidence
    [:term :ref]
    [:code :ref]))  ; Kind-of an enum...
  (schema gene.relation
   (fields
    ; has evidence
    [:gene :ref]
    [:species :ref]))
  (schema gene.disease-model
   (fields
    ; has evidence
    [:do-term :ref]
    [:species :ref]))
  (schema gene.disease-relevance
   (fields        
    ; has evidence
    [:note :string]
    [:species :ref]))

  (schema gene-class
   (fields       
    [:id :string :unique-identity]
    ; What to do about the top-level evidence, if any?  Is this used anywhere?
    [:phenotype :string :many]    ; Why not refs to Phenotype objects?
    [:description :string :many]
    [:designating-laboratory :ref]
    [:former-designating-laboratory :ref]
    [:main-name :ref :many]
    [:remark :ref :many]))
    
    
  (schema method
   (fields
    [:id :string :unique-identity]))

  (schema go
   (fields
    [:id :string :unique-identity]))

  (schema do
   (fields
    [:id :string :unique-identity]))

  (schema laboratory
   (fields
    [:id :string :unique-identity]

    ; Icky non-standard address to mirror ACeDB.
    ; Can we regularize this?
    [:addr.mail :string :many]
    [:addr.phone :string :many]
    [:addr.email :string :many]
    [:addr.fax :string :many]
    [:addr.url :string :many]

    [:cgc.strain-designation :string]
    [:cgc.allele-designation :string]
    [:cgc.alleles :ref :many]
    ; Gene_classes handled on :gene-class
   
    ; Staff handled on :person
    
    [:remark :ref :many]))

  (schema analysis
   (fields
    [:id :string :unique-identity]))

  (schema variation
   (fields
    [:id :string :unique-identity]))

  (schema sequence
   (fields
    [:id :string :unique-identity]))

  (schema protein
   (fields
    [:id :string :unique-identity]))
          
  (schema feature
   (fields
    [:id :string :unique-identity]))

  (schema lifestage
   (fields
    [:id :string :unique-identity]))

  (schema operon
   (fields
    [:id :string :unique-identity]))

  (schema species
   (fields       
    [:id :string :unique-identity]))

  (schema strain
   (fields
    [:id :string :unique-identity]))


  (schema cds
   (fields
    [:id :string :unique-identity]))

  (schema transcript
   (fields
    [:id :string :unique-identity]))

  (schema transposon
   (fields
    [:id :string :unique-identity]))

  (schema pseudogene
   (fields
    [:id :string :unique-identity]))

  (schema txn-factor
   (fields
    [:id :string :unique-identity]))

  (schema database
   (fields
    [:id :string :unique-identity]))

  (schema database-field
   (fields
    [:id :string :unique-identity]))

  (schema keyword
   (fields
    [:name :string :unique-identity]))        
  ;
  ; Evidence model and sub-tags
  ;
  
  (schema evidence
   (fields
     [:link :ref]             ; Not an evidence tag.  Can be used to link to another entity.
     [:note :string]          ; Not an evidence tag.
     [:paper :ref :many]
     [:published :string :many]
     [:person :ref :many]
     [:author :ref :many :component]     ; link entity
     [:accession :ref :many :component]  ; link entity
     [:protein-id :string :many]
     [:go-term :ref :many]
     [:expr-pattern :ref :many]
     [:microarray-results :ref :many]
     [:rnai :ref :many]
     [:cgc-submission :boolean :many]    ; :many is silly, but keeps evidence-to-datomic regular
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

    
  
