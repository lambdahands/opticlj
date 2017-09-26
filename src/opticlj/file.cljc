(ns opticlj.file
  (:require #?(:clj [clojure.java.io :as io])
            #?(:cljs [cljsjs.jsdiff])
            [clojure.string :as str])
  #?(:clj (:import [java.io BufferedReader StringReader FileReader]
                   [difflib DiffUtils])))

#?(:cljs (def fs        (js/require "fs")))
#?(:cljs (def node-path (js/require "path")))

;; File utils

(def file-match #?(:clj  #"(\.err\.clj$|\.clj$)"
                   :cljs #"(\.err\.cljs$|\.cljs$)"))

(defn sym->filepath [sym]
  (let [ns-str   (str/replace (namespace sym) #"-" "_")
        sym-file (str/replace (name sym) #"-" "_")
        path-vec (str/split ns-str #"\.")]
    (str/join "/" (conj path-vec (str sym-file #?(:clj  ".clj"
                                                  :cljs ".cljs"))))))

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

#?(:cljs
   (defn mkdir [parent child]
     (let [curdir (node-path.resolve parent child)]
       (when-not (fs.existsSync curdir)
         (fs.mkdirSync curdir))
       curdir)))

(defn stage [dir path]
  #?(:clj  (str (doto (io/file dir path) (.. getParentFile mkdirs)))
     :cljs (let [path' (node-path.join dir path)]
             (reduce mkdir (str/split (node-path.dirname path') #"/"))
             path')))

(defn exists [path]
  #?(:clj  (.exists (io/file path))
     :cljs (fs.existsSync path)))

(defn rename [from-path to-path]
  #?(:clj  (.renameTo (io/file to-path) (io/file from-path))
     :cljs (when (exists from-path) (fs.rename from-path to-path))))

(defn delete [path]
  #?(:clj  (.delete (io/file path))
     :cljs (when (exists path) (fs.unlink path (constantly nil)))))

(defn path [file]
  #?(:clj  (.getPath file)
     :cljs file))

(defn write [file output]
  #?(:clj  (spit file output)
     :cljs (fs.writeFileSync file output)))

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
     :cljs (let [file-str (.toString (fs.readFileSync path))]
             (when-not (= file-str output)
               (js/JsDiff.createTwoFilesPatch path err-path file-str output)))))
