(ns datomic-rest-api.db.sequence
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datomic-rest-api.db.sequencesql :as sequencesql]
   [environ.core :refer (env)]))

(defn database-version []
  (env :ws-version))

(def species-assemblies
  (->> "ASSEMBLIES.json"
       io/resource
       io/reader
       json/read))

(defn get-default-sequence-database [g-species]
  (if-let [assemblies (species-assemblies g-species)]
    (let [defaults (for [assembly (assemblies "assemblies")
                         :when (= (assembly "is_canonical") true)]
                     (str/join
                       "_"
                       [g-species
                        (assembly "bioproject")
                        (database-version)]))]
      (if (seq defaults)
        (first defaults)))))

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
                       (str "?user=wormbase&"
                            "password=sea3l3ganz&"
                            "useSSL=false&"
                            "useLocalSessionState=true"))}})))))))

(defn gene-features [db-spec gene-name]
  (sequencesql/gene-features db-spec {:name gene-name}))
