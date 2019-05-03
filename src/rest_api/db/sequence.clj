(ns rest-api.db.sequence
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.db.main :as db]
   [rest-api.db.sequencesql :as sequencesql]))

(defn database-version []
  (pace-utils/wbdb-name (db/datomic-uri)))

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
               {:subprotocol "mysql"
                :connection-uri
                 (str "jdbc:mysql://10.0.0.181:3306/"
		       db-name
                       (str "?user=wormbase&"
                            "password=sea3l3ganz&"
                            "useSSL=false&"
                            "useLocalSessionState=true"))}})))))))

(defn get-features [db-spec feature-name]
  (sequencesql/get-features db-spec {:name feature-name}))

(defn get-features-by-attribute [db-spec feature-name]
  (let [attributes (sequencesql/get-attribute-id-by-name db-spec {:name feature-name})]
    (flatten
      (for [attribute attributes]
        (sequencesql/get-features-by-id db-spec attribute)))))

(defn sequence-features-where-type [db-spec feature-name method]
  (let [features (sequencesql/sequence-features-where-type
                      db-spec
                      {:name feature-name
                       :tag method}) ]
    (if (> (count features) 0)
      features
      (when (= method "CDS%")
        (sequencesql/sequence-features-where-type
          db-spec
          {:name feature-name
           :tag "mRNA%"})))))

(defn variation-features [db-spec variation-name]
  (sequencesql/variation-features
    db-spec
    {:name variation-name}))

(defn get-sequence [db-spec location start stop]
  (let [low (if (> stop start) start stop)
        high (if (> stop start) stop start)
        low-offset (* (int (/ low 2000)) 2000)
        high-offset  (* (int (/ high 2000)) 2000)
        offsets  (range low-offset (+ 2000 high-offset) 2000)
        start-remove (mod low 2000)
        offsets-sequence (some->> offsets
                                  (map
                                    (fn [offset]
                                      (some->> (sequencesql/get-sequence
                                                 db-spec
                                                 {:location location
                                                  :offset offset})
                                               (first)
                                               (:sequence)
                                               )))
                                  (str/join)
                                  (str/lower-case))]
    (subs offsets-sequence
          (- (mod low 2000) 1)
          (- high low-offset))))

(defn get-seq-features [db-spec transcript]
  (sequencesql/get-seq-features db-spec {:name transcript}))
