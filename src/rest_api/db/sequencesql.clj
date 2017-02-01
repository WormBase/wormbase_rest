(ns rest-api.db.sequencesql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "rest_api/db/sql/sequence.sql")
