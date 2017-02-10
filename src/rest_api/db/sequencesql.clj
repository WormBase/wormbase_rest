(ns rest-api.db.sequencesql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "rest_api/db/sql/sequence.sql")

; Use the following to determine the sql being generated.
; See: https://www.hugsql.org/
;(hugsql/def-sqlvec-fns "rest_api/db/sql/sequence.sql")
