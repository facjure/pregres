(ns pregres.db-test
  (:require [clojure.test :refer :all]
            [pregres.core.test-fixtures :as fixtures]
            [pregres.db :refer :all]))

(use-fixtures :once fixtures/db)

(def bad-db-config {})
(def good-db-config {:database-url "postgresql://localhost:5432/"})

(deftest connections
  (testing "Connect without any params?"
    (is (contains? (connect)  :datasource)))
  (testing "Connect with bad db config?"
    (is (connect good-db-config) :datasource))
  (testing "Connect with good db config and find info"
    (let [db (connect good-db-config)]
      (contains? db :datasource)
      (contains? (info db) :schemas)))
  (testing "Disconnect bad conn"
    (let [db (connect good-db-config)]
      (disconnect db))))
