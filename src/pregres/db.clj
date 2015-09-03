(ns pregres.db
  (:refer-clojure :exclude [find read update])
  (:require
   [clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]
   [clojure.string :as str]
   [hikari-cp.core :as hikari]
   [environ.core :refer :all]
   [pregres.utils :as utils]
   [clj-time.jdbc])
  (:import
   [java.sql Timestamp]
   [java.util Date UUID]
   [org.postgresql.jdbc4 Jdbc4Array]
   [org.postgresql.util PGobject]
   [java.net.URI]))

;; Managed Datasource
(def ds (atom {:datasource nil}))

;; ----------------------------------------------------------------------------
;; Connection Pool, Datasource

(defn connect
  "Create a Connection backed by Hikari Connection Pool, with optional db-spec map.
   If no db-spec is passed, I will connect to DATABASE_URL environment
  variable. If that's not found, I will assume a fresh postgres installation at
  localhost:5432/$user with no auth. Calling connect multiple times is safe, as
  it does not create another pool: a cached instance is returned."
  [& db-spec]
  (if (nil? (:datasource @ds))
    (let [db-spec (first db-spec) ;; grab the first db-spec map
          db-uri (java.net.URI. (or (:database-url db-spec)
                                    (:database-url env)
                                    (str "postgresql://localhost:5432/" (:user env))))
          user-and-password (if (nil? (.getUserInfo db-uri))
                              nil (str/split (.getUserInfo db-uri) #":"))
          pooled-ds (->> (hikari/make-datasource
                          {:auto-commit true
                           :connection-timeout (or (:connection-timeout db-spec) 30000)
                           :validation-timeout (or (:validation-timeout db-spec) 5000)
                           :idle-timeout (or (:idle-timeout db-spec) 600000)
                           :max-lifetime (or (:max-lifetime db-spec) 1800000)
                           :minimum-idle (or (:minimum-idle db-spec) 10)
                           :maximum-pool-size (or (:maximum-pool-size db-spec) 10)
                           :adapter "postgresql"
                           :username (get user-and-password 0)
                           :password (get user-and-password 1)
                           :database-name (str/replace-first (.getPath db-uri) "/" "")
                           :server-name (.getHost db-uri)
                           :port-number (.getPort db-uri)})
                         (assoc {} :datasource))]
      (reset! ds pooled-ds))
    @ds))

(defn info
  "Get Database Information from the current connection"
  [ds]
  (let [meta (.getMetaData (.getConnection (:datasource ds)))]
    {:url (.getURL meta)
     :driver-version (.getDriverVersion meta)
     :driver-name (.getDriverName meta)
     :db-product-version (.getDatabaseProductVersion meta)
     :db-product-name (.getDatabaseProductName meta)
     :db-major-version (.getDatabaseMajorVersion meta)
     :db-minor-version (.getDatabaseMinorVersion meta)
     :types (.getTypeInfo meta)
     :schemas (.getSchemas meta)}))

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
;; Coercions, Serializers, Deserializers

;; NOTE: (require '[clj.time.jdbc]) above registers sql/time coercions

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

