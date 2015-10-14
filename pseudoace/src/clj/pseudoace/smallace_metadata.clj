(ns pseudoace.smallace-metadata
  (:require [datomic.api :refer (tempid)]))

;;
;; Should fairly definitely move somewhere else if
;; this project goes anywhere...
;;

(def smallace
  [{:db/id                  (tempid :db.part/db)
    :db/ident               :gene/id
    :pace/identifies-class  "Gene"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/name.cgc
    :pace/tags              "Identity Name CGC_name"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/name.sequence
    :pace/tags              "Identity Name Sequence_name"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/name.public
    :pace/tags              "Identity Name Public_name"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/desc
    :pace/tags              "Structured_description"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene.desc/concise
    :pace/tags              "Concise_description"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/reference
    :pace/tags              "Reference"
    :pace/obj-ref           :paper/id}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene/rnai
    :pace/tags              "Experimental_info RNAi_result"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :gene.rnai/rnai
    :pace/tags              ""
    :pace/obj-ref           :rnai/id}

   ;; Paper class
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/id
    :pace/identifies-class  "Paper"
    :pace/xref              {:db/id               (tempid :db.part/user)  ;; Recent Datomic forbids any
                                                                          ;; non-schema entities in :db.part/db
                             :pace.xref/tags      "Refers_to Gene"
                             :pace.xref/attribute :gene/reference
                             :pace.xref/obj-ref   :gene/id}}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/author
    :pace/tags              "Author"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/brief.citation
    :pace/tags              "Brief_citation"}
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/ref.title
    :pace/tags              "Reference Title"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/ref.journal
    :pace/tags              "Reference Journal"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/ref.volume
    :pace/tags              "Reference Volume"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/ref.page
    :pace/tags              "Reference Page"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper/abstract
    :pace/tags              "Abstract"
    :pace/obj-ref           :longtext/id}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :paper.author/name
    :pace/tags              ""}

   ;; LongText will need some special-casing...
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :longtext/id
    :pace/identifies-class  "LongText"}

   ;; Evidence model
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :evidence/paper
    :pace/tags              "Paper_evidence"
    :pace/obj-ref           :paper/id
    :pace/order             1000}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :evidence/person
    :pace/tags              "Person_evidence"
    :pace/order             1000}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :evidence/curator
    :pace/tags              "Curator_confirmed"
    :pace/order             1000}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :evidence/automatic
    :pace/tags              "Inferred_automatically"
    :pace/order             1000}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :evidence/rnai
    :pace/tags              "RNAi_evidence"
    :pace/obj-ref           :rnai/id
    :pace/order             1000}

   ;; RNAi class
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai/id
    :pace/identifies-class  "RNAi"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai/expt.strain
    :pace/tags              "Experiment Strain"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai/expt.delivery
    :pace/tags              "Experiment Delivered_by"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai/phenotype
    :pace/tags              "Phenotype"
    :pace/obj-ref           :phenotype/id}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai/not.phenotype
    :pace/tags              "Phenotype_not_observed"
    :pace/obj-ref           :phenotype/id}

   ;; RNAi delivery enum:

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai.delivery/feeding
    :pace/tags              "Bacterial_feeding"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai.delivery/injection
    :pace/tags              "Injection"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai.delivery/soaking
    :pace/tags              "Soaking"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :rnai.delivery/transgene
    :pace/tags              "Transgene_expression"}

   
   
   ;; Phenotype class
   
   {:db/id                  (tempid :db.part/db)
    :db/ident               :phenotype/id
    :pace/identifies-class  "Phenotype"
    :pace/xref              [{:db/id               (tempid :db.part/user)
                              :pace.xref/tags      "Attribute_of RNAi"
                              :pace.xref/attribute :rnai/phenotype
                              :pace.xref/obj-ref   :rnai/id}

                             {:db/id               (tempid :db.part/user)
                              :pace.xref/tags      "Attribute_of Not_in_RNAi"
                              :pace.xref/attribute :rnai/not.phenotype
                              :pace.xref/obj-ref   :rnai/id}]}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :phenotype/description
    :pace/tags              "Description"}

   {:db/id                  (tempid :db.part/db)
    :db/ident               :phenotype/name
    :pace/tags              "Name"}
   
   ;; Explicit models for now.
   
   {:db/id                  (tempid :db.part/user)
    :pacemodel/name         "Gene"
    :pacemodel/model
"?Gene    Identity Name CGC_name      UNIQUE ?Gene_name XREF CGC_name_for #Evidence
                       Sequence_name UNIQUE ?Gene_name XREF Sequence_name_for
                       Public_name   UNIQUE ?Gene_name XREF Public_name_for
         Experimental_info RNAi_result ?RNAi XREF Gene #Evidence
         Structured_description Concise_description ?Text #Evidence
         Reference ?Paper XREF Gene"}

   {:db/id                 (tempid :db.part/user)
    :pacemodel/name        "Evidence"
    :pacemodel/model
"#Evidence Paper_evidence ?Paper
          Person_evidence ?Text
          Curator_confirmed ?Text
          Inferred_automatically ?Text
          RNAi_evidence ?RNAi
          Date_last_updated UNIQUE DateType"}

   {:db/id                 (tempid :db.part/user)
    :pacemodel/name        "Paper"
    :pacemodel/model
"?Paper Author ?Text
       Reference       Title UNIQUE ?Text
                       Journal UNIQUE ?Text
                       Volume UNIQUE Text
                       Page  UNIQUE  Text 
       Brief_citation UNIQUE Text
       Abstract ?LongText
       Refers_to Gene ?Gene XREF Reference
                 RNAi ?RNAi XREF Reference"}

   
   {:db/id                 (tempid :db.part/user)
    :pacemodel/name        "Phenotype"
    :pacemodel/model
"?Phenotype Description UNIQUE ?Text
           Name  Primary_name UNIQUE ?Phenotype_name XREF Primary_name_for
           Attribute_of RNAi ?RNAi XREF Phenotype
                        Not_in_RNAi ?RNAi XREF Phenotype_not_observed"}

   {:db/id                 (tempid :db.part/user)
    :pacemodel/name        "RNAi"
    :pacemodel/model
"?RNAi   Evidence #Evidence
        Experiment Delivered_by UNIQUE Bacterial_feeding
                                       Injection
                                       Soaking
                                       Transgene_expression
                   Strain ?Text
        Inhibits Gene ?Gene XREF RNAi_result #Evidence
        Phenotype ?Phenotype XREF RNAi
        Phenotype_not_observed ?Phenotype XREF Not_in_RNAi
        Reference ?Paper XREF RNAi"}



])


