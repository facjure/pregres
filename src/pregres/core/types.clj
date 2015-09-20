(ns pregres.core.types
  (:refer-clojure :exclude [find read update])
  (:require
   [clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]
   [clojure.string :as str]
   [hikari-cp.core :as hikari]
   [environ.core :refer :all]
   [pregres.utils :as utils]
   ;; importing clj-time registers sql/time coercions
   [clj-time.jdbc])
  (:import
   [java.sql Timestamp]
   [java.util Date UUID]
   [org.postgresql.jdbc4 Jdbc4Array]
   [org.postgresql.util PGobject]
   [java.net.URI]))

;; ----------------
;; Coercion Helpers

(defn ->json-pg-object [val]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (if (string? val) val
                 (json/encode val)))))

(defn json-pg-object->clj [^PGobject pg-obj]
  (json/decode (.getValue pg-obj) keyword))

(defn ->pg-uuid-array [^org.postgresql.jdbc4.Jdbc4Connection conn coll]
  (let [coll (map #(UUID/fromString %) coll)]
    (.createArrayOf conn "uuid" (into-array coll))))

(defn to-vec [^Jdbc4Array jdbc-arr]
  (vec (.getArray ^Jdbc4Array jdbc-arr)))

;; -------------------
;; Protocol Extensions

;; SQL -> CLJ
(extend-protocol jdbc/IResultSetReadColumn

  Jdbc4Array
  (result-set-read-column [jdbc-arr _ _]
    (to-vec jdbc-arr))

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
