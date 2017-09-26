(ns opticlj.core
  (:require [opticlj.file :as file]
            [opticlj.writer :as writer]))

;; System

(def system* (atom {:dir "test/__optic__" :optics {}}))

;; Library exports

(defmacro defoptic [sym form & {:keys [dir system]}]
  `(let [sym#  (symbol (namespace ::__hack) (name '~sym))
         dir#  (or ~dir (some-> ~system deref :dir) (:dir @system*))
         path# (file/stage dir# (file/sym->filepath sym#))]
     (defn ~sym []
       (let [optic# (writer/write path# sym# '~form ~form)]
         (swap! (or ~system system*) update :optics assoc sym# optic#)
         optic#))
     (~sym)
     ~sym))

(defn error
  ([sym] (error system* sym))
  ([system sym]
   (some-> (get-in @system [:optics sym]) :diff :string println)))

(defn errors
  ([] (errors system*))
  ([system] (run! error (keys (:optics @system)))))

(defmacro adjust!* [system sym]
  `(if-let [{:keys [err-file# file#]} (get-in @~system [:optics ~sym])]
     (when (and err-file# file#)
       (file/rename err-file# file#)
       {:adjusted ((resolve ~sym))})
     {:failure (str "Could not find `" ~sym "` in defined optics")}))

(defn adjust!
  ([sym] (adjust!* system* sym))
  ([system sym] (adjust!* system sym)))

(defn adjust-all!
  ([] (adjust-all! system*))
  ([system]
   (filter identity (map adjust! (keys (:optics @system))))))

(defmacro review!* [f exceptions]
 `(try ((resolve ~f))
       #?(:clj  (catch Exception e# (swap! ~exceptions conj e#))
          :cljs (catch js/Error e# (swap! ~exceptions conj e#)))))

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

(defn atom? [x]
  #?(:clj  (instance? clojure.lang.Atom x)
     :cljs (instance? cljs.core/Atom x)))

(defn remove! [& syms]
  (let [syms'  (if (atom? (first syms)) (rest syms) syms)
        system (if (atom? (first syms)) (first syms) system*)]
    (doseq [sym syms']
      (let [{:keys [file err-file]} (get-in @system [:optics sym])]
        (when file (file/delete file))
        (when err-file (file/delete err-file))))
    (apply swap! system dissoc :optics syms')))

(defn clear!
  ([] (clear! system*))
  ([system]
   (apply remove! system (keys (:optics @system)))))

(defn set-dir!
  ([dir] (set-dir! system* dir))
  ([system dir] (swap! system assoc :dir dir)))

(defn clean-msg [dir syms k]
  (cond
   (empty? syms)  (println "Directory" dir "is clean.")
   (= k :confirm) (println "Deleting files...")
   :else          (println "The below files aren't defined in the system with"
                           "the :dir" dir ". Run with :confirm to delete.")))

#?(:clj
   (defn filtered-syms [dir optics]
     (->> (into [] (file/dir-syms dir))
       (remove (fn [[sym _]] (get optics sym))))))

#?(:clj
   (defn clean!
     ([] (clean! system* nil))
     ([k] (clean! system* k))
     ([system k]
      (let [{:keys [optics dir]} @system
            syms (filtered-syms dir optics)]
        (clean-msg dir syms k)
        (doseq [[sym path] syms]
          (when (= k :confirm)
            (file/delete path))
          (println path))))))