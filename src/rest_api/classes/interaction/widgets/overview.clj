(ns rest-api.classes.interaction.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn libraries-screened [i]
  {:data (when-let [lhs (:interaction/library-screened i)]
           (for [lh lhs]
             (:interaction.library-screened/library lh)))
   :description "Libraries screened for the interaction"})

(defn regulation-result [i]
  {:data (when-let [hs (:interaction/regulation-result i)]
           (for [h hs]
          {:type (when-let [t (:interaction.regulation-result/value h)]
                   (str/capitalize (str/replace (name t) #"-" "_")))
           :life_stage (when-let [life-stages (:gr-condition/life-stage h)]
                           (for [life-stage life-stages]
                             (pack-obj life-stage)))
           :subcellular_localization (:gr-condition/subcellular-localization h)
           :anatomy_term (when-let [terms (:gr-condition/anatomy-term h)]
                           (for [term terms]
                             (pack-obj term)))}))
   :description "Regulation results for this interaction"})

(defn interaction-type [interaction]
  {:data (when-let [types (:interaction/type interaction)]
           (let [raw-type-name (-> (first types)  ; all types of an interaction will have the same super type
                                   (name)
                                   (str/split #":")
                                   (first))
                 type-name (if (re-matches #"gi-module-.+" raw-type-name)
                             "genetic"
                             raw-type-name)]
             type-name))
   :description "Supertype of the interaction"})

(defn interaction-subtype [i]
  {:data
   (when-let [types (:interaction/type i)]
     (let [type-map (->> types
                         (map (fn [type]
                                (let [[supertype subtype] (str/split (name type) #":")
                                      subtype-new (case subtype
                                                    "proteindna" "protein-DNA"
                                                    "proteinprotein" "protein-protein"
                                                    "proteinrna" "protein-RNA"
                                                    subtype)]
                                  [supertype subtype-new])))
                         (filter (fn [[supertype subtype]]
                                   ;; type with :genetic legacy
                                   (not= supertype "genetic")))
                         (into {}))]
       (if-let [genetic-modifiers (->> [(get type-map "gi-module-one")
                                        (get type-map "gi-module-two")
                                        (get type-map "gi-module-three")]
                                       (filter identity)
                                       (seq))]
         (str/join " " genetic-modifiers)
         (first (vals type-map)))))
   :description "Subtype of the interaction"})

(defn interaction-phenotype [i]
  {:data (when-let [phenotypes (:interaction/interaction-phenotype i)]
           (for [phenotype phenotypes] (pack-obj phenotype)))
   :description "Phenotype details for the interaction"})

(defn interaction-summary [i]
  {:data (when-let [shs (:interaction/interaction-summary i)]
           (for [sh shs] (:interaction.interaction-summary/text sh)))
   :description
   (str "Interaction data was extracted by a curator from sentences enriched by Textpresso."
        "   The interaction was attributed to the paper(s) from which it was extracted.")})

(defn process [i]
  {:data (when-let [process (:wbprocess/_interaction
                              (first
                                (:wbprocess.interaction/_interaction i)))]
                     (pack-obj process))
   :description "WBProcess for the interaction"})

(defn detection-method [i]
  {:data (when-let [dmethods (:interaction/detection-method i)]
           (for [dmethod dmethods
                 :let [method-kw (:interaction.detection-method/value dmethod)]]
             (let [method-str (str/replace
                                (str/replace
                                  (str/capitalize
                                    (str/replace (name method-kw) #"-" " "))
                                  #"rna" "RNA")
                                #"dna" "DNA")]
               (if-let [text (:interaction.detection-method/text dmethod)]
                  (str/join ": " [method-str text])
                  method-str))))
   :description "Method(s) by which the interaction was detected"})

(defn regulation-level [i]
  {:data (when-let [level (:interaction/regulation-level i)]
           (str/capitalize (name (first level))))
   :description "Regulation level for this interaction"})

(defn confidence [i]
  {:data (when-let [description (:interaction/confidence-description i)]
           {:Description (first description)})
   :description "Confidence details for the interaction"})

(defn rnai [i]
  {:data (when-let [rhs (:interaction/interaction-rnai i)]
           (for [rh rhs] (pack-obj rh)))
   :description "RNAi details for the interaction"})

(defn interactor [i]
  {:data (not-empty
           (remove
             nil?
           (flatten
             (conj
              (when-let [hs (:interaction/rearrangement i)]
                 (for [h hs]
                   {:interactor (when-let [r (:interaction.rearrangement/rearrangement h)]
                                  (pack-obj r))
                    :interactor_type "Rearrangement"
                    :role (when-let [roles (:interactor-info/interactor-type h)]
                            (for [role roles] (str/capitalize (name role))))
                    :transgene (when-let [tgs (:interactor-info/transgene h)]
                                 (for [tg tgs]
                                   (pack-obj tg)))
                    :variation nil}))
               (when-let [hs (:interaction/feature-interactor i)]
                 (for [h hs]
                   {:interactor (when-let [f (:interaction.feature-interactor/feature h)]
                                  (pack-obj f))
                    :interactor_type "Feature interactor"
                    :role (when-let [roles (:interactor-info/interactor-type h)]
                            (for [role roles] (str/capitalize (name role))))
                    :transgene (when-let [tgs (:interactor-info/transgene h)]
                                 (for [tg tgs]
                                   (pack-obj tg)))
                    :variation nil}))
               (when-let [hs (:interaction/molecule-interactor i)]
                 (for [h hs]
                   {:interactor (when-let [gene (:interaction.molecule-interactor/molecule h)]
                                  (pack-obj gene))
                    :interactor_type "Molecule interactor"
                    :role (when-let [roles (:interactor-info/interactor-type h)]
                            (for [role roles] (str/capitalize (name role))))
                    :transgene (when-let [tgs (:interactor-info/transgene h)]
                                 (for [tg tgs]
                                   (pack-obj tg)))
                    :variation nil}))
               (when-let [hs (:interaction/other-interactor i)]
                 (for [h hs]
                   {:interactor (:interaction.other-interactor/text h)
                    :interactor_type "Other interactor"
                    :role (when-let [roles (:interactor-info/interactor-type h)]
                            (for [role roles] (str/capitalize (name role))))
                    :transgene (when-let [tgs (:interactor-info/transgene h)]
                                 (for [tg tgs]
                                   (pack-obj tg)))
                    :variation nil}))

               (when-let [hs (:interaction/interactor-overlapping-gene i)]
                 (for [h hs
                       :let [gene (:interaction.interactor-overlapping-gene/gene h)]]
                   {:interactor (pack-obj gene)
                    :interactor_type "Interactor overlapping gene"
                    :role (when-let [roles (:interactor-info/interactor-type h)]
                            (for [role roles] (str/capitalize (name role))))
                    :transgene (when-let [tgs (:interactor-info/transgene h)]
                                 (for [tg tgs]
                                   (pack-obj tg)))
                    :variation (when-let [vhs (:interaction/variation-interactor i)]
                                 (not-empty
                                   (remove
                                     nil?
                                     (flatten
                                     (for [vh vhs
                                           :let [v (:interaction.variation-interactor/variation vh)
                                                 vghs (:variation/gene v)]]
                                       (for [vgh vghs
                                             :let [var-gene (:variation.gene/gene vgh)]]
                                         (if (= (:gene/id gene) (:gene/id var-gene))
                                           (pack-obj v))))))))}))))))
   :description "interactors in this interaction"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :libraries_screened libraries-screened
   :regulation_result regulation-result
   :interaction_type interaction-type
   :interaction_subtype interaction-subtype
   :interaction_phenotype interaction-phenotype
   :interaction_summary interaction-summary
   :process process
   :detection_method detection-method
   :regulation_level regulation-level
   :remarks generic/remarks
   :confidence confidence
   :hisotrical_gene generic/historical-gene
   :rnai rnai
   :interactor interactor})
