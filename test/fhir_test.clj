(ns fhir-test
  (:require
    [clojure.test :refer :all]
    [fhir :as f]))


(def x (slurp "test/fixtures/patient.xml"))
(def jx (slurp "test/fixtures/patient.json"))

(deftest conformance-test
  (is (not= (f/conformance) nil))

  (is (instance? org.hl7.fhir.instance.model.Patient
                 (f/parse jx)))
  (is (instance? org.hl7.fhir.instance.model.Patient
                 (f/parse x)))
  (is (= (f/errors (f/parse x)) nil))

  (is (instance? String (f/serialize :xml (f/parse x)))))
