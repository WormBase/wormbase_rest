(ns rest-api.perf.endpoint-discovery
  "Discovers and enumerates all API endpoints for testing."
  (:require
   [rest-api.classes.analysis :as analysis]
   [rest-api.classes.anatomy-term :as anatomy-term]
   [rest-api.classes.antibody :as antibody]
   [rest-api.classes.cds :as cds]
   [rest-api.classes.clone :as clone]
   [rest-api.classes.construct :as construct]
   [rest-api.classes.do-term :as do-term]
   [rest-api.classes.expr-pattern :as expr-pattern]
   [rest-api.classes.expr-profile :as expr-profile]
   [rest-api.classes.expression-cluster :as expression-cluster]
   [rest-api.classes.feature :as feature]
   [rest-api.classes.gene :as gene]
   [rest-api.classes.gene-class :as gene-class]
   [rest-api.classes.gene-cluster :as gene-cluster]
   [rest-api.classes.genotype :as genotype]
   [rest-api.classes.go-term :as go-term]
   [rest-api.classes.homology-group :as homology-group]
   [rest-api.classes.interaction :as interaction]
   [rest-api.classes.laboratory :as laboratory]
   [rest-api.classes.life-stage :as life-stage]
   [rest-api.classes.microarray-results :as microarray-results]
   [rest-api.classes.molecule :as molecule]
   [rest-api.classes.motif :as motif]
   [rest-api.classes.operon :as operon]
   [rest-api.classes.paper :as paper]
   [rest-api.classes.pcr-oligo :as pcr-oligo]
   [rest-api.classes.person :as person]
   [rest-api.classes.phenotype :as phenotype]
   [rest-api.classes.picture :as picture]
   [rest-api.classes.position-matrix :as position-matrix]
   [rest-api.classes.protein :as protein]
   [rest-api.classes.pseudogene :as pseudogene]
   [rest-api.classes.rearrangement :as rearrangement]
   [rest-api.classes.rnai :as rnai]
   [rest-api.classes.sequence :as seqs]
   [rest-api.classes.strain :as strain]
   [rest-api.classes.structure-data :as structure-data]
   [rest-api.classes.transcript :as transcript]
   [rest-api.classes.transgene :as transgene]
   [rest-api.classes.transposon :as transposon]
   [rest-api.classes.transposon-family :as transposon-family]
   [rest-api.classes.variation :as variation]
   [rest-api.classes.wbprocess :as wbprocess]
   ;; Widget namespaces for gene (most complex entity)
   [rest-api.classes.gene.widgets.overview :as gene-overview]
   [rest-api.classes.gene.widgets.expression :as gene-expression]
   [rest-api.classes.gene.widgets.external-links :as gene-external-links]
   [rest-api.classes.gene.widgets.feature :as gene-feature]
   [rest-api.classes.gene.widgets.genetics :as gene-genetics]
   [rest-api.classes.gene.widgets.history :as gene-history]
   [rest-api.classes.gene.widgets.homology :as gene-homology]
   [rest-api.classes.gene.widgets.human-diseases :as gene-human-diseases]
   [rest-api.classes.gene.widgets.location :as gene-location]
   [rest-api.classes.gene.widgets.mapping-data :as gene-mapping-data]
   [rest-api.classes.gene.widgets.ontology :as gene-ontology]
   [rest-api.classes.gene.widgets.phenotype :as gene-phenotype]
   [rest-api.classes.gene.widgets.phenotype-graph :as gene-phenotype-graph]
   [rest-api.classes.gene.widgets.reagents :as gene-reagents]
   [rest-api.classes.gene.widgets.references :as gene-references]
   [rest-api.classes.gene.widgets.sequences :as gene-sequences]
   [rest-api.classes.gene.widgets.biocyc :as gene-biocyc]
   [rest-api.classes.gene.variation :as gene-variation]
   [rest-api.classes.interaction.core :as interaction-core]
   [rest-api.classes.graphview.widget :as graphview]))

(def endpoint-registry
  "Registry of all entity endpoints with their handlers.
   Structure: {entity-ns {:widgets {name handler} :fields {name handler}}}"
  {"gene"
   {:widgets
    {:overview gene-overview/widget
     :expression gene-expression/widget
     :external_links gene-external-links/widget
     :feature gene-feature/widget
     :genetics gene-genetics/widget
     :history gene-history/widget
     :homology gene-homology/widget
     :human_diseases gene-human-diseases/widget
     :location gene-location/widget
     :mapping_data gene-mapping-data/widget
     :gene_ontology gene-ontology/widget
     :phenotype gene-phenotype/widget
     :phenotype_graph gene-phenotype-graph/widget
     :reagents gene-reagents/widget
     :references gene-references/widget
     :sequences gene-sequences/widget
     :biocyc gene-biocyc/widget
     :graphview graphview/widget
     :interactions interaction-core/widget}
    :fields
    {:alleles_other gene-variation/alleles-other
     :polymorphisms gene-variation/polymorphisms
     :interaction_details interaction-core/interaction-details}}

   "variation"
   {:widgets
    {:overview #(require 'rest-api.classes.variation.widgets.overview)
     :genetics #(require 'rest-api.classes.variation.widgets.genetics)
     :human_diseases #(require 'rest-api.classes.variation.widgets.human-diseases)
     :isolation #(require 'rest-api.classes.variation.widgets.isolation)
     :molecular_details #(require 'rest-api.classes.variation.widgets.molecular-details)
     :phenotypes #(require 'rest-api.classes.variation.widgets.phenotypes)
     :location #(require 'rest-api.classes.variation.widgets.location)
     :external_links gene-external-links/widget
     :references gene-references/widget
     :graphview graphview/widget}}

   "protein"
   {:widgets
    {:overview #(require 'rest-api.classes.protein.widgets.overview)
     :location #(require 'rest-api.classes.protein.widgets.location)
     :sequences #(require 'rest-api.classes.protein.widgets.sequences)
     :motif_details #(require 'rest-api.classes.protein.widgets.motif-details)
     :homology #(require 'rest-api.classes.protein.widgets.homology)
     :blast_details #(require 'rest-api.classes.protein.widgets.blast-details)
     :history #(require 'rest-api.classes.protein.widgets.history)
     :external_links gene-external-links/widget
     :graphview graphview/widget}}

   "strain"
   {:widgets
    {:overview #(require 'rest-api.classes.strain.widgets.overview)
     :contains #(require 'rest-api.classes.strain.widgets.contains)
     :human_diseases #(require 'rest-api.classes.strain.widgets.human-diseases)
     :isolation #(require 'rest-api.classes.strain.widgets.isolation)
     :natural_isolates #(require 'rest-api.classes.strain.widgets.natural-isolates)
     :origin #(require 'rest-api.classes.strain.widgets.origin)
     :phenotypes #(require 'rest-api.classes.strain.widgets.phenotypes)
     :references gene-references/widget}}})

(defn discover-endpoints
  "Return list of all endpoint descriptors.
   Each descriptor: {:entity-ns :scheme :endpoint-name :handler}"
  []
  (for [[entity-ns {:keys [widgets fields]}] endpoint-registry
        [scheme endpoints] [[:widget widgets] [:field fields]]
        :when endpoints
        [endpoint-name handler] endpoints]
    {:entity-ns entity-ns
     :scheme scheme
     :endpoint-name (name endpoint-name)
     :handler handler}))

(defn endpoints-for-entity
  "Get all endpoints for a specific entity type."
  [entity-ns]
  (filter #(= entity-ns (:entity-ns %)) (discover-endpoints)))

(defn widget-endpoints
  "Get only widget endpoints."
  []
  (filter #(= :widget (:scheme %)) (discover-endpoints)))

(defn field-endpoints
  "Get only field endpoints."
  []
  (filter #(= :field (:scheme %)) (discover-endpoints)))

(defn endpoint-url
  "Generate URL for an endpoint."
  [entity-ns scheme endpoint-name entity-id]
  (format "/rest/%s/%s/%s/%s"
          (name scheme)
          (clojure.string/replace entity-ns "-" "_")
          entity-id
          endpoint-name))
