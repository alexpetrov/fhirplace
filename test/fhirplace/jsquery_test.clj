(ns fhirplace.jsquery-test
  (:require
    [clojure.test :refer :all]
    [fhirplace.jsquery :as fj]))

(defn query-match? [x y]
  (is (= (fj/jsquery x) y)))

(deftest jsquery-test
  (query-match? "confirmed" "\"confirmed\"")

  (query-match? 1 1)

  (query-match?  [:= "status" "confirmed"] "\"status\" = \"confirmed\"")

  (query-match?  [:= "status" 1] "\"status\" = 1")

  (query-match?
    [:& [:= "system" 2] [:= "code" 3]]
    "\"system\" = 2 & \"code\" = 3")

  (query-match?
    [:| [:= "system" 2] [:= "code" 3]]
    "\"system\" = 2 | \"code\" = 3")

  (query-match?
    [:| [:= "system" 2] [:= "code" 3]]
    "\"system\" = 2 | \"code\" = 3")
  (query-match?
    ["category.coding.#"
     [:&
      [:= "system" 2]
      [:= "code" 3]]]
    "\"category\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 )")

  (query-match?
    [:&
     [:= "status" "confirmed"]
     ["category.coding.#"
      [:&
       [:= "system" 2]
       [:= "code" 3]]]
     ["code.coding.#"
      [:&
       [:= "system" 2]
       [:= "code" 3]]]]

    "\"status\" = \"confirmed\" & \"category\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 ) & \"code\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 )")
  )

