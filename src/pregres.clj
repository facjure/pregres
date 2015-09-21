(ns pregres
  (:require [clojure.tools.logging :as log]
            [pregres.db :as db]
            [com.stuartsierra.component :as component]))

(defrecord Database [db-spec connection]

  component/Lifecycle

  (start [component]
    (if connection
      component
      (do (log/info "Starting database")
          (let [conn (db/connect db-spec)]
            (assoc component :connection conn)))))

  (stop [component]
    (if (not connection)
      component
      (do
        (log/info "Stopping database")
        (db/disconnect connection)
        (assoc component :connection nil)))))


(defn new-Database
  "Create a new Database Component"
  [db-spec]
  (map->Database {:db-spec db-spec}))
