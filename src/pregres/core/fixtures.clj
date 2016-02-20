(ns pregres.core.fixtures
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [pregres.db :as db]))

(defn db [f]
  (let [db (db/connect)]
    (f)
    (db/disconnect db)))
