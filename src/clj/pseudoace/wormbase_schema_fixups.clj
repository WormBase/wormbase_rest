(ns pseudoace.wormbase-schema-fixups)

(def schema-fixups
  [

   ;;
   ;; Classes which have "locatable" attributes at top-level.
   ;;

   {:db/id          :gene/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :variation/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :feature/id
    :pace/use-ns    ["locatable"]}
   
   {:db/id          :cds/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :transcript/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :pseudogene/id
    :pace/use-ns    ["locatable"]}
   
   {:db/id          :transposon/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :pcr-product/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :operon/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :oligo-set/id
    :pace/use-ns    ["locatable"]}

   {:db/id          :expr-profile/id
    :pace/use-ns    ["locatable"]}

   ;;
   ;; XREFs inside hash models
   ;; (see https://github.com/WormBase/db/wiki/Ace-to-Datomic-mapping#xrefs-in-hash-models)
   ;;

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :multi-counts.gene/gene
    :pace.xref/obj-ref   :multi-pt-data/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :multi-counts.allele/variation
    :pace.xref/obj-ref   :multi-pt-data/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :multi-counts.locus/locus
    :pace.xref/obj-ref   :multi-pt-data/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :multi-counts.transgene/transgene
    :pace.xref/obj-ref   :multi-pt-data/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :multi-counts.rearrangement/rearrangement
    :pace.xref/obj-ref   :multi-pt-data/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :mass-spec-data/protein
    :pace.xref/obj-ref   :mass-spec-peptide/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :interactor-info/transgene
    :pace.xref/obj-ref   :interaction/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :interactor-info/construct
    :pace.xref/obj-ref   :interaction/id}

   {:db/id               #db/id[:db.part/user]
    :pace.xref/attribute :interactor-info/antibody
    :pace.xref/obj-ref   :interaction/id}

   ;;
   ;; Timestamp
   ;;
   
   {:db/id          #db/id[:db.part/tx]
    :db/txInstant   #inst "1970-01-01T00:00:01"}

   ])
