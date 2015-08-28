(ns pregres.db
  (:refer-clojure :exclude [find read update])
  (:require
   [clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]
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

;; ----------------------------------------------------------------------------
;; Connection Pooling, Datasource

(defn connect
  "Create a datasource backed by Hikari Connection Pool, with options.
   Returns a conn map {:datasource datasource} that can be passed to jdbc/method"
  [& options]
  (let [db-uri (java.net.URI. (or (:database-url options)
                                  (:database-url env)
                                  (str "postgresql://localhost:5432/" (:user env))))
        user-and-password (if (nil? (.getUserInfo db-uri))
                            nil (str/split (.getUserInfo db-uri) #":"))]
    (->> (hikari/make-datasource
          {:auto-commit true
           :connection-timeout (or (:connection-timeout options) 30000)
           :validation-timeout (or (:validation-timeout options) 5000)
           :idle-timeout (or (:idle-timeout options) 600000)
           :max-lifetime (or (:max-lifetime options) 1800000)
           :minimum-idle (or (:minimum-idle options) 10)
           :maximum-pool-size (or (:maximum-pool-size options) 10)
           :adapter "postgresql"
           :username (get user-and-password 0)
           :password (get user-and-password 1)
           :database-name (str/replace-first (.getPath db-uri) "/" "")
           :server-name (.getHost db-uri)
           :port-number (.getPort db-uri)})
         (assoc {} :datasource))))

;; -----------------------------------------------------------------------------
;; Transactions

(defmacro with-transaction
  "(with-db-transaction [txn db-spec]
    ... txn ...)"
  [binding & body]
  `(jdbc/with-db-transaction ~binding ~@body))

(defmacro with-read-only-transaction
  "(with-db-transaction [txn db-spec]
    ... txn ...)"
  [binding & body]
  `(jdbc/with-db-transaction ~(into binding [:read-only? true]) ~@body))

(defmacro with-test-transaction
  "Tests in a Transaction block., but rolls back after txns.
   Useful for tests and ad-hoc reports"
  [[txn maybe-spec] & body]
  (let [spec (or maybe-spec (connect))]
    `(jdbc/with-db-transaction [~txn ~spec]
       (try
         ~@body
         (finally
           (reset! (:rollback ~txn) true))))))

;; -----------------------------------------------------------------------------
;; Drivers, Serializers, Deserializers

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

