(defproject facjure/pregres "0.3.0-SNAPSHOT"
 :description "A Clojure interface to Postgres"
  :url "http://facjure.com/pregres"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git" :url "https://github.com/facjure/pregres"}
  :min-lein-version "2.8.0"
  :global-vars {*warn-on-reflection* false *assert* false}
  :jvm-opts ["-Xmx512m"]
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.postgresql/postgresql "42.2.9"]
                 [clojure.java-time "0.3.2"]
                 [cheshire "5.9.0"]
                 [hikari-cp "2.10.0"]
                 [ragtime "0.8.0"]])
