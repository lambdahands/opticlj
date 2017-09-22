(in-ns 'opticlj.core-test)

[(.toString (form-output-stream *ns* '(+ 1 1) 2))
 (.toString
  (form-output-stream
   *ns*
   '(map inc (range 10))
   '(1 2 3 4 5 6 7 8 9 10)))]

["(in-ns 'opticlj.core-test)\n\n(+ 1 1)\n\n2\n"
 "(in-ns 'opticlj.core-test)\n\n(map inc (range 10))\n\n(1 2 3 4 5 6 7 8 9 10)\n"]
