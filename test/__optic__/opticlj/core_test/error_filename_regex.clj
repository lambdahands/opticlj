(in-ns 'opticlj.core-test)

[(err-filename (java.io.File. "foo.clj"))
 (err-filename (java.io.File. "foo-bar-baz..clj"))]

["foo.err.clj" "foo-bar-baz..err.clj"]
