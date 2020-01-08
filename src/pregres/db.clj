(ns pregres.db
  "Core Database connection, pool, and other management features."
  (:require
   [clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [hikari-cp.core :as hikari]
   [pregres.utils :as utils]
   [pregres.types :as types]))


;; TODO Fine tune defaults
(defn connect
  "Connect to the specified database and return a Connection backed by Hikari Pool.
   If db-spec map is passed, connect to DATABASE_URL environment variable. If
   that's not found, I connect to a fresh postgres installation at
   localhost:5432/$user with no auth. Calling connect multiple times is safe, as
   I don't create another pool, but will return a cached connection from the
   pool."
  [& db-spec]
  (let [db-uri (java.net.URI. (or (:database-url db-spec)
                                  (str "postgresql://localhost:5432/postgres")))
        user-and-password (if (nil? (.getUserInfo db-uri))
                            nil (str/split (.getUserInfo db-uri) #":"))
        pooled-ds (hikari/make-datasource
                   {:auto-commit true
                    :connection-timeout (or (:connection-timeout db-spec) 30000)
                    :validation-timeout (or (:validation-timeout db-spec) 5000)
                    :idle-timeout (or (:idle-timeout db-spec) 600000)
                    :max-lifetime (or (:max-lifetime db-spec) 1800000)
                    :minimum-idle (or (:minimum-idle db-spec) 10)
                    :maximum-pool-size (or (:maximum-pool-size db-spec) 100)
                    :adapter "postgresql"
                    :username (get user-and-password 0)
                    :password (get user-and-password 1)
                    :database-name (str/replace-first (.getPath db-uri) "/" "")
                    :server-name (.getHost db-uri)
                    :port-number (.getPort db-uri)})
        db (assoc {}
                  :datasource pooled-ds
                  :connection (.getConnection pooled-ds))]
    db))

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

(defn disconnect
  "Disconnect datasource, shutdown connection pool, cleanup."
  [ds]
  (log/info "Shutting down datasource")
  (.close (:datasource ds)))
