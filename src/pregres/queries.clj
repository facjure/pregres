(ns pregres.queries
  (:refer-clojure :exclude [find read update])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [hikari-cp.core :as hikari]
   [environ.core :refer :all]
   [pregres.utils :as utils])
  (:import
   [java.sql Timestamp]
   [java.util Date UUID]
   [org.postgresql.jdbc4 Jdbc4Array]
   [org.postgresql.util PGobject]
   [java.net.URI]))

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

(defn read
  "Read sequences of 0 or more rows"
  [db-spec sql-params]
  (jdbc/query db-spec sql-params
              :identifiers utils/underscores->hyphens))

(defn find
  "Pull 1 or nil rows from the DB"
  [db-spec sql-args]
  (first (read db-spec sql-args)))

(defn- row-transform [only-id? row]
  (if only-id?
    (:id row)
    row))

(defn create [db-spec table m & {:keys [only-id?] :or {only-id? true}}]
  (->> (jdbc/insert! db-spec table m :entities utils/keyword->underscored-string)
       utils/only
       (row-transform only-id?)))

(defn create-batch [db-spec table ms & {:keys [only-id?] :or {only-id? true}}]
  (->> (apply jdbc/insert!
              db-spec
              table
              (conj (vec ms) :entities utils/keyword->underscored-string))
       (map #(row-transform only-id? %))))

(defn current-sql-time []
  (Timestamp. (utils/now)))

(defn update
  "Update 0-many rows in the DB"
  [db-spec table m where-clause]
  (jdbc/update! db-spec
                table
                (assoc m :updated-at (current-sql-time))
                where-clause
                :entities utils/keyword->underscored-string)
  nil)

(defn soft-delete
  "Mark row as (soft) deleted, but keep it in the DB."
  [db-spec table id]
  (update db-spec
          table
          {:deleted-at (current-sql-time)}
          ["id = ?" id])
  nil)

(defn delete [db-spec table where-clause]
  (jdbc/delete! db-spec table where-clause
                :entities utils/keyword->underscored-string))
