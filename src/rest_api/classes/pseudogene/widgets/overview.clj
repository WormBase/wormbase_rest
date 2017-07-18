(ns rest-api.classes.pseudogene.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn gene [pg]
  {:data (when-let [pghs (:gene.corresponding-pseudogene/_pseudogene pg)]
           (for [pgh pghs
                 :let [gene (:gene/_corresponding-pseudogene pgh)]]
             (pack-obj gene)))
   :description "Gene corresponding to this pseudogene"})

(defn remarks [pg]
  {:data (if-let [rhs (:pseudogene/remark pg)]
           (for [rh rhs]
             {:text (:pseudogene.remark/text rh)
              :evidence (obj/get-evidence rh)})
           (when-let [rhs (:pseudogene/db-remark pg)]
             (for [rh rhs]
               {:text (:pseudogene.db-remark/text rh)
                :evidence (obj/get-evidence rh)})))
   :description "curatorial remarks for the Pseudogene"})

(defn from-lab [pg]
  {:data (when-let [l (:pseudogene/from-laboratory pg)]
           (pack-obj l))
   :description "The laboratory of origin"})

(defn related-seqs [pg]
  {:data nil ; need sequence database (e.g. CRE31928) - not working through catalyst code.
   :description "Sequences related to this pseudogene"})

(defn parent-sequence [pg]
  {:data (when-let [pghs (:gene.corresponding-pseudogene/_pseudogene pg)]
           (when-let [gene (:gene/_corresponding-pseudogene (first pghs))]
             (when-let [parent (:locatable/parent gene)]
               (pack-obj parent))))
   :description "parent sequence of this gene"})

(defn transposon [pg]
  {:data (when-let [pghs (:gene.corresponding-pseudogene/_pseudogene pg)]
           (when-let [gene (:gene/_corresponding-pseudogene (first pghs))]
             (when-let [tg (:gene/corresponding-transposon gene)]
               (pack-obj (:gene.corresponding-transposon/transposon (first tg))))))
   :description "Transposon corresponding to this pseudogene"})

(defn brief-id [pg]
  {:data (:pseudogene.brief-identification/text
           (:pseudogene/brief-identification pg))
   :description "Short identification for this pseudogene"})

(defn type-field [pg]
  {:data (when-let [th (:pseudogene/type pg)]
           (let  [n (name (:pseudogene.type/type th))]
             (case n
               "rna" "RNA pseudogene"
               (str (str/capitalize n) " pseudogene"))))
   :k (keys pg)
   :description "The type of the pseudogene"})

(def widget
  {:name generic/name-field
   :gene gene
   :from_lab from-lab
   :taxonomy generic/taxonomy
   :related_seqs related-seqs
   :parent_sequence parent-sequence
   :transposon transposon
   :remarks remarks
   :brief_id brief-id
   :type type-field})
