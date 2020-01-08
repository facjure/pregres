(ns pregres.fixtures
  (:require [clojure.test :as test]
            [pregres.db :as db]))

(defn db [f]
  (let [db (db/connect)]
    (f)
    (db/disconnect db)))
