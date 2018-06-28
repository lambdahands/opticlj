(ns ^:figwheel-always opticlj.cljs.core-test
  (:require [clojure.test :as test :refer-macros [deftest]]
            [opticlj.core :as optic :refer-macros [defoptic]]))

(defoptic ::two-plus-two (+ 2 2))

(deftest optics
  (test/is (optic/ok? (optic/review!))))
