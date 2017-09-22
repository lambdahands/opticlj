(ns opticlj.core-test
  (:require [opticlj.core :refer :all]))

;;;; Temporary initial optic

(defoptic error-filename-regex
  [(err-filename (File. "foo.clj"))
   (err-filename (File. "foo-bar-baz..clj"))])

