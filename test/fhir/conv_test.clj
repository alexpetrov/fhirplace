(ns fhir.conv-test
  (:require
    [clojure.test :refer :all]
    [fhir.conv :as f]))

(def json-str (slurp "test/fixtures/patient.json"))
(def xml-str  (slurp "test/fixtures/patient.xml"))

(import org.hl7.fhir.instance.model.Patient)

(deftest conv-test

  (is
    (instance?  org.hl7.fhir.instance.model.Patient
               (f/from-json json-str)))
  (is
    (instance?  org.hl7.fhir.instance.model.Patient
               (f/from-xml xml-str))))
