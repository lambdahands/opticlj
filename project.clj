(defproject opticlj "1.0.0-alpha8"
  :description "A Clojure expectation/snapshot testing library, inspired by cram, ppx_expect, and jest"
  :url "http://github.com/lambdahands/opticlj"
  :license {:name "MIT"}
  :dependencies [;; Clojure Deps
                 [org.clojure/clojure "1.8.0"]
                 [com.googlecode.java-diff-utils/diffutils "1.3.0"]
                 ;; ClojureScript Deps
                 [org.clojure/clojurescript "1.9.908"]
                 [cljsjs/jsdiff "3.1.0-0"]
                 [figwheel-sidecar "0.5.13"]
                 [com.cemerick/piggieback "0.2.1"]
                 [doo "0.1.7"]
                 [zprint "0.4.9"]
                 [clojure-future-spec "1.9.0-alpha17"]]
  :plugins      [[lein-cljsbuild "1.1.7"]
                 [lein-figwheel "0.5.13"]
                 [lein-doo "0.1.7"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :clean-targets ^{:protect false} ["target"]
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src/opticlj/cljs/" "test/opticlj/cljs/"]
                        :compiler {:main opticlj.cljs.runner
                                   :output-to "target/test/main.js"
                                   :output-dir "target/test"
                                   :target :nodejs
                                   ;:source-map true
                                   :optimizations :none}}
                       {:id "test-dev"
                        :source-paths ["src/opticlj/cljs/" "test/opticlj/cljs/"]
                        :figwheel true
                        :compiler {:main opticlj.cljs.core-test
                                   :output-to "target/test-dev/main.js"
                                   :output-dir "target/test-dev"
                                   :target :nodejs
                                   ;:source-map true
                                   :optimizations :none}}]}
  :figwheel {})
