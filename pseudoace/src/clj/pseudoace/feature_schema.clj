(ns pseudoace.feature-schema
  (:use datomic-schema.schema)
  (:require [datomic.api :refer (tempid)]))

(def feature-schema
  (generate-schema tempid [
   (schema fdata
    (fields
     [sequence :ref]        
     [species :ref]
     [strain :ref]
     [method :ref]
     [start :long]
     [end :long]
     [score :float]
     [note :string]
     [embl-qualifier :string :many]))

   (schema splice-confirm
    (fields
     [intron-start :long]
     [intron-end :long]
     [cdna :ref]
     [est :ref]
     [ost :ref]
     [rst :ref]
     [rnaseq :ref :component]
     [mass-spec :ref]
     [mrna :ref]
     [homology :string]
     [utr :ref]
     [false-splice :ref]
     [inconsistent :ref]))

   (schema splice-confirm.rnaseq
    (fields
     [analysis :ref]
     [count :long]))

   (schema splice
    (fields
     [site :enum [:five-prime
                  :three-prime]]
     [score :float]))

   (schema homol
    (fields
     [dna :ref]
     [peptide :ref]
     [motif :ref]
     [homol :ref]
     [rnai :ref]
     [oligo-set :ref]
     [structure :ref]
     [expr :ref]
     [ms-peptide :ref]
     [sage :ref]

     [start :long]
     [end :long]

     [cigar :string]
     [id :string]
     [target-species :ref]))]))
