(ns opticlj.cljs.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [opticlj.cljs.core-test]))

(doo-tests 'opticlj.cljs.core-test)
