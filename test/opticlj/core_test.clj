(ns opticlj.core-test
  (:require [opticlj.core :as optic]))

(optic/defoptic form-output-stream
  (map (fn [[form result]]
         (optic/form-output-stream `optic/form-output-stream form result))
       '[[(+ 1 1)              2]
         [(map inc (range 10)) (1 2 3 4 5 6 7 8 9 10)]]))

(optic/defoptic err-filename
  [(optic/err-filename (java.io.File. "foo.clj"))
   (optic/err-filename (java.io.File. "foo-bar-baz..clj"))])

(optic/defoptic defoptic
  (let [system (atom {:optics {} :dir "test/__optic__"})]
    (optic/defoptic fibonacci
      (take 10 (iterate (fn [[a b]] [b (+ a b)]) [1 1]))
      :system system)
    (get-in @system [:optics `fibonacci])))

(optic/review!)
