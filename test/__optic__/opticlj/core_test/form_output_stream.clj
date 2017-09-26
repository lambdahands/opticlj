(in-ns 'opticlj.core-test)

(map
 (fn
  [[form result]]
  (writer/form-output-stream
   'opticlj.writer/form-output-stream
   form
   result))
 '[[(+ 1 1) 2] [(map inc (range 10)) (1 2 3 4 5 6 7 8 9 10)]])

("(in-ns 'opticlj.writer)\n\n(+ 1 1)\n\n2\n"
 "(in-ns 'opticlj.writer)\n\n(map inc (range 10))\n\n(1 2 3 4 5 6 7 8 9 10)\n")
