(ns opticlj.core-test
  (:require [opticlj.core :refer :all]))

;;;; Temporary initial optic

(defoptic error-filename-regex
  [(err-filename (java.io.File. "foo.clj"))
   (err-filename (java.io.File. "foo-bar-baz..clj"))])

