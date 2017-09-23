(in-ns 'opticlj.core-test)

(take 10 (iterate (fn [[a b]] [b (+ a b)]) [1 1]))

([1 1] [1 2] [2 3] [3 5] [5 8] [8 13] [13 21] [21 34] [34 55] [55 89])
