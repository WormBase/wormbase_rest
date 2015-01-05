(ns pseudoace.metadata-schema
  (:use datomic-schema.schema)
  (:require [datomic.api :refer (tempid)]))

(def metaschema
  [{:db/id           (tempid :db.part/db)
    :db/ident        :pace/identifies-class
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db/unique       :db.unique/identity
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :pace/is-hash
    :db/valueType    :db.type/boolean
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id           (tempid :db.part/db)
    :db/ident        :pace/tags
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/obj-ref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  ; Used to impose an ordering on Datomic components which
  ; map to complex ACeDB internal tags
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/order
   :db/valueType     :db.type/long
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/use-ns
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/many
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/fill-default
   :db/valueType     :db.type/boolean
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}
  
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/xref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/many
   :db/isComponent   true
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/tags
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/attribute
   :db/unique        :db.unique/identity       ;; Mainly to ensure we don't get duplicates
                                               ;; if we transact this multiple times.
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/obj-ref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  ;;
  ;; Short-term solution to make the "model" command work...
  ;;
  
  {:db/id            (tempid :db.part/db)
   :db/ident         :pacemodel/name
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pacemodel/model
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/one
   :db.install/_attribute :db.part/db}

])

(def basetypes
  [{:db/id           (tempid :db.part/db)
    :db/ident        :longtext/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :pace/identifies-class "LongText"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :longtext/text
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db/fulltext     true
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :dna/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :pace/identifies-class "DNA"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :dna/sequence
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :peptide/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :pace/identifies-class "Peptide"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :peptide/sequence
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db.install/_attribute :db.part/db}
   
   {:db/id           (tempid :db.part/db)
    :db/ident        :keyword/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :pace/identifies-class "Keyword"}])
