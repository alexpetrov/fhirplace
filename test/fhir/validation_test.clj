(ns fhir.validation-test
  (:require
    [clojure.test :refer :all]
    [fhir.conv :as fc]
    [fhir.validation :as fv]))


(defn slurp-res [pth]
  (fc/from-xml (slurp pth)))

(slurp-res "test/fixtures/patient.xml")

(deftest validation-test

  (is
    (nil?
      (-> (slurp-res "test/fixtures/patient.xml")
          fv/errors)))
  (is
    (map?
      (-> (slurp-res "test/fixtures/patient-invalid-schematron.xml")
          fv/errors
          first))))
