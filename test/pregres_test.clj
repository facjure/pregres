(ns pregres-test
  (:require
   [clojure.test :refer :all]
   [pregres.core.fixtures :as fixtures]
   [pregres :refer :all]))

(use-fixtures :once fixtures/db)

(def db-spec {:database-url "postgresql://localhost:5432/"})

(deftest components
  (testing "starting, components"
    (new-Database db-spec)))
