(ns pseudoace.metadata-schema
  (:use datomic-schema.schema)
  (:require [datomic.api :refer (tempid)]))

(def metaschema
  [{:db/id           (tempid :db.part/db)
    :db/ident        :pace/identifies-class
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db/unique       :db.unique/identity
    :db/doc          "Attribute of object-identifers (e.g. :gene/id), indicating the name of the corresponding ACeDB class."
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :pace/is-hash
    :db/valueType    :db.type/boolean
    :db/cardinality  :db.cardinality/one
    :db/doc          "Marks an object-identifier as identifying a hash-model."
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :pace/prefer-part
    :db/valueType    :db.type/ref
    :db/cardinality  :db.cardinality/one
    :db/doc          "Attribute of object-identifiers indicating a preferred Datomic partition for storing entities of this type."
    :db.install/_attribute :db.part/db}
   
   {:db/id           (tempid :db.part/db)
    :db/ident        :pace/tags
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db/doc          "Space-separated sequence of tag names from the ACeDB model."
    :db.install/_attribute :db.part/db}
   
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/obj-ref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db/doc           "The object-identifier for the type of object referenced by this attribute."
   :db.install/_attribute :db.part/db}

  ; Used to impose an ordering on Datomic components which
  ; map to complex ACeDB internal tags
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/order
   :db/valueType     :db.type/long
   :db/cardinality   :db.cardinality/one
   :db/doc           "The order of positional parameters within a component."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/use-ns
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/many
   :db/doc           "For a component attribute, specifies that the component entity may contain attributes from the specied namespace (e.g. \"evidence\")."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/fill-default
   :db/valueType     :db.type/boolean
   :db/cardinality   :db.cardinality/one
   :db/doc           "Hint that the importer should supply a default value if none is specified in ACeDB."
   :db.install/_attribute :db.part/db}
  
  {:db/id            (tempid :db.part/db)
   :db/ident         :pace/xref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/many
   :db/isComponent   true
   :db/doc           "Information about XREFs to this attribute from other classes."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/tags
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/one
   :db/doc           "The XREF's tag-path within this class."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/attribute
   :db/unique        :db.unique/identity       ;; Mainly to ensure we don't get duplicates
                                               ;; if we transact this multiple times.
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db/doc           "The attribute from the foreign class corresponding to this XREF."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/obj-ref
   :db/valueType     :db.type/ref
   :db/cardinality   :db.cardinality/one
   :db/doc           "Identity attribute for the object at the outbound end of the XREF."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/db)
   :db/ident         :pace.xref/use-ns
   :db/valueType     :db.type/string
   :db/cardinality   :db.cardinality/many
   :db/doc           "For 'complex' XREFs, a set of namespaces for additional data which should be visible on the inbound end."
   :db.install/_attribute :db.part/db}

  {:db/id            (tempid :db.part/tx)
   :db/txInstant     #inst "1970-01-01T00:00:01"}

])

(def basetypes
  [{:db/id           (tempid :db.part/db)
    :db/ident        :longtext/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db/doc          "Built-in ?LongText class."
    :db.install/_attribute :db.part/db
    :pace/identifies-class "LongText"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :longtext/text
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db/fulltext     true
    :db/doc          "The text associated with this object.  A full-text index will be built."
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :dna/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db/doc          "Built-in ?DNA class."
    :db.install/_attribute :db.part/db
    :pace/identifies-class "DNA"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :dna/sequence
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db/doc          "The sequence of this DNA."
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :peptide/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db/doc          "Built-in ?Peptide type."
    :db.install/_attribute :db.part/db
    :pace/identifies-class "Peptide"}

   {:db/id           #db/id[:db.part/db]
    :db/ident        :peptide/sequence
    :db/cardinality  :db.cardinality/one
    :db/valueType    :db.type/string
    :db/doc          "The sequence of this protein/peptide."
    :db.install/_attribute :db.part/db}
   
   {:db/id           (tempid :db.part/db)
    :db/ident        :keyword/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :db/doc          "Built-in ?Keyword type."
    :pace/identifies-class "Keyword"}

   ;;
   ;; Importer support
   ;;

   {:db/id           (tempid :db.part/db)
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db/unique       :db.unique/identity
    :db/ident        :importer/temp
    :db/doc          "Identifier used as scaffolding by the timestamp-aware importer.  Should generally be excised after import is complete."
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/valueType    :db.type/string
    :db/cardinality  :db.cardinality/one
    :db/ident        :importer/ts-name
    :db/doc          "Username from a legacy timestamp."
    :db.install/_attribute :db.part/db}
   
   ;;
   ;; Special #Ordered virtual hash-model
   ;;
   
   {:db/id           (tempid :db.part/db)
    :db/ident        :ordered/id
    :db/valueType    :db.type/string
    :db/unique       :db.unique/identity
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db
    :pace/identifies-class "Ordered"
    :pace/is-hash    true}

   {:db/id           (tempid :db.part/db)
    :db/ident        :ordered/index
    :db/valueType    :db.type/long
    :db/cardinality  :db.cardinality/one
    :db/doc          "Index in an ordered collection."
    :db.install/_attribute :db.part/db}   ;; no :pace/tags since we'd never want these to appear in ACeDB-
                                          ;; style output.


   ;;
   ;; Position_Matrix data
   ;;

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix/background
    :db/valueType    :db.type/ref
    :db/isComponent  true
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix/values
    :db/valueType    :db.type/ref
    :db/isComponent  true
    :db/cardinality  :db.cardinality/many
    :pace/use-ns     #{"ordered"}
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix.value/a
    :db/valueType    :db.type/float
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix.value/c
    :db/valueType    :db.type/float
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix.value/g
    :db/valueType    :db.type/float
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id           (tempid :db.part/db)
    :db/ident        :position-matrix.value/t
    :db/valueType    :db.type/float
    :db/cardinality  :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id            (tempid :db.part/tx)
    :db/txInstant     #inst "1970-01-01T00:00:01"}

   ])
