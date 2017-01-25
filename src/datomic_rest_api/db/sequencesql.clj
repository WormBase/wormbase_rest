(ns datomic-rest-api.db.sequencesql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "datomic_rest_api/db/sql/sequence.sql")
