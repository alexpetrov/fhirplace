(ns fhirplace.core-test
  (:require
    [clojure.test :refer :all]
    [fhirplace.core :as fc]))

(defn mk-mw [msg]
  (fn [h]
    (fn [r]
      (h (conj r msg)))))

(deftest test-build-stack
  (let [st (fc/build-stack
             (fn [req] (conj req "handler"))
             [(mk-mw "inter-1")
              (mk-mw "inter-2")
              (mk-mw "inter-3")])]

    (is (= (st [])
           ["inter-1" "inter-2" "inter-3" "handler"]))))

(deftest test-get-cfg
  (is
    (= (fc/base-url {:scheme :http
                     :server-name "hostic"
                     :server-port 3000})
       "http://hostic:3000"))

  (= (fc/base-url {:scheme :https
                   :server-name "hostic"
                   :server-port 80})
     "https://hostic"))
