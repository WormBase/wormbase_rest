(ns rest-api.classes.phenotype.widgets.variaion
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(def widget
  {:varaition nil
   :variation_not nil
   :name nil})
