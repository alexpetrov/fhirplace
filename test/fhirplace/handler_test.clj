(ns fhirplace.handler-test
  (:use midje.sweet
        fhirplace.core
        fhirplace.app
        ring.mock.request)
  (:require [fhirplace.handler :refer :all]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.db :as db]
            [fhirplace.system :as system])) 

(def db-spec (db/conn))

(defn read-patient []
  (slurp "test/fixtures/patient.json"))

(defn perform-request [& request-params]
  (let [app (create-web-handler (system/create))]
    (app (apply request request-params))))

(defn parse-body [response]
  (json/read-str (:body response)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(facts "About READ for existed resource"
  (let [patient (read-patient)
        patient-id (insert-patient db-spec patient)
        req (perform-request :get (str "/patient/" patient-id))
        res (parse-body req)]
              
      (get res "_id")          => patient-id
      (get res "resourceType") => "Patient"
      (:status req)            => 200)
    (clear-resources db-spec))

(facts "About READ for non-existed resource"
  (:status (perform-request :get "/patient/blablabla"))     => 404
  (:status (perform-request :get (str "/patient/" (uuid)))) => 404)

(facts "About CREATE"
  (let [patient (read-patient)]))
