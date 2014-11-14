(ns fhirplace.pg
  (:require [clojure.java.jdbc :as cjj]
            [honeysql.core :as hc]
            [clojure.data.json :as json]
            [clojure.string :as cs]
            [fhir :as f]
            [honeysql.helpers :as hh]
            [environ.core :as env]))

(import ' org.postgresql.util.PGobject)
(import 'java.sql.Timestamp)

(def ^:dynamic *db*
  {:subprotocol (env/env :fhirplace-subprotocol)
   :subname (env/env :fhirplace-subname)
   :user (env/env :fhirplace-user)
   :stringtype "unspecified"
   :password (env/env :fhirplace-password)})

(defmacro with-db  [db & body]
  `(binding  [*db* ~db]
     ~@body))

(defmacro transaction  [& body]
  `(cjj/with-db-transaction  [t-db# *db*]
     (with-db t-db# ~@body)))

(defmacro rollback-transaction  [& body]
  `(cjj/with-db-transaction  [t-db# *db*]
     (cjj/db-set-rollback-only! t-db#)
     (with-db t-db# ~@body)))


(defn uuid  [] (java.util.UUID/randomUUID))

(extend-protocol cjj/IResultSetReadColumn
  PGobject
  (result-set-read-column  [pgobj metadata idx]
    (let  [type  (.getType pgobj)
           value  (.getValue pgobj)]
      (case type
        "json" value
        "jsonb" value
        :else value))))

(defn q* [sql]
  (println "SQL:" (pr-str sql))
  (cjj/query *db* sql))

(defn call* [proc & args]
  (let [proc-name (name proc)
        params (cs/join "," (map (constantly "?") args))
        sql (str "SELECT " proc-name "(" params ")")]
    (get (first (q* (into [sql] args))) proc)))

(defn qcall* [proc & args]
  (let [proc-name (name proc)
        params (cs/join "," (map (constantly "?") args))
        sql (str "SELECT * FROM " proc-name "(" params ")")]
    (q* (into [sql] args))))

(defn q [hsql]
  (let [sql (hc/format hsql)]
    (println "SQL:" sql)
    (cjj/query *db* sql)))

(defn q-one [hsql]
  (first (q hsql)))

(defn e [sql]
  (let [sql sql]
    (println "SQL:" sql)
    (cjj/execute! *db* sql)))

(defn i [tbl attrs]
  (first
    (cjj/insert! *db* tbl attrs)))
