(in-ns 'opticlj.core-test)

(let
 [system (atom {:optics {}, :dir "test/__optic__"})]
 (optic/defoptic
  fibonacci
  (take 10 (iterate (fn [[a b]] [b (+ a b)]) [1 1]))
  :system
  system)
 (get-in @system [:optics 'opticlj.core-test/fibonacci]))

{:file "test/__optic__/opticlj/core_test/fibonacci.clj",
 :passing? true,
 :diff nil,
 :err-file nil,
 :form (take 10 (iterate (fn [[a b]] [b (+ a b)]) [1 1])),
 :result
 ([1 1]
  [1 2]
  [2 3]
  [3 5]
  [5 8]
  [8 13]
  [13 21]
  [21 34]
  [34 55]
  [55 89]),
 :sym opticlj.core-test/fibonacci}
