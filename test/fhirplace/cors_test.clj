(ns fhirplace.cors-test
  (:require
    [clojure.test :refer :all]
    [fhirplace.cors :as c]))


(def cors-options-request
  {:request-method :options
   :headers {"access-control-request-headers" "headers"
             "access-control-request-method" "method"}})

(deftest test-cors
  (is
    (= (c/cors-options cors-options-request)
       {:status 200
        :body "preflight complete"
        :headers {"Access-Control-Allow-Headers" "headers"
                  "Access-Control-Allow-Methods" "method"}}))
  (is
    (=
     (c/allow "orig" {:donttach :ups :headers {"donottach" "ups"}})
     {:donttach :ups
      :headers
      {"donottach" "ups"
       "Access-Control-Allow-Origin" "orig"
       "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type"}}))


  (is (=
       ((c/<-cors identity) {:request-method :options
                             :headers {"origin" "orig"}})
       {:status 200
        :body "preflight complete"
        :headers {"Access-Control-Allow-Headers" nil
                  "Access-Control-Allow-Origin" "orig"
                  "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type"
                  "Access-Control-Allow-Methods" nil}}))

  (is (=
       ((c/<-cors identity) {:request-method :post
                             :headers {"origin" "orig"}})
       {:request-method :post
        :headers {"origin" "orig"
                  "Access-Control-Allow-Origin" "orig"
                  "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type"}})))
