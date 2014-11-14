(ns fhirplace.plugins-test
  (:require
    [clojure.test :refer :all]
    [fhirplace.plugins :as fp]))

(def test-url "https://github.com/fhirbase/fhirplace-empty-plugin.git")

(deftest test-utils
  (is (= (fp/url-to-name test-url)
         "fhirplace-empty-plugin"))
  (is (not (nil? (fp/plugin-path "ups")))))


(comment
  (fp/pull-plugin-plan test-url)

  (fp/exec-plan (fp/pull-plugin-plan test-url)))
