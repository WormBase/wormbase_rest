(ns datomic-rest-api.db.sequence
  (:require [datomic-rest-api.db.sequencesql :as sequencesql]
            [environ.core :refer (env)]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(defn database-version []
  (env :wb-version "WS257"))

(def species-assemblies
  (json/read-str
    (slurp
      (str/join
        "."
        ["resources/assemblies/ASSEMBLIES"
         (database-version)
         "json"]))))

(defn get-default-sequence-database [g-species]
  (first
    (for [assembly ((species-assemblies g-species) "assemblies") :when (= (assembly "is_canonical") true)]
      (str/join
        "_"
        [g-species
         (assembly "bioproject")
         (database-version)]))))

(def sequence-dbs
  (into
    {}
    (flatten
      (for [[g-species species] species-assemblies]
        (flatten
          (for [assembly (species "assemblies")]
            (let [db-name (str/join
                            "_"
                            [(name g-species)
                             (assembly "bioproject")
                             (database-version)])]

              {(keyword db-name)
               {:connection-uri
                 (str "jdbc:mysql://10.0.0.181:3306/"
		       db-name
                       "?user=wormbase&password=sea3l3ganz&useSSL=false&useLocalSessionState=true")}})))))))

(defn gene-features [db-spec gene-name]
  (sequencesql/gene-features db-spec {:name gene-name}))
