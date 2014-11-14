(ns fhirplace.external-test
  (:require [fhirplace.core :as fc]
            [fhir :as f]
            [clojure.test :refer :all]
            [plumbing.core :refer [fnk]]
            [plumbing.graph :as pg]
            [clj-http.client :as cc]
            [environ.core :as env]
            [clojure.string :as cs]))

; (import 'org.hl7.fhir.instance.model.Conformance)
; (import 'org.hl7.fhir.instance.model.AtomFeed)
; (import 'org.hl7.fhir.instance.model.Alert)
; (import 'org.hl7.fhir.instance.model.Patient)
; (import 'org.hl7.fhir.instance.model.Profile)
; (import 'org.hl7.fhir.instance.model.OperationOutcome)

; (defmacro def-scenario [nm m]
;   `(def ~nm  (pg/lazy-compile ~m)))

; (def base-url (env/env :fhirplace-test-url))

; (defn url [& parts]
;   (apply str base-url "/" (interpose "/" parts)))

; (defn fixture [nm]
;   (slurp (str "test/fhirplace/fixtures/" nm)))

; (defn GET [url]
;   (println "GET: " url)
;   (cc/get url {:throw-exceptions false}))

; (defn POST [url attrs]
;   (println "POST: " url)
;   (cc/post url (merge {:throw-exceptions false}  attrs)))

; (defn PUT [url attrs]
;   (println "PUT: " url)
;   (cc/put url (merge {:throw-exceptions false}  attrs)))

; (defn DELETE [url]
;   (println "DELETE: " url)
;   (cc/delete url (merge {:throw-exceptions false})))

; (defn get-header
;   [h res]
;   (get-in res [:headers h]))

; (defn mime-type
;   [fmt]
;   ({"xml" "application/xml+fhir"
;     "json" "application/json+fhir"} fmt))

; (defn get-resource-ids [content-loc]
;   (let [[vid id rt] (reverse (cs/split content-loc #"/"))]
;     [rt id vid]))

; (def-scenario simple-crud
;   {:metadata (fnk [] (GET (url "metadata")))
;    :conformance (fnk [metadata] (f/parse (:body metadata)))

;    :pt_profile (fnk [] (GET (url "Profile" "Patient")))

;    :search (fnk [] (GET (url "Patient" "_search")))
;    :search_atom (fnk [search] (f/parse (:body search)))

;    :tags (fnk [] "cat; scheme=\"http://hl7.org/fhir/tag\"; label=\"Cato\"")
;    :new_resource (fnk [tags] (POST
;                                (url "Patient")
;                                {:headers {"Category" tags} :body (fixture "patient.json")}))
;    :new_resource_loc (fnk [new_resource] (get-header "Content-Location" new_resource))

;    :get_new_resource (fnk [new_resource_loc] (GET new_resource_loc))

;    :update_resource  (fnk [new_resource_loc]
;                           (let [[rt id vid] (get-resource-ids new_resource_loc)]
;                             (PUT (url rt id)
;                                  {:headers {"Content-Location" new_resource_loc}
;                                   :body    (fixture "patient.json")})))

;    :updated_version  (fnk [update_resource]
;                           (GET (get-header "Content-Location" update_resource)))


;    :history_of_resource  (fnk [update_resource]
;                               (let [[rt id vid] (get-resource-ids (get-header "Content-Location" update_resource))]
;                                 (GET (url rt id "_history"))))

;    :delete_resource  (fnk [new_resource_loc]
;                           (let [[rt id vid] (get-resource-ids new_resource_loc)]
;                             (DELETE (url rt id))))
;    })

; (def subj (simple-crud {}))

; (defn status? [status response]
;   (is (= (:status response) status)))

; (deftest test-simple-crud
;    (status? 200 (:metadata subj))
;    (is (instance? Conformance (:conformance subj)))

;    (status? 200 (:pt_profile subj))
;    (is (instance? Profile
;                   (f/parse (:body (:pt_profile subj)))))

;    (status? 200 (:search subj))
;    (is (instance? AtomFeed (:search_atom subj)))

;    (status? 201 (:new_resource subj))

;    (is (not (nil? (:new_resource_loc subj))))

;    (status? 200 (:get_new_resource subj))

;    (is (= (get-in (:get_new_resource subj) [:headers "Category"])
;           (:tags subj)))

;    (status? 200 (:update_resource subj))

;    (status? 200 (:updated_version subj))

;    (is (instance? Patient
;                   (f/parse (:body (:updated_version subj)))))


;    (status? 200 (:history_of_resource subj))

;    (is (instance? AtomFeed
;                   (f/parse (:body (:history_of_resource subj)))))

;    #_(status? 204 (:delete_resource subj))
;    )
