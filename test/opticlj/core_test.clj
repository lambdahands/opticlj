(ns opticlj.core-test
  (:require [clojure.java.io :as io]
            [opticlj.core :as optic]
            [opticlj.file :as file]
            [opticlj.writer :as writer]
            [clojure.test :as test :refer [deftest]]))

(optic/defoptic ::form-output-stream
  (map (fn [[form result]]
         (writer/form-output-stream `writer/form-output-stream form result))
       '[[(+ 1 1)              2]
         [(map inc (range 10)) (1 2 3 4 5 6 7 8 9 10)]]))

(optic/defoptic ::err-filename
  [(file/err-path "foo.clj") (file/err-path "foo-bar-baz..clj")])

(defn fib [n]
  (take n (iterate (fn [[a b]] [b (+ a b)]) [1 1])))

(optic/defoptic ::defoptic
  (let [system (atom {:optics {} :dir "test/__optic__"})]
    (optic/defoptic ::fibonacci (fib 10) :system system)
    (get-in @system [:optics ::fibonacci])))

(deftest optics
  (test/is (optic/ok? (optic/review!))))
