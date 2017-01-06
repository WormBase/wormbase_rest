(ns datomic-rest-api.utils.db
  (:require [mount.core :refer [defstate]]
            [conman.core :as conman]
            [environ.core :refer (env)]
            [miner.ftp :as ftp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [datomic.api :as d]))

(defn- new-datomic-connection []
  (d/connect (env :trace-db)))

(defn- database-version []
    (env :wb-version))

(defn- datomic-disconnect [conn]
  (d/release conn))

(defstate datomic-conn
  :start (new-datomic-connection)
  :stop (datomic-disconnect datomic-conn))

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
    (for [assembly ((species-assemblies g-species) "assemblies")]; :when (= (assembly "is_canonical") "true")]
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

(defn connect!
  [pool-specs]
  (reduce merge (map (fn [pool-spec]
                       {(keyword (key pool-spec)) (conman/connect! {:jdbc-url (val pool-spec)})}) pool-specs)))

;; Disconnect from all databases in db-connections
(defn disconnect!
  [db-connections]
  (map (fn [db] (conman/disconnect! (val db))) db-connections))

;; Establish connections to all databases
;; and store connections in *dbs*
(defstate ^:dynamic *sequence-dbs*
          :start (connect!
                   sequence-db-urls)
          :stop (disconnect! *sequence-dbs*))
