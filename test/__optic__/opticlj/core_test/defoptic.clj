(in-ns 'opticlj.core-test)

(let
 [system (atom {:optics {}, :dir "test/__optic__"})]
 (optic/defoptic :opticlj.core-test/fibonacci (fib 10) :system system)
 (get-in @system [:optics 'opticlj.core-test/fibonacci]))

{:form (fib 10),
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
 :sym opticlj.core-test/fibonacci,
 :file "test/__optic__/opticlj/core_test/fibonacci.clj",
 :err-file nil,
 :diff nil,
 :passing? true}
