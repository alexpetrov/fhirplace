(ns fhirplace.format
  (:require
    [fhir :as f]
    [clojure.string :as cs]))

(defn mime
  [content-type]
  (first (cs/split content-type #"\;")))

(def formats
  {"json" :json
   "application/json" :json
   "application/json+fhir" :json
   "xml" :xml
   "text/xml" :xml
   "application/xml" :xml
   "application/atom+xml" :xml
   "application/xml+fhir" :xml})

(def response-formats
  {:atom "application/atom+xml"
   :xml "application/xml+fhir"
   :json "application/json+fhir" })


(def default-format :json)

(defn get-format [req]
  (let [fmt          (get-in req [:params :_format])
        content-type (get-in req [:headers "content-type"])
        mime         (and content-type (mime content-type))]
    (or (get formats fmt)
        (get formats mime)
        default-format)))

(defn content-type-format
  [fmt bd]
  (-> (or (and (f/atom? bd) (= :xml fmt) (:atom response-formats))
          (get response-formats fmt))
      (str "; charset=UTF-8")))

(defn response-content-type
  [resp fmt body]
  (update-in resp [:headers]
             merge {"content-type" (content-type-format fmt body)}))


(defn <-format [h]
  "formatting midle-ware
  read incoming content-type
  inspect body and if it is instance of fhir reference impl
  use fhir java searivalizer"
  (fn [req]
    (let [fmt (get-format req)
          {bd :body :as resp} (h req)
          bd (if (f/serializable? bd)(f/serialize fmt bd) bd)]
      #_(println "Formating " fmt ": " bd "")
      (-> (assoc resp :body bd)
          (response-content-type fmt bd)))))
