(ns pregres.core.test-fixtures
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [pregres.db :as db]
            [environ.core :refer [env]]))

(defn db [f]
  (let [db (db/connect)]
    (f)
    (db/disconnect db)))
