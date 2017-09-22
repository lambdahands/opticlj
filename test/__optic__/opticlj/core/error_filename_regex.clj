(in-ns 'opticlj.core)

[(err-filename (File. "foo.clj"))
 (err-filename (File. "foo-bar-baz..clj"))]

["foo.err.clj" "foo-bar-baz..err.clj"]
