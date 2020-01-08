(ns pregres-test
  (:require
   [clojure.test :refer :all]
   [pregres.fixtures :as fixtures]
   [pregres :refer :all]))

(use-fixtures :once fixtures/db)

(def db-spec {:database-url "postgresql://localhost:5432/"})
