(defproject facjure/pregres "0.2.0-SNAPSHOT"
  :description "A simple interface to Postgres"
  :url "http://facjure.com/pregres"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git" :url "https://github.com/facjure/pregres"}
  :min-lein-version "2.5.0"
  :global-vars {*warn-on-reflection* false *assert* false}
  :jvm-opts ["-Xmx512m"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.postgresql/postgresql "9.4-1206-jdbc41"]
                 [com.h2database/h2 "1.4.191"]
                 [com.stuartsierra/component "0.3.1"]
                 [prismatic/schema "1.0.1"]
                 [cheshire "5.5.0"]
                 [clj-time "0.11.0"]
                 [hikari-cp "1.3.1"]
                 [ragtime "0.5.2"]
                 [yesql "0.5.2"]])
