(ns fhirplace.conformance
  (:require [clj-time.core :as t]
            [fhirplace.core :as core]))

(defn- build-resource [resource]
  {:type resource
   :readHistory false
   :updateCreate true
   :searchInclude []
   :operation (mapv (fn [i] {:type i})
                ["read" "update" "create" "delete" "validate" "search-type"])})

(defn- build-rest [resources]
  [{:mode "server"
    :documentation "Blah"
    :security {:service [{ :text "HTTP Digest" }]
               :descrition "Use plain simple HTTP auth"}
    :resources (mapv build-resource resources)}])

(defn build-conformance
  "Returns Conformance resource describing this FHIRPlace server"
  [resources]
  {:resourceType "Conformance"
   :name "FHIRPlace"
   :publisher "Health Samurai Developers"
   :telecom [{:system "url" :value "http://healthsamurai.github.io"}]
   :description "Open Source FHIR server written in Clojure"
   :date (t/local-date 2013 3 20)
   :fhirVersion "DSTU"
   :acceptUnknown true
   :format ["json" "xml"]
   :software {:name "FHIRPlace"
              :version (:version core/project)}
   :rest (build-rest resources)})

