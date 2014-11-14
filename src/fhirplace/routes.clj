(ns fhirplace.routes
  (:require
    [fhirplace.app :refer :all]
    [fhirplace.category :refer [->parse-tags!]]))

(defn h
  "mk handler hash by convention"
  [& hnds]
  {:fn (last hnds)
   :mw (into [] (butlast hnds))})

;; /api
(def api
  {"app" {:GET (h #'=list-apps)
          :POST (h #'=upload-app)
          [:app] {:DELETE  (h #'=rm-app)}}})

;; /Patient/:id/_history/_tags
(def instance-hx-tag-routes
  {:GET       (h #'=resource-version-tags)
   :POST      (h #'->parse-tags!
                 #'->check-tags
                 #'=affix-resource-version-tags)
   "_delete" (:POST (h #'=remove-resource-version-tags))})

;; /Patient/:id/_history
(def instance-hx-routes
  {:GET     (h #'=history)
   "_tags"  instance-hx-tag-routes
   [:vid]   {:GET    (h #'=vread)
             "_tags" {:GET (h #'=resource-version-tags)}}})

;; /Patient/:id/_tags
(def instance-tag-routes
  {:GET      (h #'=resource-tags)
   :POST     (h #'->parse-tags!
                #'->check-tags
                #'=affix-resource-tags)
   "_delete" {:POST (h #'=remove-resource-tags)}})

;; /Patient/:id/
(def instance-level-routes
  {:mw [#'->resource-exists! #'->check-deleted!]
   :GET       (h #'=read)
   :DELETE    (h #'=delete)
   :PUT       (h #'->parse-tags!
                 #'->parse-body!
                 #'->latest-version!
                 #'->valid-input!
                 #'=update)
   "_tags"    instance-tag-routes
   "_history" instance-hx-routes })

;; /Patient/_validate
(def validate-routes
  {:mw [#'->parse-body! #'->valid-input!]
   :POST (h #'->parse-tags! #'=validate-create)
   [:id] {:POST (h #'->latest-version! #'=validate-update)}})

;; /Patient/
(def type-level-routes
  {:mw [#'->type-supported!]
   :POST       (h #'->parse-tags!
                  #'->parse-body!
                  #'->valid-input!
                  #'=create)
   :GET        (h #'=search)
   "_search"   {:GET (h #'=search)}
   "_tags"     {:GET (h #'=resource-type-tags)}
   "_history"  {:GET (h #'=history-type)}
   "_validate" validate-routes
   [:id]       instance-level-routes})

;; /
(def routes
  {:mw [#'<-outcome-on-exception]
   "api" api
   :GET        (h #'=html-face)
   :POST       (h #'=transaction)
   "metadata" {:GET (h #'=metadata)}
   "_tags"    {:GET (h #'=tags-all)}
   "_history" {:GET (h #'=history-all)}
   "Profile"  {[:type] {:GET (h #'=profile)}}
   [:type]    type-level-routes})
