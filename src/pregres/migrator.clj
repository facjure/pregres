(ns pregres.migrator
  "SQL schema migration library"
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [environ.core :refer :all])
  (:import (java.sql Timestamp)))

(defn load-config [e]
  {:datastore   (case e
                  :dev (jdbc/sql-database (:database-url env))
                  :test (jdbc/sql-database (:database-test-url env)))
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  ([]
   (migrate :dev))
  ([e]
   (repl/migrate (load-config e))))

(defn rollback
  ([]
   (rollback :dev))
  ([e]
   (repl/rollback (load-config e))))

(defn reset!
  ([]
   (reset! :dev))
  ([e]
   (let [migrations (count (second (second (load-config e))))]
     (dotimes [n migrations]
       (rollback e)))))
