(ns wb.schema1
  (:use datomic-schema.schema)
  (:require [datomic.api :as d]))

(def wb1
  (concat
   (generate-schema d/tempid
                    
 [(schema longtext
    (fields
     [:id :string :unique-identity]
     [:text :string :fulltext]))
  
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

  (schema gene
    (fields
     [:id :string :unique-identity]
     [:name.cgc :string]
     [:name.sequence :string]
     [:name.public :string]
     [:desc :ref :component]
     [:reference :ref :many]
     [:rnai :ref :many :component]))
  (schema gene.desc
    (fields
     ; Also has evidence
     [:concise :string :fulltext]))
  (schema gene.rnai
    (fields
     ; Also has evidence
     [:rnai :ref]))

  (schema evidence
    (fields
     [:paper :ref :many]
     [:person :string :many]
     [:curator :string :many]
     [:automatic :string :many]
     [:rnai :ref :many]))

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

    
  
  
     
        
                                      
                              
                              
