(ns pseudoace.locatable-schema
  (:use datomic-schema.schema))

(def locatable-schema
 (concat
  (generate-schema [
   (schema locatable
     (fields

       ;;
       ;; core attributes, used for all features
       ;;

       [parent :ref
          "An entity (e.g. sequence or protein) which defines the coordinate system for this locatable."]
       [min :long :indexed
          "The lower bound of a half-open (UCSC-style) interval defining the location."]
       [max :long :indexed
          "The upper bound of a half-open (UCSC-style) interval defining the location."]
       [strand :enum [:positive :negative]
          "Token designating the strand or orientation of this feature.  Omit if unknown or irrelevant."]
       [method :ref
          "Method entity defining the meaning of this feature.  Required for lightweight features."]

       ;;
       ;; Attributes from ?Feature_data and #Feature_info -- used for lightweight features
       ;;

       [score :float
          "Feature score, as used in ?Feature_data."]
       [note :string :many
          "Human-readable note associated with a lightweight feature."]

       ;;
       ;; Binning system
       ;;

       [murmur-bin :long :indexed
          "Bottom 20 bits contain a UCSC/BAM-style bin number.  High bits contain a Murmur3 hash code
           for the parent sequence.  Only used for locatables attached to a parent with a :sequence/id."]

       ;;
       ;; Assembly support
       ;;

       [assembly-parent :ref
          "The parent sequence in a genome assembly."]
       ))

   (schema splice-confirm
    (fields
     [cdna :ref
        "cdna entity which supports this intron."]
     [est :ref
        "sequence entity of an EST which supports this intron."]
     [ost :ref
        "sequence entity of an OST which supports this intron."]
     [rst :ref
        "sequence entity of an RST which supports this intron."]
     [mrna :ref
        "sequence entity of an mRNA which supports this intron."]
     [utr :ref
        "sequence entity of a UTR which supports this intron."]
     [rnaseq :ref :component
        "Details of RNA-seq data supporting this intron (uses splice-confirm.rna namespace)."]
     [mass-spec :ref
        "mass-spec-peptide entity which supports this intron."]
     [homology :string
        "accession number of an external database record which supports this intron (is this used?)."]
     [false-splice :ref
        "sequence entity providing evidence for a false splice site call."]
     [inconsistent :ref
        "sequence entity providing evidence for an inconsistent splice site call."]))

   (schema splice-confirm.rnaseq
    (fields
     [analysis :ref
        "Analysis entity describing the RNA-seq dataset."]
     [count :long
        "Number of reads supporting the intron."]))

   (schema homology
    (fields
     ;;
     ;; The target of this homology.  Only one is allowed per homology entity.
     ;;
     [dna :ref
        "Sequence entity representing the target of a DNA homology."]

     [protein :ref
        "Protein entity representing the target of a peptide homology."]

     [motif :ref
        "A motif entity which is mapped to a sequence by this homology."]

     [rnai :ref
        "An RNAi entity which is mapped to a sequence by this homology."]

     [oligo-set :ref
        "An oligo-set which is mapped to a sequence by this homology."]

     [structure :ref
        "Structure-data which is mapped to a sequence by this homology."]

     [expr :ref
        "Expression-pattern which is mapped to a sequence by this homology."]

     [ms-peptide :ref
        "Mass-spec-peptide which is mapped to a sequence by this homology."]

     [sage :ref
        "SAGE-tag which is mapped to a sequence by this homology."]

     ;;
     ;; Parent sequence, parent location, and method are specified using "locatable".
     ;;

     [min :long :indexed
        "Lower bound of a half-open interval defining the extent of this homology 
         in the target's coordinate system."]
     [max :long :indexed
        "Upper bound of a half-open interval defining the extent of this homology 
         in the target's coordinate system."]
     [strand :enum [:positive :negative]
          "Token designating the strand or orientation of this homology on the 
           target's coordinate system. Should only be used in situations where 
           a negative-to-negative alignment would be meaningful (e.g. tblastx)"]
     [gap :string
          "Gapped alignment.  The locations of matches and gaps are encoded 
           in a CIGAR-like format as defined in 
           http://www.sequenceontology.org/gff3.shtml"]

     ;; 
     ;; Parity with legacy #Homol_info -- are these needed in the long run?
     ;;

     [target-species :ref
         "Link to target species of alignment."]

     [align-id :string
         "Alignment ID to emit in GFF dumps."]))

    ])

  ;;
  ;; Fake pseudoace metadata for locatable-model enums so that
  ;; they are visible in TrACeView and Colonnade
  ;;
  
  [{:db/id          #db/id[:db.part/user]
    :db/ident       :locatable.strand/positive
    :pace/tags      "Positive"}

   {:db/id          #db/id[:db.part/user]
    :db/ident       :locatable.strand/negative
    :pace/tags      "Negative"}

   {:db/id          #db/id[:db.part/user]
    :db/ident       :homology.strand/positive
    :pace/tags      "Positive"}

   {:db/id          #db/id[:db.part/user]
    :db/ident       :homology.strand/negative
    :pace/tags      "Negative"}]
  
  [{:db/id          #db/id[:db.part/tx]
    :db/txInstant   #inst "1970-01-01T00:00:01"}]))

