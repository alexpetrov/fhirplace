(ns fhirplace.app
  (:require [ring.util.response :as rur]
            [clojure.string :as cs]
            [fhir :as f]
            [fhirplace.pg :as fp]
            [fhirplace.plugins :as fpl]
            [fhirplace.category :as fc]
            [fhirplace.views :as fv]
            [hiccup.core :as hc]
            [fhir.operation-outcome :as fo]
            [clojure.stacktrace :as cst]
            [clojure.data.json :as json]))

;; TODO merge db here
(defn- get-stack-trace [e]
  (with-out-str (cst/print-stack-trace e)))

(defn cfg  [x]
  (merge x
         {:identifier "http://fhirplace.org"
          :version :todo
          :description "FHIR open source server"
          :name "fhirplace"
          :publisher "fhirplace"
          :date "2014-08-30"
          :software  {:name "fhirplace"
                      :version "0.0.1" }
          :telecom  [{:system "url" :value "http://try-fhirplace.hospital-systems.com/fhirface/index.html#/" }]
          :acceptUnknown false
          :fhirVersion "integration build"
          :format  ["json" "xml"]
          :cors true
          }))

(defn cfg-str  [x] (json/write-str  (cfg x)))

(def outcomes
  {:server-error         500
   :type-not-supported   404
   :resource-not-exists  404
   :resource-not-parsed  400
   :resource-not-valid   422
   :resource-deleted     410
   :not-last-version     412
   :no-version-info      401
   :no-tags              422})

(defmacro  defmw [nm h prms & body]
  `(defn ~(symbol nm) [h#]
     (let [~h h#]
       (fn ~prms
         (println "MW: " ~(name nm))
         ~@body))))

(defn- outcome [error-key text & issues]
  (println "ERROR[" error-key "]" text)
  (let [status (get outcomes error-key)
        issues (or issues [{:severity "fatal" :details text}])]
    {:status status
     :body (fo/operation-outcome
             {:text {:status "generated"
                     :div (str "<div>" text "</div>")}
              :issue issues})}))

(defmw <-outcome-on-exception h [req]
  (try (h req)
       (catch Exception e
         (println "EXEPTION:" (get-stack-trace e))
         (outcome :server-error
                  (str "Unexpected server error " (hc/h (get-stack-trace e)))))))


(defmw ->type-supported! h
  [{{tp :type} :params :as req}]
  (if tp
    (h req)
    (outcome :type-not-supported
             (str "Resource type [" tp "] isn't supported"))))

(defmw ->resource-exists! h
  [{{tp :type id :id } :params cfg :cfg :as req}]
  (if (fp/call* :fhir_is_resource_exists (cfg-str cfg) tp id)
    (h req)
    (outcome :resource-not-exists
             (str "Resource with id: " id " not exists"))))

(defn- safe-parse [x]
  (try
    [:ok (f/parse x)]
    (catch Exception e
      [:error (str "Resource could not be parsed: \n" x "\n" e)])))

;; "parse body and put result as :data"
(defmw ->parse-body! h
  [{bd :body :as req}]
  (let [[st res] (safe-parse (slurp bd)) ]
    (if (= st :ok)
      (h (assoc req :data res))
      (outcome :resource-not-parsed res))))

;"validate :data key for errors"
(defmw ->valid-input! h
  [{res :data :as req}]
  (let [errors (f/errors res)]
    (if (empty? errors)
      (h (assoc req :data res))
      (outcome :resource-not-valid
               "Resource Unprocessable Entity"
               (map #({:severity "fatal" :details (str %)}) errors)))))

(defmw ->check-deleted! h
  [{{tp :type id :id} :params cfg :cfg :as req}]
  (if (fp/call* :fhir_is_deleted_resource (cfg-str cfg) tp id)
    (outcome :resource-deleted (str "Resource " tp " with " id " was deleted"))
    (h req)))

;; TODO: fixme
(defn- check-latest-version [cfg cl]
  (println "check-latest-version " cl)
  (let [[_ cl-] (cs/split cl (re-pattern (:base cfg)))]
    (let [[_ tp id _ vid] (cs/split cl- #"/")]
      (println "check-latest " tp " " id " " vid)
      (fp/call* :fhir_is_latest_resource (cfg-str cfg) tp id vid) )))

(defmw ->latest-version! h
  [{{cl "content-location"} :headers cfg :cfg :as req}]
  (cond
    (not cl) (outcome :no-version-info "Provide 'Content-Location' header for update resource")
    (not (check-latest-version cfg cl)) (outcome :not-last-version "Updating not last version of resource")
    :else (h req)))

(defmw ->check-tags h
  [{tags :tags :as req}]
  (if (seq tags)
    (h req)
    (outcome :no-tags "Expected not empty tags (i.e. Category header)")))


;; ACTIONS

(defn respond [bd]
  {:body bd})

(defn =metadata [{cfg :cfg :as req}]
  (-> (fp/call* :fhir_conformance (cfg-str cfg))
      f/parse
      rur/response))

(defn =profile [{{tp :type} :params cfg :cfg}]
  (-> (fp/call* :fhir_profile (cfg-str cfg) tp)
      f/parse
      rur/response))

(defn =html-face [req]
  (-> (fv/html-face {:plugins (fpl/read-plugins)})
      (rur/response)
      (rur/content-type "text/html; charset=UTF-8")
      (rur/status 200)))

(defn =search [{{tp :type} :params cfg :cfg q :query-string}]
  (-> (fp/call* :fhir_search (cfg-str cfg) tp (or q ""))
      f/parse
      rur/response))

(defn =tags-all [{cfg :cfg}]
  (-> (fp/call* :fhir_tags (cfg-str cfg))
      rur/response))

(defn =resource-type-tags [{{rt :type} :params cfg :cfg}]
  (-> (fp/call* :fhir_tags (cfg-str cfg) rt)
      rur/response))

(defn =resource-tags [{{rt :type id :id} :params cfg :cfg}]
  (-> (fp/call* :fhir_tags (cfg-str cfg) rt id)
      rur/response))

(defn =resource-version-tags [{{rt :type id :id vid :vid} :params cfg :cfg}]
  (-> (fp/call* :fhir_tags (cfg-str cfg) rt id vid)
      rur/response))

(defn =affix-resource-tags [{{tp :type id :id} :params tags :tags cfg :cfg}]
  (fp/call* :fhir_affix_tags (cfg-str cfg) tp id (json/write-str tags)   )
  (-> (fp/call* :fhir_tags (cfg-str cfg) tp id)
      rur/response))

(defn =affix-resource-version-tags [{{tp :type id :id vid :vid} :params tags :tags cfg :cfg}]
  (fp/call* :fhir_affix_tags (cfg-str cfg) tp id vid (json/write-str tags)   )
  (-> (fp/call* :fhir_tags (cfg-str cfg) tp id vid)
      rur/response))

(defn =remove-resource-tags [{{tp :type id :id} :params cfg :cfg}]
  (-> (fp/call* :fhir_remove_tags (cfg-str cfg) tp id)
      (str " tags was removed")
      rur/response))

(defn =remove-resource-version-tags [{{tp :type id :id vid :vid} :params cfg :cfg}]
  (-> (fp/call* :fhir_remove_tags (cfg-str cfg) tp id vid)
      (str " tags was removed")
      rur/response))

(defn =history [{{tp :type id :id} :params cfg :cfg}]
  (-> (fp/call* :fhir_history (cfg-str cfg) tp id "{}")
      f/parse
      rur/response))

(defn =history-type [{{tp :type} :params cfg :cfg}]
  (-> (fp/call* :fhir_history (cfg-str cfg) tp "{}")
      f/parse
      rur/response))

(defn =history-all [{cfg :cfg}]
  (-> (fp/call* :fhir_history (cfg-str cfg) "{}")
      f/parse
      rur/response))

(defn resource-resp [res]
  (let [bundle (json/read-str res :key-fn keyword)
        entry (first (:entry bundle))
        loc (:href (first (:link entry)))
        tags (:category entry)
        last-modified (:updated entry)
        fhir-res (f/parse (json/write-str (:content entry)))]

    (-> {:body fhir-res}
        (rur/header "Location" loc)
        (rur/header "Content-Location" loc)
        (rur/header "Category" (fc/encode-tags tags))
        (rur/header "Last-Modified" last-modified))))

(defn =create
  [{{rt :type} :params res :data tags :tags cfg :cfg :as req}]
  {:pre [(not (nil? res))]}
  (println "=create " (keys req))
  (let [json (f/serialize :json res)
        jtags (json/write-str tags)
        resource-type (str (.getResourceType res))]
    (if (= rt resource-type)
      (-> (fp/call* :fhir_create (cfg-str cfg) resource-type json jtags)
          (resource-resp)
          (rur/status 201)
          (rur/header "Category" (fc/encode-tags tags)))
      (throw (Exception. (str "Wrong resource type '" resource-type "' for '" rt "' endpoint"))))))

(defn =update
  [{{rt :type id :id} :params res :data tags :tags cfg :cfg :as req}]
  {:pre [(not (nil? res))]}
  (let [json (f/serialize :json res)
        jtags (json/write-str tags)
        resource-type (str (.getResourceType res))]
    (if (= rt resource-type)
      (let [cl (get-in req [:headers "content-location"])
            item (fp/call* :fhir_update (cfg-str cfg) rt id cl json jtags)]
        (-> (resource-resp item)
            (rur/status 200)))
      (throw (Exception. (str "Wrong resource type '" resource-type "' for '" rt "' endpoint"))))))

(defn- validate-resource-type
  [cfg rt res]
  (let [json (f/serialize :json res)
        resource-type (str (.getResourceType res))]
    (if (= rt resource-type)
      {:status 200}
      (throw (Exception. (str "Wrong resource type '" resource-type "' for '" rt "' endpoint"))))))

(defn =validate-create
  [{{rt :type} :params res :data tags :tags cfg :cfg}]
  #_{:pre [(not (nil? res))]}
  (validate-resource-type cfg rt res))

(defn =validate-update
  [{{rt :type} :params res :data cfg :cfg}]
  #_{:pre [(not (nil? res))]}
  (validate-resource-type cfg rt res))

(defn =delete
  [{{rt :type id :id} :params body :body cfg :cfg}]
  (-> (fp/call* :fhir_delete (cfg-str cfg) rt id)
      (str)
      (rur/response)
      (rur/status 204)))

(defn =read [{{rt :type id :id} :params cfg :cfg}]
  (-> (fp/call* :fhir_read (cfg-str cfg) rt id)
      (resource-resp)
      (rur/status 200)))

(defn =vread [{{rt :type id :id vid :vid} :params cfg :cfg}]
  (-> (fp/call* :fhir_vread (cfg-str cfg) rt (str id "/_history/" vid))
      (resource-resp)
      (rur/status 200)))

(defn =transaction
  [{bd :body cfg :cfg :as req}]
  (let [bundle (f/parse (slurp bd))
        json (f/serialize :json bundle)]
    (-> (fp/call* :fhir_transaction (cfg-str cfg) json)
        (f/parse)
        (rur/response))))
;; api

(defn =list-apps [req]
  (-> (fpl/read-plugins)
      (json/write-str)
      (rur/response)))

(defn =upload-app [{form :multipart-params :as req}]
  (let [tmpfile (get-in form ["file" :tempfile])
        plugin-name (get form "app")
        res (fpl/upload plugin-name tmpfile)]
    (if (= (:exit res) 0)
      (-> (fpl/read-plugin plugin-name)
          (json/write-str)
          (rur/response))
      {:status 500
       :body (pr-str res)})))

(defn =rm-app [{{nm :app} :params :as req}]
  (let [res (fpl/rm nm)]
    (if (= (:exit res) 0)
      (-> {:status "removed" :name nm :message (str "app [" nm "] successfully removed")}
          (json/write-str)
          (rur/response))
      {:status 500
       :body (pr-str res)})))
