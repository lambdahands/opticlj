(in-ns 'opticlj.core-test)

[(file/err-path "foo.clj") (file/err-path "foo-bar-baz..clj")]

["foo.err.clj" "foo-bar-baz..err.clj"]
