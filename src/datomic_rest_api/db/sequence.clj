(ns datomic-rest-api.db.sequence
  (:require ;[hugsql.core :as hugsql]
            [clojure.java.jdbc :as j]
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
;                {:subprotocol "mysql"
;                 :subname  (str "//10.0.0.181:3306/" db-name "?username=wormbase&password=&useSSL=false&useLocalSessionState=true")
;                 :username "wormbase"
;                 :password "sea3l3ganz"}})))))))
              {:classname "com.mysql.jdbc.Driver"
               :connection-uri (str "jdbc:mysql://10.0.0.181:3306/" db-name "?username=wormbase&password=sea3l3ganz&useSSL=false&useLocalSessionState=true")
               
               }})))))))

;               {:dbtype "mysql"
;                :classname "com.mysql.cj.jdbc.Driver"
;                :dbname db-name
;                :host "10.0.0.181"
;                :port 3306
;                :ssl false
;                :localSessionState true
;;                :subname (str "//10.0.0.181:3306/" db-name "?useSSL=false&useLocalSessionState=true")
;                :user "wormbase"
;                :password "sea3l3ganz"}})))))))
;
;
(defn gene-features [db-spec gene-name]
  (println "test")
(println db-spec)
  (let [data (j/query db-spec ["SELECT f.id,f.object,f.typeid,f.seqid,f.start,f.end,f.strand FROM feature as f JOIN name as n ON n.id=f.id WHERE n.name = ?" gene-name])]
      data))


;(hugsql/def-sqlvec-fns "datomic_rest_api/db/sql/sequence.sql")
;(hugsql/def-db-fns "datomic_rest_api/db/sql/sequence.sql")
