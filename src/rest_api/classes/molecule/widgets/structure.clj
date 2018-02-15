(ns rest-api.classes.molecule.widgets.structure
  (:require
    [rest-api.classes.generic-fields :as generic]))

(defn inchi [m]
  {:data (first (:molecule/inchi m))
   :description "InChi structure"})

(defn monoisotopic-mass [m]
  {:data (when-let [m (first (:molecule/monoisotopic-mass m))]
           (format "%.4f" m))
   :description "Monoisotopic mass calculated from the chemical formula of the molecule"})

(defn formula [m]
  {:data (first (:molecule/formula m))
   :description "Molecular formula from ChEBI"})

(defn smiles [m]
  {:data (first (:molecule/smiles m))
   :description "SMILES structure"})

(defn iupac [m]
  {:data (first (:molecule/iupac m))
   :description "IUPAC name"})

(defn inchi-key [m]
  {:data (first (:molecule/inchikey m))
   :description "InChi structure key"})

(def widget
  {:name generic/name-field
   :inchi inchi
   :monoisotopic_mass monoisotopic-mass
   :formula formula
   :smiles smiles
   :iupac iupac
   :inchi_key inchi-key})
