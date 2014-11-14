(ns fhir
  (:require
    [fhir.conv :as fc]
    [fhir.validation :as fv]
    [cheshire.core :as cc]
    [fhir.bundle :as fb]
    [fhir.profiles :as fp]))

(import 'org.hl7.fhir.instance.model.Resource)
(import 'org.hl7.fhir.instance.model.AtomFeed)


(def re-xml #"(?m)^<.*>")
(def re-json #"(?m)^[{].*")

(defn parse
  "parse xml or json string,if not throws error"
  [x]
  (cond
    (re-seq re-xml x) (fc/from-xml x)
    (re-seq re-json x) (fc/from-json x)
    :else (throw (Exception. "Don't know how to parse: " (pr-str x)))))

(defn atom? [res]
   (instance? AtomFeed res))

(defn serializable? [x]
  (and x
       (or (instance? Resource x)
           (instance? AtomFeed x))))

(defn serialize [fmt x]
  (cond
    (= fmt :xml) (fc/to-xml x)
    (= fmt :json) (fc/to-json x)))

(defn errors [x]
  (fv/errors x))

(defn conformance []
  fp/conformance)

(defn profile
  "return clojure representation of resource profile"
  [res-type]
  (cc/parse-string
    (serialize :json
               (.getResource
                 (fp/profile res-type)))
    true))

(defn profile-resource
  "return fhir representation of resource profile"
  [res-type]
  (fp/profile-resource res-type))

(defn bundle
  "build bundle from hash-map with entry :content parsed to fhir.model.Resource"
  [attrs]
  (fb/bundle attrs))
