(ns opticlj.file
  (:require #?(:clj [clojure.java.io :as io])
            [clojure.string :as str])
  #?(:clj (:import [java.io BufferedReader StringReader FileReader]
                   [difflib DiffUtils])))

#?(:cljs (def fs        (js/require "fs")))
#?(:cljs (def mkdirp    (js/require "mkdirp")))
#?(:cljs (def node-path (js/require "path")))
#?(:cljs (def node-diff (js/require "diff")))

;; File utils

(def file-match #?(:clj  #"(\.err\.clj$|\.clj$)"
                   :cljs #"(\.err\.cljs$|\.cljs$)"))

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

(defn dir-optics [dir]
  (filter #(re-find file-match %)
          #?(:clj (map str (file-seq (io/file dir)))
             :cljs '())))

(defn dir-syms [dir]
  (into {} (map #(vector (filepath->sym % dir) %) (dir-optics dir))))

(defn stage [dir path]
  #?(:clj  (str (doto (io/file dir path) (.. getParentFile mkdirs)))
     :cljs (let [path' (node-path.join dir path)]
             (mkdirp (node-path.dirname path) #js {} (constantly nil))
             path')))

(defn exists [path]
  #?(:clj  (.exists (io/file path))
     :cljs (fs.existsSync path)))

(defn rename [from-path to-path]
  #?(:clj  (.renameTo (io/file from-path) (io/file to-path))
     :cljs (when (exists from-path) (fs.rename from-path to-path))))

(defn delete [path]
  #?(:clj  (.delete (io/file path))
     :cljs (when (exists path) (fs.delete path))))

(defn path [file]
  #?(:clj  (.getPath file)
     :cljs file))

(defn write [file output]
  #?(:clj  (spit file output)
     :cljs (fs.writeFile file output (constantly nil))))

(defn err-path [path]
  #?(:clj  (str/replace path #"\.clj$" ".err.clj")
     :cljs (str/replace path #"\.cljs$" ".err.cljs")))

;; diff
(defn diff [path err-path output]
  #?(:clj (let [f-lines (line-seq (BufferedReader. (FileReader. (io/file path))))
                o-lines (line-seq (BufferedReader. (StringReader. output)))
                f-diff  (DiffUtils/diff f-lines o-lines)
                unified (DiffUtils/generateUnifiedDiff path err-path f-lines f-diff 3)]
            (when (seq unified)
              (str/join "\n" unified)))
     :cljs (let [file-str nil #_(fs.readFileSync path)]
             (println path err-path output)
             (when-not (= file-str output)
               (node-diff.createTwoFilesPatch path err-path file-str output)))))
