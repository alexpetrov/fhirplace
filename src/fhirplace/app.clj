(ns fhirplace.app
  (:use ring.util.response
        ring.util.request)
  (:require [compojure.core :as cc]
            [compojure.route :as cr]
            [compojure.handler :as ch]
            [clojure.string :as cs]
            [fhir :as f]
            [fhir.operation-outcome :as fo]
            [fhirplace.db :as db]
            [ring.adapter.jetty :as jetty]))

(import 'org.hl7.fhir.instance.model.Resource)
(import 'org.hl7.fhir.instance.model.AtomFeed)

(defn url [& parts]
  (apply str (interpose "/" parts)))

(defn- determine-format
  "Determines request format (:xml or :json)."
  [{{fmt :_format} :params}]
  (or (get {"application/json" :json
            "application/xml"  :xml} fmt)
      :json))

(defn <-format [h]
  "formatting midle-ware
  expected body is instance of fhir reference impl"
  (fn [req]
    (let [{bd :body :as resp} (h req)
          fmt (determine-format req)]
      ;; TODO set right headers
      (println "Formating: " bd)
      (if (and bd (or (instance? Resource bd) (instance? AtomFeed bd)))
        (assoc resp :body (f/serialize fmt bd))
        resp))))

(defn- get-stack-trace [e]
  (let [sw (java.io.StringWriter.)]
    (.printStackTrace e (java.io.PrintWriter. sw))
    (println "ERROR: " sw)
    (str sw)))

(defn- outcome [status text & issues]
  {:status status
   :body (fo/operation-outcome
           {:text {:status "generated" :div (str "<div>" text "</div>")}
            :issue issues })})

(defn <-outcome-on-exception [h]
  (fn [req]
    (println "<-outcome-on-exception")
    (try
      (h req)
      (catch Exception e
        (println "Exception")
        (println (get-stack-trace e))
        (outcome 500 "Server error"
                 {:severity "fatal"
                  :details (str "Unexpected server error " (get-stack-trace e))})))))


(defn ->type-supported! [h]
  (fn [{{tp :type} :params :as req}]
    (println "TODO: ->type-supported!")
    (if tp
      (h req)
      (outcome 404 "Resource type not supported"
               {:severity "fatal"
                :details (str "Resource type [" tp "] isn't supported")}))))

(defn ->resource-exists! [h]
  (fn [{{tp :type id :id } :params :as req}]
    (println "->resource-exists!")
    (if (db/-resource-exists? tp id)
      (h req)
      (outcome 404 "Resource not exists"
               {:severity "fatal"
                :details (str "Resource with id: " id " not exists")}))))

;; TODO: move to fhir f/errors could do it
(defn- safe-parse [x]
  (try
    [:ok (f/parse x)]
    (catch Exception e
      [:error (str "Resource could not be parsed: \n" x "\n" e)])))

(defn ->parse-body!
  "parse body and put result as :data"
  [h]
  (fn [{bd :body :as req}]
    (println "->parse-body!")
    (let [[st res] (safe-parse (slurp bd)) ]
      (if (= st :ok)
        (h (assoc req :data res))
        (outcome 400 "Resource could not be parsed"
                 {:severity "fatal"
                  :details res})))))

(defn ->valid-input! [h]
  "validate :data key for errors"
  (fn [{res :data :as req}]
    (println "->valid-input!")
    (let [errors (f/errors res)]
      (if (empty? errors)
        (h (assoc req :data res))
        (apply outcome 422
               "Resource Unprocessable Entity"
               (map
                 (fn [e] {:severity "fatal"
                          :details (str e)})
                 errors))))))

(defn ->check-deleted! [h]
  (fn [{{tp :type id :id} :params :as req}]
    (println "->check-deleted!")
    (if (db/-deleted? tp id)
      (outcome 410 "Resource was deleted"
               {:severity "fatal"
                :details (str "Resource " tp " with " id " was deleted")})
      (h req))))

(defn- check-latest-version [cl]
  (println "check-latest-version " cl)
  (let [[tp id vid] (cs/split cl #"/")]
    (println "check-latest " tp " " id " " vid)
    (db/-latest? tp id vid)))

(defn ->latest-version! [h]
  (fn [{{tp :type id :id} :params :as req}]
    (println "->latest-version!")
    (if-let [cl (get-in req [:headers "content-location"])]
      (if (check-latest-version cl)
        (h req)
        (outcome 412 "Updating not last version of resource"
                 {:severity "fatal"
                  :details (str "Not last version")}))

      (outcome 401 "Provide 'Content-Location' header for update resource"
               {:severity "fatal"
                :details (str "No 'Content-Location' header")}))))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")


(defn =metadata [req]
  {:body (f/conformance)})

(defn =search [{{rt :type} :params}]
  {:body (db/-search rt)})

(defn =history [{{rt :type id :id} :params}]
  {:body (db/-history rt id)})

(defn resource-resp [res]
  ( let [fhir-res (f/parse (:data res))]
    (-> {:body fhir-res}
      (header "Location" (url (.getResourceType fhir-res) (:logical_id res) (:version_id res)))
      (header "Content-Location" (url (.getResourceType fhir-res) (:logical_id res) (:version_id res)))
      (header "Last-Modified" (:last_modified_date res)))))

(defn =create
  [{{rt :type} :params res :data :as req}]
  #_{:pre [(not (nil? res))]}
  (println "=create " (keys req))
  (let [json (f/serialize :json res)
        item (db/-create (str (.getResourceType res)) json)]
    (-> (resource-resp item)
        (status 201))))

(defn =update
  [{{rt :type id :id} :params res :data}]
  {:pre [(not (nil? res))]}
  (let [json (f/serialize :json res)
        item (db/-update rt id json)]
    (-> (resource-resp item)
        (status 200))))

(defn =delete
  [{{rt :type id :id} :params body :body}]
  (-> (response (str (db/-delete rt id)))
      (status 204)))

;;TODO add checks
(defn =read [{{rt :type id :id} :params}]
  (let [res (db/-read rt id)]
    (-> (resource-resp res)
        (status 200))))

(defn =vread [{{rt :type id :id vid :vid} :params}]
  (let [res (db/-vread rt id vid)]
    (println res)
    (-> (resource-resp res)
        (status 200))))
