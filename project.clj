(defproject facjure/pregres "0.1.0"
  :description "An integrated Clojure lib for Postgres"
  :url "http://facjure.com/pregres"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git" :url "https://github.com/facjure/pregres"}
  :min-lein-version "2.5.0"
  :global-vars {*warn-on-reflection* false *assert* false}
  :jvm-opts ["-Xmx1g"]
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [prismatic/schema "1.0.0"]
                 [cheshire "5.5.0"]
                 [clj-time "0.10.0"]
                 [environ "1.0.0"]
                 [hikari-cp "1.3.0"]
                 [ragtime "0.5.1"]
                 [yesql "0.4.2"]])
