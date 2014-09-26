(ns user
  (:require [fhirplace.core :as fc]))

(def srv (fc/start-server))
