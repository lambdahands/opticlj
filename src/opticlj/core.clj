(ns opticlj.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [java.io ByteArrayOutputStream OutputStreamWriter Writer
            BufferedReader StringReader FileReader]
           [difflib DiffUtils]))

;; Manage diff display

(defrecord Diff [string])

(defmethod print-method Diff [v ^Writer w]
  (.write w "{:string <truncated>}"))

;; Output stream writer

(defn form-output-stream [sym form result]
  (str/join "\n" [(str "(in-ns '" (namespace sym) ")") ""
                  (pp/write form :stream nil) ""
                  (pp/write result :stream nil) ""]))

;; File utils

(def file-match #"(\.err\.clj$|\.clj$)")

(defn sym->filepath [sym]
  (let [ns-str   (str/replace (namespace sym) #"-" "_")
        sym-file (str/replace (name sym) #"-" "_")
        path-vec (str/split ns-str #"\.")]
    (str/join "/" (conj path-vec (str sym-file ".clj")))))

(defn filepath->sym [filepath prefix]
  (let [subpath (str/replace filepath (re-pattern (str "^" prefix "?/")) "")
        tokens  (str/split (str/replace subpath #"_" "-")  #"/")
        symname (str/replace (last tokens) file-match "")
        ns-path (str/join "." (butlast tokens))]
    (symbol (str ns-path "/" symname))))

(defn dir-syms [dir]
  (->> (map str (file-seq (io/file dir)))
    (filter #(re-find file-match %))
    (map (fn [s] [(filepath->sym s dir) s]))
    (into {})))

(defn build-file [dir path]
  (doto (io/file dir path)
    (.. getParentFile mkdirs)))

(defn diff-optic [file-obj output]
  (let [filename   (.getPath file-obj)
        file-lines (line-seq (BufferedReader. (FileReader. filename)))
        oput-lines (line-seq (BufferedReader. (StringReader. output)))
        file-diff  (DiffUtils/diff file-lines oput-lines)
        unified    (DiffUtils/generateUnifiedDiff filename
                                                  (err-filename file-obj)
                                                  file-lines
                                                  file-diff
                                                  3)]
    (when (seq unified)
      {:out (str/join "\n" unified)})))

;; Test checker

(defn err-filename [file-obj]
  (str/replace (.getPath file-obj) #"\.clj$" ".err.clj"))

(defn write-optic [file-obj sym form result]
  (let [output (form-output-stream sym form result)
        err-file-obj (io/file (err-filename file-obj))]
    (merge
     (if-let [err (and (.exists file-obj) (diff-optic file-obj output))]
       (do (spit err-file-obj output)
           {:file (.getPath file-obj) :passing? false :diff (->Diff (:out err))
            :err-file (.getPath err-file-obj)})
       (do (spit file-obj output)
           (when (.exists err-file-obj) (.delete err-file-obj))
           {:file (.getPath file-obj) :passing? true :diff nil :err-file nil}))
     {:form form :result result :sym sym})))

;; System

(def system* (atom {:dir "test/__optic__" :optics {}}))

;; Library exports

(defmacro defoptic [sym form & {:keys [dir system]}]
  `(let [ns-sym#   (symbol (str (:ns (meta (declare ~sym)))) (name '~sym))
         filepath# (sym->filepath ns-sym#)
         file-obj# (build-file (or ~dir (some-> ~system deref :dir)
                                   (:dir @system*)) filepath#)]
     (defn ~sym []
       (let [optic# (write-optic file-obj# ns-sym# '~form ~form)]
         (swap! (or ~system system*) update :optics assoc ns-sym# optic#)
         optic#))
     (~sym)
     ~sym))

(defn error
  ([sym] (error system* sym))
  ([system sym]
   (some-> (get-in @system [:optics sym])
           :diff :string println)))

(defn errors
  ([] (errors system*))
  ([system] (run! error (keys (:optics @system)))))

(defn adjust!
  ([sym] (adjust! system* sym))
  ([system sym]
   (if-let [{:keys [err-file file]} (get-in @system [:optics sym])]
     (when (and err-file file)
       (.renameTo (io/file err-file) (io/file file))
       {:adjusted ((resolve sym))})
     {:failure (str "Could not find `" sym "` in defined optics")})))

(defn adjust-all!
  ([] (adjust-all! system*))
  ([system]
   (filter identity (map adjust! (keys (:optics @system))))))

(defn review!* [f exceptions]
  (try ((resolve f))
       (catch Exception e (swap! exceptions conj e))))

(defn review!
  ([] (review! system*))
  ([system]
   (let [exceptions (atom [])]
     (run! #(review!* % exceptions) (keys (:optics @system)))
     (let [optics (:optics @system)
           passed (count (filter :passing? (vals optics)))
           failed (- (count optics) passed)]
       (errors)
       {:passed passed :failed failed :exceptions (count @exceptions)}))))

(defn remove! [& syms]
  (let [atom?  (instance? clojure.lang.Atom (first syms))
        syms'  (if atom? (rest syms) syms)
        system (if atom? (first syms) system*)]
    (doseq [sym syms']
      (let [{:keys [file err-file]} (get-in @system [:optics sym])]
        (when file (.delete (io/file file)))
        (when err-file (.delete (io/file err-file)))))
    (apply swap! system dissoc :optics syms')
    (review!)))

(defn clear!
  ([] (clear! system*))
  ([system]
   (apply remove! system (keys (:optics @system)))))

(defn set-dir!
  ([] (set-dir! system*))
  ([system dir] (swap! system assoc :dir dir)))

(defn clean!
  ([] (clean! system* nil))
  ([k] (clean! system* k))
  ([system k]
   (let [{:keys [optics dir]} @system]
     (if (= k :confirm)
       (println "Deleting files...")
       (println "The below files are stale. Run with :confirm to delete."))
     (doseq [[sym path] (dir-syms dir)]
       (when-not (get optics sym)
         (when (= k :confirm)
           (.delete (io/file path)))
         (println path))))))
