(ns ^:figwheel-always opticlj.cljs.core-test
  (:require [cljs.nodejs :as node]
            [opticlj.file :as file]
            [opticlj.core :as optic :refer-macros [defoptic]]))

(node/enable-util-print!)

(defoptic hello (+ 2 2))

(def -main (fn [] nil))

(set! *main-cli-fn* -main)
