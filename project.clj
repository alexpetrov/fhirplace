(defproject fhirplace "0.1.0-SNAPSHOT"
  :description "FHIR server backed by fhirbase"
  :url "https://github.com/fhirbase/fhirplace"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[com.jakemccrary/lein-test-refresh "0.5.2"] ]

  :source-paths  ["lib/route-map/test"
                  "lib/route-map/src"
                  "src"]

  :resource-paths    ["resources"]
  :java-source-paths ["java"]

  :ring {:handler fhirplace.core/app}

  :dependencies [[org.clojure/clojure "1.5.1"]

                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [clojure-saxon "0.9.3"]

                 [honeysql "0.4.3"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]

                 [prismatic/plumbing "0.2.2"]
                 [prismatic/schema "0.2.2"]
                 [ring-mock "0.1.5"]
                 [compojure "1.1.6"]
                 ;;[org.clojure/algo.monads "0.1.5"]
                 [ring "1.2.1"]
                 [clj-time "0.6.0"]
                 [cheshire "5.3.1"]
                 [clj-http "0.9.2"]
                 [instaparse "1.3.2"] ;; parse params
                 [commons-codec "1.3"]
                 [com.google.code.gson/gson "2.2.4"]
                 [xpp3 "1.1.3.4.O"]
                 ;;[org.clojure/core.match  "0.2.1"]
                 [environ  "0.5.0"]
                 [hiccup "1.0.5"]
                 ]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [midje "1.6.0"]
                                  [leiningen "2.3.4"]
                                  [org.clojure/java.classpath "0.2.0"]]
                   :plugins [[lein-kibit "0.0.8"]] }})
