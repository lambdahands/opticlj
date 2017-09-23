(in-ns 'opticlj.core-test)

[(optic/err-filename (java.io.File. "foo.clj"))
 (optic/err-filename (java.io.File. "foo-bar-baz..clj"))]

["foo.err.clj" "foo-bar-baz..err.clj"]
