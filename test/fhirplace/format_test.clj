(ns fhirplace.format-test
  (:require
    [clojure.test :refer :all]
    [fhirplace.format :as fi]))

(import 'org.hl7.fhir.instance.model.AtomFeed)

(defn match-format? [req fmt]
  (is (= (fi/get-format req) fmt)))

(deftest test-get-format
  (match-format? {} :json)
  (match-format? {:params {:_format "json"}} :json)
  (match-format? {:params {:_format "application/json"}} :json)
  (match-format? {:params {:_format "application/json+fhir"}} :json)

  (match-format? {:headers {"content-type" "json"}} :json)
  (match-format? {:headers {"content-type" "application/json"}} :json)
  (match-format? {:headers {"content-type" "application/json+fhir"}} :json)

  (match-format? {:params {:_format "xml"}} :xml)
  (match-format? {:params {:_format "application/xml"}} :xml)
  (match-format? {:params {:_format "application/atom+xml"}} :xml)
  (match-format? {:params {:_format "application/xml+fhir"}} :xml)

  (match-format? {:headers {"content-type" "xml"}} :xml)
  (match-format? {:headers {"content-type" "application/xml"}} :xml)
  (match-format? {:headers {"content-type" "application/atom+xml"}} :xml)
  (match-format? {:headers {"content-type" "application/xml+fhir"}} :xml))

(deftest content-type-format-test
  (is (= (fi/content-type-format :xml "str") "application/xml+fhir; charset=UTF-8"))
  (is (= (fi/content-type-format :json "str") "application/json+fhir; charset=UTF-8"))
  (is (= (fi/content-type-format :xml (new AtomFeed)) "application/atom+xml; charset=UTF-8")))

(def <-format (fi/<-format identity))

(deftest <-format-test
  (let [resp (<-format {:body "ups" :params {:_format "xml"}})]
    (is (= (get-in resp [:headers "content-type"])
           "application/xml+fhir; charset=UTF-8")))

  (let [resp (<-format {:body "ups" :params {:_format "json"}})]
    (is (= (get-in resp [:headers "content-type"])
           "application/json+fhir; charset=UTF-8"))))
