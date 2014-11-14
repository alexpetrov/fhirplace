(ns fhirplace.shell
  (:require  [clojure.string :as cs]))

(declare shell)

(defn opts  [x]
  (cs/join " "
           (map  (fn  [[k v]]
                   (str  (shell k) " "  (shell v) " ")  )
                x)))
(def ops
  {:and " && "
   :or " && "})

(defn cmd  [x]
  (cond
    (contains? ops  (first x))  (cs/join  (get ops  (first x))  (map shell  (rest x)))
    :else  (cs/join " "  (map shell x))))

(defn shell  [x]
  (cond
    (string? x) x
    (keyword? x)  (name x)
    (map? x)  (opts x)
    (vector? x)  (cmd x)))
