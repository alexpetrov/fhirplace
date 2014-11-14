(ns fhirplace.plugins
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as cjs]
    [clojure.string :as cs]
    [fhirplace.shell :as fs]
    [clojure.data.json :as json])
  (:import [java.net.URL]))

(defn -plugins [acc file]
  (let [plug (-> (io/reader file)
                 (java.io.PushbackReader.)
                 (json/read-json true))]
    (assoc acc (:name plug) (merge plug {:dir (.getName (.getParentFile file))}))))


(def base-path (.getPath (io/resource "public")))

(defn plugin-path [nm]
  (-> (str base-path "/" nm)
      (io/file)
      (.getPath)))

(defn read-plugin [nm]
  (let [manifest-file (-> (plugin-path nm) (str "/fhir.json") (io/file))]
    (->
      (if (.exists manifest-file)
        (try
          (->  manifest-file
              (io/reader)
              (java.io.PushbackReader.)
              (json/read-json true))
          (catch Exception e
            {:name nm :title nm :description (str "Error while reading manifest " e)}))
        {:name nm :title nm :description "plugin without manifest"})
      (merge {:url (str "/" nm "/index.html")}))))

(defn read-plugins []
  (->> (io/resource "public")
       (io/file)
       (.listFiles)
       (filter #(.isDirectory %))
       (map #(.getName  %))
       (map #(read-plugin %))))

(defn url [s] (java.net.URL. s))

(defn rm [nm]
  (let [plugin-path (plugin-path nm)
        cmd (fs/shell [:rm :-rf plugin-path])]
    (println cmd)
    (cjs/sh "bash" "-c" cmd)))

;; TODO support for zip
(defn upload [nm tmpfile]
  (let [tar-path (.getPath tmpfile)
        plugin-path (plugin-path nm)
        cmd (fs/shell
              [:and
               [:rm :-rf plugin-path]
               [:mkdir :-p plugin-path]
               [:cd plugin-path]
               [:tar :-xzf tar-path]
               [:ls :-lah]])]
    (println cmd)
    (cjs/sh "bash" "-c" cmd)))

(comment
  (read-plugins)
  (read-plugin "test")

  (def tmpfile (io/file "/home/devel/fhirplace-empty-plugin/arch.tar.gz"))
  (rm "test")
  (println (upload "test" tmpfile)))
