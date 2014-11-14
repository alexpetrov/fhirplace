(ns fhirplace.core
  (:require [route-map :as rm]
            [compojure.handler :as ch]
            [ring.middleware.file :as rmf]
            [fhirplace.app :refer :all]
            [fhirplace.format :as ff]
            [fhirplace.cors :as fc]
            [fhirplace.routes :as fr]
            [ring.adapter.jetty :as jetty]
            [environ.core :as env]
            [clojure.string :as cs]))

(defn build-stack
  "wrap h with middlewares mws"
  [h mws]
  ((apply comp mws) h))

(defn base-url [{:keys [scheme server-name server-port]}]
  (str (name scheme) "://"
       server-name
       (if (= server-port 80)
         "" (str ":" server-port))))

(defn wrap-cfg
  "wrap with config; extract base url from request"
  [h]
  (fn [req] (h (assoc req :cfg {:base (base-url req)}))))

(defn match-route [meth path]
  (rm/match [meth path] fr/routes))

(defn resolve-route [h]
  (fn [{uri :uri meth :request-method :as req}]
    (if-let [route (match-route meth uri)]
      (h (assoc req :route route))
      {:status 404 :body (str "No route " meth " " uri)})))

(defn collect-mw [match]
  (->> (conj (:parents match) (:match match))
       (mapcat :mw)
       (filterv (complement nil?))))

(defn dispatch [{handler :handler route :route :as req}]
  (let [mws     (collect-mw route)
        handler (get-in route [:match :fn])
        req     (update-in req [:params] merge (:params route))]
    (println "\n\nDispatching " (:request-method req) " " (:uri req) " to " (pr-str handler))
    (println "Middle-wares: "   (pr-str mws))
    ((build-stack handler mws) req)))

(def app (-> dispatch
             (resolve-route)
             (ff/<-format)
             (wrap-cfg)
             (fc/<-cors)
             (ch/site)
             (rmf/wrap-file "resources/public")))

(defn start-server []
  (jetty/run-jetty #'app {:port (env/env :fhirplace-web-port) :join? false}))

(defn stop-server [server] (.stop server))
