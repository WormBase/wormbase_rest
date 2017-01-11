(ns datomic-rest-api.db.sequence
  (:require [mount.core :refer [defstate]]
            [hugsql.core :as hugsql]
            [environ.core :refer (env)]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(defn database-version []
  (env :wb-version))

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

(def sequence-db-urls
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
               (str
                 "jdbc:mysql://10.0.0.181:3306/"
                 db-name
                 "?user=wormbase&password=sea3l3ganz&useSSL=false&useLocalSessionState=true")})))))))
;                 "?useSSL=false&useLocalSessionState=true")})))))))

(hugsql/def-db-fns "datomic_rest_api/db/sql/sequence.sql")

(defn sequence-features [sequence-database gene-id]
 (let [db-url  ((keyword sequence-database) sequence-db-urls)]
     db-url
;    (features db {:name gene-id})
))
