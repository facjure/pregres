(ns pregres.queries
  (:refer-clojure :exclude [find read update])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [hikari-cp.core :as hikari]
   [pregres.db :as db]
   [pregres.core.utils :as utils])
  (:import
   [java.sql Timestamp]
   [java.util Date UUID]
   [org.postgresql.jdbc4 Jdbc4Array]
   [org.postgresql.util PGobject]
   [java.net.URI]))

;; Helpers -----

(defn- row-transform [only-id? row]
  (if only-id?
    (:id row)
    row))

(defn current-sql-time []
  (Timestamp. (utils/now)))

(defn in [xs]
  (format "IN (%s)"
          (->> "?"
               (repeat (count xs))
               (str/join ", "))))

(defn uuid-in [xs]
  (format "IN (%s)"
          (->> "?::uuid"
               (repeat (count xs))
               (str/join ", "))))

;; API -----

(defn create [db table m & {:keys [only-id?] :or {only-id? true}}]
  (->> (jdbc/insert! db table m :entities utils/keyword->underscored-string)
       utils/only
       (row-transform only-id?)))

(defn create-batch [db table ms & {:keys [only-id?] :or {only-id? true}}]
  (->> (apply jdbc/insert!
              db
              table
              (conj (vec ms) :entities utils/keyword->underscored-string))
       (map #(row-transform only-id? %))))

(defn read
  "Read sequences of 0 or more rows"
  [db sql-params]
  (jdbc/query db sql-params
              :identifiers utils/underscores->hyphens))

(defn update
  "Update 0-many rows in the DB"
  [db table m where-clause]
  (jdbc/update! db
                table
                (assoc m :updated-at (current-sql-time))
                where-clause
                :entities utils/keyword->underscored-string)
  nil)

(defn delete [db table where-clause]
  (jdbc/delete! db table where-clause
                :entities utils/keyword->underscored-string))

(defn soft-delete
  "Mark row as (soft) deleted, but keep it in the DB."
  [db table id]
  (update db
          table
          {:deleted-at (current-sql-time)}
          ["id = ?" id])
  nil)

(defn find
  "Pull 1 or nil rows from the DB"
  [db sql-args]
  (first (read db sql-args)))

(defn run-script
  "Executes a SQL script under resources dir."
  [ds sql-file]
  (let [commands (slurp (io/resource sql-file))]
    (jdbc/db-do-commands ds commands)))
