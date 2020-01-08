(ns pregres.types
  (:refer-clojure :exclude [find read update])
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [java-time :as dt]
            [java-time.format :as dtf]
            [pregres.utils :as utils])
  (:import [java.net URI]
           [java.sql Array Timestamp]
           [java.util Date UUID]
           [java.time LocalDate LocalDateTime LocalTime ZoneId]
           [java.time.format DateTimeFormatter]
           [org.postgresql.util PGobject]))


(defprotocol ICoerce
  (to-date-time ^LocalDateTime [obj] "Convert any `obj` to a DateTime instance."))


(defn ->uuid [^String s]
  (UUID/fromString s))

(defn ->pg-object [^String type ^String value]
  (doto (PGobject.)
    (.setType type)
    (.setValue value)))

(defn ->json-pg-object [val]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (if (string? val) val
                 (json/encode val)))))

(defn json-pg-object->clj [^PGobject pg-obj]
  (json/decode (.getValue pg-obj) keyword))

(defn ->pg-uuid-array [conn coll]
  (let [coll (map #(UUID/fromString %) coll)]
    (.createArrayOf conn "uuid" (into-array coll))))

(defn pg-array->vec [^Array j4a]
  (vec (.getArray ^Array j4a)))


;; Protocol Extensions

;; SQL -> CLJ
(extend-protocol jdbc/IResultSetReadColumn

  Array
  (result-set-read-column [jdbc-arr _ _]
    (vec (.getArray jdbc-arr)))

  Timestamp
  (result-set-read-column [ts _ _]
    (.getTime ^Timestamp ts))

  UUID
  (result-set-read-column [uuid _ _]
    (str uuid))

  PGobject
  (result-set-read-column [pg-obj _ _]
    (case (.getType pg-obj)
      "json" (json-pg-object->clj pg-obj)
      "jsonb" (json-pg-object->clj pg-obj)
      pg-obj)))

;; CLJ -> SQL
(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [val]
    (->json-pg-object val))

  clojure.lang.IPersistentVector
  (sql-value [val]
    (->json-pg-object val)))


(extend-protocol ICoerce
  nil
  (to-date-time [_]
    nil)

  Date
  (to-date-time [date]
    (when date
      (dt/local-date (.toInstant date) (ZoneId/systemDefault))))

  java.sql.Date
  (to-date-time [sql-date]
    (when sql-date
      (.toLocalDate sql-date)))

  java.sql.Timestamp
  (to-date-time [sql-time]
    (when sql-time
      (.toLocalDate sql-time)))

  Integer
  (to-date-time [integer]
    (dt/local-date (long integer) (ZoneId/systemDefault)))

  Long
  (to-date-time [longnum]
    (dt/local-date longnum (ZoneId/systemDefault)))

  String
  (to-date-time [string]
    (dt/local-date "FIXME" "2011-12-03T10:15:30Z" ))) ;; ISO_INSTANT
