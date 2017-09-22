(ns opticlj.core
  (:require [clojure.java.shell :as shell :refer [sh]]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [java.io File ByteArrayOutputStream OutputStreamWriter Writer]))

;; Manage diff display

(defrecord Diff [string])

(defmethod print-method Diff [v ^Writer w]
  (.write w "{:string <truncated>}"))

;; Output stream writer

(defn- form-output-stream [ns- form result]
  (let [baos (ByteArrayOutputStream.)
        writer (OutputStreamWriter. baos)]
    (.write writer (str "(in-ns '" ns- ")"))
    (.write writer "\n\n")
    (pp/write form :stream writer)
    (.write writer "\n\n")
    (pp/write result :stream writer)
    (.write writer "\n")
    (.close writer)
    baos))

;; File utils

(defn- build-filepath [ns- sym]
  (let [ns-str   (str/replace (str ns-) #"-" "_")
        sym-file (str/replace (name sym) #"-" "_")
        path-vec (str/split ns-str #"\.")]
    (str/join "/" (conj path-vec (str sym-file ".clj")))))

(defn- build-file [dir path]
  (doto (File. dir path)
    (.. getParentFile mkdirs)))

(defn- diff-optic [file-obj output]
  (let [in (.toByteArray output)
        diff (sh "diff" "-u" (.getPath file-obj) "-" :in in)]
    (when (seq (:out diff))
      diff)))

;; State

(def system (atom {:dir "test/__optic__" :optics {}}))

;; Test checker

(defn err-filename [file-obj]
  (str/replace (.getPath file-obj) #"\.clj$" ".err.clj"))

(defn- write-optic [ns- sym file-obj form result]
  (let [output (form-output-stream ns- form result)
        err-file-obj (File. (err-filename file-obj))]
    (merge
     (if-let [err (and (.exists file-obj) (diff-optic file-obj output))]
       (do (spit err-file-obj (.toString output))
           {:file (.getPath file-obj) :passing? false :diff (->Diff (:out err))
            :err-file (.getPath err-file-obj)})
       (do (spit file-obj (.toString output))
           (when (.exists err-file-obj) (.delete err-file-obj))
           {:file (.getPath file-obj) :passing? true :diff nil :err-file nil}))
     {:form form :result result :sym sym :ns ns-})))

;; Library exports

(defmacro defoptic [sym form & {:keys [dir]}]
  `(let [ns-#      *ns*
         filepath# (build-filepath ns-# '~sym)
         file-obj# (build-file (or ~dir (:dir @system)) filepath#)
         ns-sym#   (symbol (str ns-#) (name '~sym))]
     (defn ~sym []
       (let [optic# (write-optic ns-# ns-sym# file-obj# '~form ~form)]
         (swap! system update :optics assoc ns-sym# optic#)
         optic#))
     (~sym)
     ~sym))

(defn error [sym]
  (some-> (get-in @system [:optics sym])
          :diff :string println))

(defn errors []
  (run! error (keys (:optics @system))))

(defn adjust! [sym]
  (if-let [{:keys [err-file file]} (get-in @system [:optics sym])]
    (when (and err-file file)
      (.renameTo (File. err-file) (File. file))
      {:adjusted ((resolve sym))})
    {:failure (str "Could not find `" sym "` in defined optics")}))

(defn adjust-all! []
  (filter identity (map adjust! (keys (:optics @system)))))

(defn review!* [f exceptions]
  (try ((resolve f))
       (catch Exception e (swap! exceptions conj e))))

(defn review! []
  (let [exceptions (atom [])]
    (run! #(review!* % exceptions) (keys (:optics @system)))
    (let [optics (:optics @system)
          passed (count (filter :passing? (vals optics)))
          failed (- (count optics) passed)]
     (errors)
     {:passed passed :failed failed :exceptions (count @exceptions)})))

(defn remove! [& syms]
  (doseq [sym syms]
    (let [{:keys [file err-file]} (get-in @system [:optics sym])]
      (when file (.delete (File. file)))
      (when err-file (.delete (File. err-file)))))
  (apply swap! system dissoc :optics syms)
  (review!))

(defn clear! []
  (apply remove! (keys (:optics @system))))

(defn set-dir! [dir]
  (swap! system assoc :dir dir))

;;;; Temporary initial optic

(defoptic error-filename-regex
  [(err-filename (File. "foo.clj"))
   (err-filename (File. "foo-bar-baz..clj"))])
