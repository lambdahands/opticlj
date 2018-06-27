(ns opticlj.core
  (:require [opticlj.file :as file]
            [opticlj.writer :as writer]))

;; System

(def system*
  (atom {:dir #?(:clj  "test/__optic__"
                 :cljs "test/__optic_cljs__")
         :optics {}
         :optic-fns {}}))

;; Library exports

(defmacro defoptic [kw form & {:keys [dir system]}]
  `(let [dir#  (or ~dir (some-> ~system deref :dir) (:dir @system*))
         path# (file/stage dir# (file/sym->filepath ~kw))
         run#  (fn []
                 (let [optic# (writer/write path# ~kw '~form ~form)]
                   (swap! (or ~system system*) update :optics assoc ~kw optic#)
                   optic#))]
     (swap! (or ~system system*) update :optic-fns assoc ~kw run#)
     (run#)
     ~kw))

(defn run
  ([kw] (run kw system*))
  ([kw system]
   (if-let [f (get-in @system [:optic-fns kw])]
     (f) (str "Optic " kw " not found in system"))))

(defn error
  ([kw] (error system* kw))
  ([system kw]
   (some-> (get-in @system [:optics kw]) :diff :string println)))

(defn errors
  ([] (errors system*))
  ([system] (run! error (keys (:optics @system)))))

(defn check [{:keys [kw passing?]}]
  (when-not passing? (error kw))
  passing?)

(defn adjust!* [system kw]
  (if-let [optic (get-in system [:optics kw])]
    (when (and (:err-file optic) (:file optic))
      (file/rename (:err-file optic) (:file optic))
      {:adjusted (when-let [f (get-in system [:optic-fns kw])] (f))})
    {:failure (str "Could not find `" kw "` in defined optics")}))

(defn adjust!
  ([kw] (adjust!* @system* kw))
  ([system kw] (adjust!* @system kw)))

(defn adjust-all!
  ([] (adjust-all! system*))
  ([system]
   (filter identity (map adjust! (keys (:optics @system))))))

(defn review!* [optic-fn exceptions]
  (try (optic-fn)
       #?(:clj  (catch Exception e (swap! exceptions conj e))
          :cljs (catch js/Error  e (swap! exceptions conj e)))))

(defn review!
  ([] (review! system*))
  ([system]
   (let [exceptions (atom [])]
     (run! #(review!* % exceptions) (vals (:optic-fns @system)))
     (let [optics (:optics @system)
           passed (count (filter :passing? (vals optics)))
           failed (- (count optics) passed)]
       (errors)
       {:passed passed :failed failed :exceptions (count @exceptions)}))))

(defn atom? [x]
  #?(:clj  (instance? clojure.lang.Atom x)
     :cljs (instance? cljs.core/Atom x)))

(defn remove! [& kws]
  (let [kws'  (if (atom? (first kws)) (rest kws) kws)
        system (if (atom? (first kws)) (first kws) system*)]
    (doseq [sym kws']
      (let [{:keys [file err-file]} (get-in @system [:optics sym])]
        (when file (file/delete file))
        (when err-file (file/delete err-file))))
    (apply swap! system dissoc :optics kws')))

(defn clear!
  ([] (clear! system*))
  ([system]
   (apply remove! system (keys (:optics @system)))))

(defn set-dir!
  ([dir] (set-dir! system* dir))
  ([system dir] (swap! system assoc :dir dir)))

(defn clean-msg [dir kws k]
  (cond
   (empty? kws)   (println "Directory" dir "is clean.")
   (= k :confirm) (println "Deleting files...")
   :else          (println "The below files aren't defined in the system with"
                           "the :dir" dir ". Run with :confirm to delete.")))

(defn ok? [{:keys [passed failed exceptions] :as review-result}]
  (zero? (+ failed exceptions)))

;; TODO: Implement these utility functions in ClojureScript

#?(:clj
   (defn filtered-syms [dir optics]
     (->> (into [] (file/dir-syms dir))
       (remove (fn [[sym _]] (get optics sym))))))

#?(:clj
   (defn clean!
     ([] (clean! system* nil))
     ([k] (clean! system* k))
     ([system k]
      (let [{:keys [optics dir]} @system]
        (clean-msg dir (filtered-syms dir optics) k)
        (doseq [[sym path] syms]
          (when (= k :confirm)
            (file/delete path))
          (println path))))))
