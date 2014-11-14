(ns fhirplace.cors)

(defn cors-options [{meth :request-method hs :headers}]
  (when (= :options meth)
    (let [headers (get hs "access-control-request-headers")
          method  (get hs "access-control-request-method")]
      (println "CORS:\n\tRequest-Headers:" headers "Request-Method" methods)
      {:status 200
       :body "preflight complete"
       :headers {"Access-Control-Allow-Headers" headers
                 "Access-Control-Allow-Methods" method}})))

(defn allow [origin resp]
  (println "CORS allowed for " origin)
  (merge-with
    merge resp
    {:headers
     {"Access-Control-Allow-Origin" origin
      "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type"}}))

(defn cors-origins
  "May check if allow CORS access here"
  [{hs :headers}]
  (get hs "origin"))

(defn <-cors [h]
  "Cross-origin resource sharing midle-ware"
  (fn [req]
    (if-let [origin (cors-origins req)]
      (allow origin (or (cors-options req)
                        (h req)))
      (h req))))

