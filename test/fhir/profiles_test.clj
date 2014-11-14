(ns fhir.profiles-test
  (:require
    [clojure.test :refer :all]
    [fhir.conv :as fc]
    [fhir.profiles :as fp]
    [clojure.java.io :as io]))

(deftest conformance-test
    (is
      (= (.toString (.getResourceType fp/conformance))
      "Conformance")))
