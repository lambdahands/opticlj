(ns ^:figwheel-always opticlj.cljs.core-test
  (:require [cljs.nodejs :as node]
            [opticlj.file :as file]
            [clojure.test :as test]
            [opticlj.core :as optic :refer-macros [defoptic]]))

(node/enable-util-print!)

(defoptic ::two-plus-two (+ 2 2))

(def -main (fn [] nil))

(set! *main-cli-fn* -main)

(test/is (optic/passing? (optic/review!)))
