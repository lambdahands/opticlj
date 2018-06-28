(ns opticlj.writer
  (:require [clojure.string :as str]
            [opticlj.file :as file]
            [zprint.core :refer [zprint-str]])
  #?(:clj (:import [java.io Writer])))

;; Manage diff display

(defrecord Diff [string])

#?(:clj (defmethod print-method Diff [v ^Writer w]
          (.write w "{:string <truncated>}")))

#?(:cljs (extend-protocol IPrintWithWriter
           Diff
           (-pr-writer [_ w _]
             (write-all w "{:string <truncated>}"))))

;; Output stream writer

(defn fmt-result [result]
  (if (string? result)
    (str/split (zprint-str result) #"\\n")
    [(zprint-str result)]))

(defn form-output-stream [kw form result]
  (str/join "\n" (concat [(str "(in-ns '" (namespace kw) ")") ""
                          (zprint-str form) ""]
                         (fmt-result result)
                         [""])))

;; Optic data

(defn err-optic [path err-path diff]
  {:file     path
   :err-file err-path
   :diff     (->Diff diff)
   :passing? false})

(defn optic [path]
  {:file     path
   :err-file nil
   :diff     nil
   :passing? true})

;; Test checker

(defn write [path kw form result]
  (let [output   (form-output-stream kw form result)
        err-path (file/err-path path)]
    (merge {:form form :result result :kw kw}
     (if-let [diff (and (file/exists path) (file/diff path err-path output))]
       (do (file/write err-path output)
           (err-optic path err-path diff))
       (do (file/write path output)
           (file/delete err-path)
           (optic path))))))
