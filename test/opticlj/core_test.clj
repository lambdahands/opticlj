(ns opticlj.core-test
  (:require [opticlj.core :refer :all]))

;;;; Temporary initial optic

(defoptic error-filename-regex
  [(err-filename (java.io.File. "foo.clj"))
   (err-filename (java.io.File. "foo-bar-baz..clj"))])

(defoptic form-output-stream-result
  [(.toString (form-output-stream *ns* '(+ 1 1) 2))
   (.toString (form-output-stream *ns* '(map inc (range 10)) '(1 2 3 4 5 6 7 8 9 10)))])
