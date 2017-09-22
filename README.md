# opticlj

opticlj is a Clojure expectation testing (also known as snapshot testing)
library.

## Rationale

Expectations, or snapshots, is an automated testing strategy that captures the
output of a program as a reference to its correctness. In contrast to unit
testing, snapshots don't require the programmer to _specify_ the correct output
of their program but instead to _verify_ the output.

`opticlj` let's you define these snapshots and automatically generate the
outputs into files. If you change the implementation of your program, the output
files may be checked against the new behavior for differences.

I was inspired by this testing strategy because it navigates elegantly between
REPL driven development and testing. Unit testing is often cumbersome, but I've
found it to be even more so while writing Clojure code: I often _verify_ the
correctness of my functions by simply evaluating them, but that output doesn't
persist outside of my own machine.

Snapshot testing may be a way for Clojure developers to cast a wide net over
the correctness of their programs while staying close to the REPL.

### Use Cases

Snapshot testing is often a great substitute to unit testing, but it in no way
has the power to verify programs as thoroughly as property-based testing.
Snapshot tests are best used for _pure functions_, and aren't recommended in
cases where correctness must be _"proven"_ (big air quotes).

**Inspirations**

- [Snapshot Testing in Swift](http://www.stephencelis.com/2017/09/snapshot-testing-in-swift)
- [Testing with expectations](https://blog.janestreet.com/testing-with-expectations/)
- [Mercurial: adding 'unified' tests](https://www.selenic.com/blog/?p=663)
- [Jest: Snapshot Testing](https://facebook.github.io/jest/docs/en/snapshot-testing.html)

## Installation

```
[opticlj "1.0.0-alpha2"]
```

[See on Clojars](https://clojars.org/opticlj)


**Disclaimer**

`opticlj` is alpha software, and its API is likely subject to change.

## Usage

Require the `opticlj.core` namespace to get started:

```clj
(ns my-project.core-test
  (:require [opticlj.core :as optic :refer [defoptic]]))
```

Let's define a function to test:

```clj
(defn add [x y]
  (+ x y))
```

Define an `optic` like so:

```clj
(defoptic one-plus-one (add 1 1))
```

This does two things:

- Defines "runner" function `one-plus-one`
- Writes an output file in `test/__optic__/my_project/core_test/one_plus_one.clj`

Here's what `one_plus_one.clj` looks like:

```clj
(in-ns 'my-project.core-test)

(add 1 1)

2
```

The `in-ns` expression allows us to evaluate this file, which is especially
useful if your editor integrates with the REPL.

Next, if we change the implementation of `add` and re-run the optic, we get an
output confirming the snapshot was checked:

```clj
(defn add [x y]
  (+ x y))

(one-plus-one)

; outputs
{:file "test/__optic__/my_project/core_test/one_plus_one.clj",
 :err-file "test/__optic__/my_project/core_test/one_plus_one.err.clj",
 :passing? false,
 :diff {:string "<truncated>"},
 :form (add 1 1),
 :result 4,
 :sym my-project.core-test/one-plus-one,
 :ns #object[clojure.lang.Namespace 0x2cc4080a "my-project.core-test"]}
```

A new file was created: `test/__optic__/my_project/core_test/one_plus_one.err.clj`

```clj
(in-ns 'my-project.core-test)

(add 1 1)

4
```

Also, note how the `:passing?` key is `false`. We can view our error diff by
calling `optic/errors`:

```clj
(optic/errors)
; prints
--- test/__optic__/my_project/core_test/one_plus_one.clj   2017-09-22 16:03:38.000000000 -0500
+++ -   2017-09-22 16:04:38.000000000 -0500
@@ -2,4 +2,4 @@

 (add 1 1)

-2
+4
```

What we get back is essentially the output of running:

```
echo "...my new output..." | diff -u <output-file> -
```

Let's say we wanted to change the rules of our universe and make the addition
of one and one equal to four. We can `adjust!` our optic to accept these new rules:

```clj
(optic/adjust! `one-plus-one)

; outputs
{:adjusted {:file "test/__optic__/my_project/core_test/one_plus_one.clj"
            :passing? true
            :diff nil
            :err-file nil
            :form (add 1 1)
            :result 4
            :sym my-project.core-test/one-plus-one
            :ns #object[clojure.lang.Namespace 0x2cc4080a "my-project.core-test"]}}
```

Now when we check for errors, we see we have resolved our new form of arithmetic:

```clj
(optic/errors)

; outputs
nil
```

## Todo

- [ ] Complete API documentation
- [ ] Reimplement core API with stateless methods, allow for wider snapshots
- [ ] Warn if optics is undefined in the program yet exists in a file
- [ ] Add a `clean!` method to remove unused optics
- [ ] Use `defoptic` on `defoptic` _(Inception noise)_
- [ ] lein/boot command integration
- [ ] Pretty output, feat. colors
