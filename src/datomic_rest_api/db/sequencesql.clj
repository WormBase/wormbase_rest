(ns datomic-rest-api.db.sequencesql
  (:require [hugsql.core :as hugsql]))

;(hugsql/def-sqlvec-fns "datomic_rest_api/db/sql/sequence.sql")
(hugsql/def-db-fns "datomic_rest_api/db/sql/sequence.sql")
