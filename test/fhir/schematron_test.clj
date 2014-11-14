(ns fhir.schematron-test
  (:require
    [clojure.test :refer :all]
    [fhir.schematron :as s]
    [clojure.java.io :as io]
    [saxon :as xml]))

(def pt-sch
  (s/compile-sch "fhir/patient.sch"))

(deftest schematron-test
  (is
    (nil? (pt-sch (slurp "test/fixtures/patient.xml"))))


  (is
    (map?
      (first (pt-sch
               (slurp "test/fixtures/patient-invalid-schematron.xml"))))))
