(ns fhir.xsd-test
  (:require
    [clojure.test :refer :all]
    [fhir.xsd :as fx]))


(def pt-validator
  (fx/mk-validator "fhir/patient.xsd"))

(deftest xsd-test
  (is
    (nil? (pt-validator
            (slurp "test/fixtures/patient.xml"))))

  (is
    (re-find #"org.xml.sax.SAXParseException"
                (pt-validator
                  (slurp "test/fixtures/invalid-patient.xml")))))
