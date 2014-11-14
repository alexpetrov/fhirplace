(ns fhirplace.category
  (:require [clojure.string :as cs]))

;; Dummy impl http://tools.ietf.org/html/draft-johnston-http-category-header-02
; The Category entity-header provides a means for serialising one or
;    more categories in HTTP headers.  It is semantically equivalent to
;    the atom:category element in Atom [RFC4287].

;    Category           = "Category" ":" #category-value
;    category-value     = term *( ";" category-param )
;    category-param     = ( ( "scheme" "=" <"> scheme <"> )
;                       | ( "label" "=" quoted-string )
;                       | ( "label*" "=" enc2231-string )
;                       | ( category-extension ) )
;    category-extension = token [ "=" ( token | quoted-string ) ]
;    enc2231-string     = <extended-value, see [RFC2231], Section 7>
;    term               = token
;    scheme             = URI

;    Each category-value conveys exactly one category but there may be
;    multiple category-values for each header-field and/or multiple
;    header-fields per [RFC2616].

;    Note that schemes are REQUIRED to be absolute URLs in Category
;    headers, and MUST be quoted if they contain a semicolon (";") or
;    comma (",") as these characters are used to separate category-params
;    and category-values respectively.

;    The "label" parameter is used to label the category such that it can
;    be used as a human-readable identifier (e.g. a menu entry).
;    Alternately, the "label*" parameter MAY be used encode this label in
;    a different character set, and/or contain language information as per
;    [RFC2231].  When using the enc2231-string syntax, producers MUST NOT
;    use a charset value other than 'ISO-8859-1' or 'UTF-8'.


(defn parse-pair
  "should parse [key = value]"
  [x]
  (let [[k v] (cs/split x #"\s?=\s?")]
    [(keyword (cs/trim k))
     (-> (cs/trim v)
         (cs/replace  #"(^\"|\"$)" ""))]))

(defn parse-tag [x]
  (->> (cs/split (str "term=" x) #";")
       ;;        ^ small hack :)
       (map cs/trim)
       (mapv parse-pair)
       (into {})))

(defn parse [x]
  (->> (cs/split x #",")
       (map cs/trim)
       (mapv parse-tag)))

(defn- safe-parse [x]
  (try (parse x)
       (catch Exception e
         (println "WARN: Tags could not be parsed: \n" x "\n" e)
         [])))

(defn- kv-to-str [k v] (str k "=\"" v "\""))

(defn encode-tag [tag]
  (->> (for [k [:label :scheme] :when (k tag)]
         (kv-to-str (name k) (get tag k)))
       (into [(:term tag)])
       (cs/join "; ")))

(defn encode-tags [tags]
  (println "TAGS--" tags)
  (->> tags
       (map encode-tag)
       (cs/join ", ")))

(defn ->parse-tags!
  "parse tags and assoc them as :tags key"
  [h]
  (fn [{{cat "category"} :headers :as req}]
    (println "->parse-tags!")
    (h (assoc req :tags (safe-parse cat)))))

