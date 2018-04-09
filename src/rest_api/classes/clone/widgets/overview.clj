(ns rest-api.classes.clone.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.expr-pattern.core :as expr-pattern]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn canonical-for [clone] ; example C05B2
  {:data (some->> (:clone/canonical-for clone)
                  (map :clone.canonical-for/clone)
                  (map (fn [c]
                         (let [obj (pack-obj c)]
                         {(:label obj) obj})))
                  (into {})
                  (into (sorted-map))
                  (not-empty))
   :description "clones that the requested clone is a canonical representative of"})

(defn maps [clone] ; example B0272 and C05B2
  {:data (some->> (or
                    (:clone/map clone)
                    (:contig/map (:clone.pmap/contig (:clone/pmap clone))))
                  (map (fn [h]
                         (or (:clone.map/map h)
                             (:contig.map/map h))))
                  (map (fn [m]
                         {(:map/id m) (:label (pack-obj m))}))
                  (into {})
                  (into (sorted-map))
                  (not-empty))
   :description "maps assigned to this clone"})

(defn sequence-status [clone]
  {:data (not-empty
           (merge
             (if (contains? clone :clone/finished)
               {:Finished (date/format-date6 (:clone/finished clone))})
             (if (contains? clone :clone/shotgun) ; found with DL2
               {:Shotgun nil})
             (when-let [accession (:clone/accession-number clone)]
               {:Accession_number accession})))
   :description "sequencing status of clone"})

(defn canonical-parent [clone] ; e.g. C35D1
  {:data (some->> (:clone.canonical-for/_clone clone)
                  (map :clone/_canonical-for)
                  (map pack-obj)
                  (not-empty))
   :description "canonical parent for clone"})

(defn screened-negative [clone]
  {:data (not-empty
           (into
             (sorted-map)
             (merge
               (some->> (:clone/negative-gene clone)
                        (map :clone.negative-gene/gene)
                        (map (fn [g]
                               {(:gene/id g)
                                (merge
                                  {:weak nil}
                                  (pack-obj g))}))
                        (into {})
                        (not-empty))
               (some->> (:clone/outside-rearr clone)
                        (map :clone.outside-rearr/rearrangement)
                        (map (fn [r]
                               {:weak nil}
                                (pack-obj r)))
                        (into {})
                        (not-empty)))))
   :d (:db/id clone)
   :description "entities shown to NOT be contained within the requested clone"})

(defn url [clone]
  {:data (first (:clone/url clone))
   :description "The website for this clone"})

(defn remarks [clone]
  {:data (some->> (:clone/general-remark clone)
                  (map (fn [r]
                         {:text r})))
   :description "Remarks"})

(defn lengths [clone]
  {:data (not-empty
           (pace-utils/vmap
             :Seq_length
             (:clone/seq-length clone)

             :Gel_length
             (:clone/gel-length clone)))
   :description "lengths relevant to this clone"})

(defn sequences [clone]; example AB070577
  {:data (some->> (:sequence/_clone clone)
                  (map pack-obj))
   :description "sequences associated with this clone"})

(defn in-strain [clone]
  {:data (some->> (:clone/in-strain clone)
                  (first)
                  (pack-obj))
   :description "The current clone is found in this strain"})

(defn pcr-product [clone]
  {:data (some->> (:pcr-product/_clone clone)
                  (map
                    (fn [pcr]
                      {:pcr_product (pack-obj pcr)
                       :oligos (when-let [oligos (some->> (:pcr-product/oligo pcr)
                                       (map :pcr-product.oligo/oligo)
                                       (map (fn [oligo]
                                              {:obj (pack-obj oligo)
                                               :sequence (:oligo/sequence oligo)})))]
                                 {:data oligos})}))
                  (remove nil?)
                  (first))
   :description "PCR product associated with this clone"})

(defn type-field [clone]
  {:data (when-let [t (:clone/type clone)]
           (let [n (name (:clone.type/value t))]
             (case n
               "cdna" "cDNA"
               "yac" "YAK"
               (str/capitalize n))))
   :description "The type of this clone"})

(defn gridded-on [clone]
  {:data (some->> (conj
		    (some->> (:clone.hybridizes-to/_clone clone)
			     (map :clone.hybridizes-to/grid))
		    (some->> (:clone/hybridizes-to clone)
			     (map :clone.hybridizes-to/grid))
		    (some->> (:clone.hybridizes-weak/_clone clone)
			     (map :clone.hybridizes-weak/grid))
		    (some->> (:clone/hybridizes-weak clone)
			     (map :clone.hybridizes-weak/grid)))
                  (flatten)
                  (distinct)
                  (remove nil?)
		  (map
		    (fn [g]
		      {(:grid/id g)
		       (pack-obj g)}))
		  (into {}))
   :description "grid this clone was gridded on during fingerprinting"})

(defn screened-positive [clone]
  {:data (not-empty
	   (into
	     (sorted-map)
	     (merge
	       (some->> (conj
			  (some->> (:clone.hybridizes-to/_clone clone)
				   (map :clone/_hybridizes-to))
			  (some->> (:clone/hybridizes-to clone)
				   (map :clone.hybridizes-to/clone)))
			(flatten)
			(remove nil?)
			(map
			  (fn [c]
			    {(:clone/id c)
			     (merge
			       {:weak nil}
			       (pack-obj c))}))
			(into {})
			(not-empty))
	       (some->> (conj
			  (some->> (:clone.hybridizes-weak/_clone clone)
				   (map :clone/_hybridizes-weak))
			  (some->> (:clone/hybridizes-weak clone)
				   (map :clone.hybridizes-weak/clone)))
			(flatten)
			(remove nil?)
			(map
			  (fn [c]
			    {(:clone/id c)
			     (merge
			       {:weak 1
				}
			       (pack-obj c))}))
			(into {})
			(not-empty))
	       (some->> (:clone/positive-gene clone)
			(map :clone.positive-gene/gene)
			(map
			  (fn [g]
			    {(:gene/id g)
			     (merge
			       {:weak nil}
			       (pack-obj g))}))
			(into {})
			(not-empty)))))
   :d (:db/id clone)
   :description "entities shown to be contained within this clone"})

(defn expression-patterns [clone]
  {:data (some->> (:expr-pattern/_clone clone)
                  (map (fn [ep]
                  (expr-pattern/pack ep nil))))
   :description (str "expression patterns associated with the Clone: " (:clone/id clone))})

(def widget
  {:canonical_for canonical-for
   :maps maps
   :sequence_status sequence-status
   :canonical_parent canonical-parent
   :screened_negative screened-negative
   :url url
   :remarks remarks
   :lengths lengths
   :sequences sequences
   :in_strain in-strain
   :pcr_product pcr-product
   :gridded_on gridded-on
   :name generic/name-field
   :taxonomy generic/taxonomy
   :genomic_position generic/genomic-position
   :type type-field
   :screened_positive screened-positive
   :expression_patterns expression-patterns})
