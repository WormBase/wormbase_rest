(ns rest-api.classes.generic-fields
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.paper.core :as generic-paper]
    [clojure.string :as str]))

(defn name-field [object]
  (obj/name-field object))

(defn gene-product [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (when-let [ghs ((keyword role "gene") object)]
             (let [gp-holder-gene-kw (keyword (str role ".gene") "gene")]
               (for [gh ghs
                     :let [gene (gp-holder-gene-kw gh)]]
                 (pack-obj gene))))
     :description (str "gene products for this " role)}))

(defn genetic-position [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        str-imp "interpolated-map-position"
        variation-with-imp (and (= "variation" role)
                                (not
                                  (contains?
                                    object
                                    :variation/interpolated-map-position)))
        [entities entity-role] (cond
                                 variation-with-imp
                                 [[(:variation.gene/gene
                                     (first
                                       (:variation/gene object)))]
                                  "gene"]

                                 (= role "protein")
                                 [(some->> (:cds.corresponding-protein/_protein object)
                                           (map :cds/_corresponding-protein)
                                           (filter #(not= "history" (:method/id (:locatable/method %))))
                                           (map :gene.corresponding-cds/_cds)
                                           first
                                           (map :gene/_corresponding-cds))
                                  "gene"]

                                 :else
                                 [[object] role])]
    {:data
     (not-empty
       (for [entity entities
             :let [[chr position error method]
                   (let [kw-imp (keyword entity-role str-imp)
                         str-imp-component (str entity-role "." str-imp)
                         kw-map (keyword entity-role "map")
                         kw-map-map (keyword (str entity-role ".map") "map")
                         kw-imp-map (keyword str-imp-component "map")
                         kw-imp-position (keyword
                                           str-imp-component
                                           (if (= role "sequence") "float" "position"))]
                     (if
                       (or (= "sequence" entity-role)
                           (= "variation" entity-role)
                           (not (contains? entity kw-map)))
                       [(:map/id (kw-imp-map (kw-imp entity)))
                        (kw-imp-position (kw-imp entity))
                        nil
                        "interpolated"]
                       (let [map-position (:map-position/position (kw-map entity))]
                         [(:map/id (kw-map-map (kw-map entity)))
                          (:map-position.position/float map-position)
                          (:map-error/error map-position)
                          (if (= role entity-role) "" "interpolated")])))]]
         {:chromosome chr
          :position position
          :error error
          :formatted (when (not-any? nil? [chr position])
                       (format "%s:%2.2f +/- %2.3f cM" chr position (or error (double 0))))
          :method method}))
     :description (str "Genetic position of " role ": " (id-kw object))}))

(defn fusion-reporter [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (when-let [t ((keyword role "fusion-reporter") object)] (first t))
     :description (str "reporter construct for this " role)}))

(defn driven-by-gene [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (when-let [gene ((keyword (str role ".driven-by-gene") "gene")
                            (first ((keyword role "driven-by-gene") object)))]
             (pack-obj gene))
     :description "gene that drives the construct"}))

(defn genomic-position [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
	role (namespace id-kw)]
    {:data (if (= role "protein")
	     (some->>  (:cds.corresponding-protein/_protein object)
		      (map :cds/_corresponding-protein)
		      (filter #(not= "history"  (:method/id  (:locatable/method %))))
		      (map :gene.corresponding-cds/_cds)
		      first
		      (map :gene/_corresponding-cds)
		      (map sequence-fns/genomic-obj))
	     (when-let [position (sequence-fns/genomic-obj object)]
	       [position]))
     :description "The genomic location of the sequence"}))

(defn method [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (:method/id (:locatable/method object))
     :description (str "the method used to describe the" role)}))

(defn identity-field [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (if-let [ident ((keyword role "brief-identification") object)]
             {:text (or (:cds.brief-identification/text ident)
                        ident)
              :evidence (obj/get-evidence ident)})
     :description (str "Brief description of the WormBase " role)}))

(defn historical-gene [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (if-let [ghs ((keyword role "historical-gene") object)]
             (for [gh ghs]
               {:text (pack-obj ((keyword (str role ".historical-gene") "gene") gh))
                :evidence (when-let [text ((keyword (str role ".historical-gene") "text") gh)]
                            {text ""})}))
     :description (str "Historical record of the dead genes originally associated with this " role)}))

(defn summary [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data ((keyword (str role ".summary") "text") ((keyword role "summary") object))
     :description (str "A brief summary of the " role ": " (id-kw object))}))

(defn available-from [object]
  {:data (when (= "Vancouver_fosmid"
                  (:method/id (:locatable/method object)))
           {:label "GeneService"
            :class "Geneservice_fosmids"})
   :description "availability of clones of the sequence"})

(defn status [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (if-let [sh ((keyword role "status") object)]
             (:status/status sh))
     :description (str "current status of the " (str/capitalize role) ": "
                       (id-kw object) "if not Live or Valid")}))

(defn other-names [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        text-kw (keyword (str role ".other-name") "text")]
    {:data (when-let [other-names ((keyword role "other-name") object)]
             (for [other-name other-names]
               (if (string? other-name)
                 other-name
                 (if (contains? other-name text-kw)
                   (text-kw other-name)
                   other-name))))
     :description (str "other names that have been used to refer to " (id-kw object))}))

(defn laboratory [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (when-let [labs (or ((keyword role "laboratory") object)
                               (or ((keyword role "location") object)
                                   [((keyword role "from-laboratory") object)]))]
             (not-empty
               (remove
                 nil?
                 (for [l labs
                       :let [kw-loc (keyword (str role ".location") "laboratory")
                             kw-lab (keyword (str role ".laboratory") "laboratory")
                             lab (cond
                                   (contains? l kw-lab)
                                   (kw-lab l)

                                   (contains? l kw-loc)
                                   (kw-loc l)

                                   :else
                                   l)]]
                   (when-let [laboratory (pack-obj lab)]
                     {:laboratory laboratory
                      :representative (when-let [reps (:laboratory/representative lab)]
                                        (for [rep reps] (pack-obj rep)))})))))
     :description (str "the laboratory where the " role " was isolated, created, or named")}))

(defn description [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (first ((keyword role "description") object))
     :description (str "description of the " role " " ((keyword role "id") object))}))

(defn taxonomy [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)]
    {:data (if-let [species (:species/id ((keyword role "species") object))]
             (let [[genus species] (str/split species #" ")]
               {:genus genus
                :species species}))
     :description "the genus and species of the current object"}))

(defn remarks [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        data (when (some? id-kw)
               (let  [remark-kw (keyword role "remark")
                      db-remark-kw (keyword role "db-remark")]
                 (when-let [remark-holders (concat (remark-kw object) (db-remark-kw object))]
                   (let [remark-text-kw (keyword (str role ".remark") "text")
                         db-remark-text-kw (keyword (str role ".db-remark") "text")]
                     (for [remark-holder remark-holders]
                       {:text (or (remark-text-kw remark-holder)
                                  (db-remark-text-kw remark-holder))
                        :evidence (obj/get-evidence remark-holder)})))))]
    {:data (not-empty data)
     :description (if (some? id-kw)
                    (str "Curatorial remarks for the " role)
                    "Can not determine class for entity and can not determine remarks")}))

(defn xrefs [object]
  (let [data
        (if-let [id-kw (first (filter #(= (name %) "id") (keys object)))]
          (let [role (namespace id-kw)
                kw-db-role (keyword role "database")
                ckw (str role ".database")]
            (reduce
              (fn [refs database]
                (let [match-accession (partial re-matches #"(?:OMIM:|GI:)(.*)")
                      kw-field (keyword ckw "field")
                      kw-database-field (keyword ckw "database-field")
                      kw-accession (keyword ckw "accession")
                      kw-text (keyword ckw "text")
                      kw-db-id (keyword ckw "database")
                      kw-field-id (if (contains? database kw-database-field)
                                    kw-database-field
                                    kw-field)
                      kw-db-acc (if (contains? database kw-text)
                                  kw-text
                                  kw-accession)]
                  (update-in refs
                             [(:database/id (kw-db-id database))
                              (:database-field/id (kw-field-id database))
                              :ids]
                             pace-utils/conjv
                             (let [acc (kw-db-acc database)]
                               (if-not (nil? acc)
                                 (if-let [[_ rest] (match-accession acc)]
                                   rest
                                   acc))))))
              {}
              (kw-db-role object))))]
    {:data (not-empty data)
     :description  (str "external databases and IDs containing "
                        "additional information on the object")}))

(defn references [object]
  (let [id-kw (first (filter #(= (name %) "id") (keys object)))
        role (namespace id-kw)
        data (when (some? id-kw)
               (let [reference-kw (keyword role "reference")]
                 (when-let [papers (reference-kw object)]
                   (let [number-of-papers (count papers)
                         kw-reference-paper (keyword (str role ".reference") "paper")]
                     {:count number-of-papers
                      :results (for [ph (if (or (kw-reference-paper papers)
                                                (:paper/id papers)) [papers] papers)
                                     :let [paper (if (contains? ph kw-reference-paper)
                                                   (kw-reference-paper ph)
                                                   ph)]]
                                 (let [abstract (:paper/abstract paper)
                                       publication-date (:paper/publication-date paper)
                                       pt (:paper/type paper)
                                       author-holder (:paper/author paper)
                                       year (if (nil? publication-date) nil (first (str/split publication-date #"-")))]
                                   {:page (:paper/page paper)
                                    :volume (:paper/volume paper)
                                    :name  (pack-obj paper)
                                    :title  [(:paper/title paper)]
                                    :author (generic-paper/get-authors paper)
                                    :ptype (->> (map :paper.type/type pt)
                                                (map obj/humanize-ident)
                                                (first))
                                    :abstract (when abstract [(:longtext/text (first abstract))])
                                    :year year
                                    :journal [(:paper/journal paper)]}))}))))]
    {:data (not-empty data)
     :description (if (some? id-kw)
                    (str "Reference papers for this " role)
                    "Could not identify the identity of the object")}))
