(ns pseudoace.wormbase-schema-fixups)

(def schema-fixups
  [{:db/id          :gene/id
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

   {:db/id          #db/id[:db.part/tx]
    :db/txInstant   #inst "1970-01-01T00:00:01"}])
