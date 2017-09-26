(ns opticlj.writer
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [opticlj.file :as file])
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

(defn concat-result [output result]
  (if (string? result)
    (vec (concat ["\""] (str/split (str/replace result "\"" "") #"\n") ["\""]))
    [(pp/write result :stream nil) ""]))

(defn form-output-stream [sym form result]
  (str/join "\n" (concat-result [(str "(in-ns '" (namespace sym) ")") ""
                                 (pp/write form :stream nil) ""]
                                result)))

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

(defn write [path sym form result]
  (let [output   (form-output-stream sym form result)
        err-path (file/err-path path)]
    (merge {:form form :result result :sym sym}
     (if-let [diff (and (file/exists path) (file/diff path err-path output))]
       (do (file/write err-path output)
           (err-optic path err-path diff))
       (do (file/write path output)
           (file/delete err-path)
           (optic path))))))
