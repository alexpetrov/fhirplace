(ns fhirplace.category-test
  (:require [fhirplace.category :as subj]
            [clojure.test :refer :all]))

(def samples
  {"dog" [{:term "dog"}]
   "dog, cat" [{:term "dog"} {:term "cat"}]
   "dog; label=\"Canine\"; scheme=\"sch\"" [{:term "dog"
                                             :scheme "sch"
                                             :label "Canine"}]})

(deftest parse-test
  (doseq [[s r] samples]
    (is (= (subj/parse s) r))))

(deftest encode-test

  (is (= (subj/encode-tag {:term "dog"})
         "dog"))

  (is (= (subj/encode-tag {:term "dog" :label "Dog"})
         "dog; label=\"Dog\""))

  (is (= (subj/encode-tag {:term "dog" :label "Dog" :scheme "Canine"})
         "dog; label=\"Dog\"; scheme=\"Canine\""))

  (doseq [[s r] samples]
    (is (= (subj/encode-tags r) s))))

;; build widdle ware
(def ->parse-tags!
  (subj/->parse-tags! identity))

(def test-request
  {:headers {"category" "dog, cat; label=\"Cat\""}})

(deftest ->parse-tags-test
  (is (= (:tags (->parse-tags! test-request))
         [{:term "dog"} {:term "cat" :label "Cat"}]))

  (is (= (:tags (->parse-tags! {})) [])))

(def parse-tags-mv (subj/->parse-tags! identity))
(deftest parse-tags-test
  (let [req {:headers {"category" "dog; label=\"label\"; scheme=\"scheme\""}}
        resp (parse-tags-mv req)
        null-resp (parse-tags-mv {})]
        ;;wrong-req {:headers {"category" "wrong; format=???=...."}}
        ;;wrong-resp (parse-tags-mv wrong-req)

    (is (not (empty? (:tags resp))))
    (is (empty? (:tags null-resp)))
    #_(is (empty? (:tags wrong-resp)))))
