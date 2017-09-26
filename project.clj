(defproject opticlj "1.0.0-alpha3"
  :description "A Clojure expectation/snapshot testing library, inspired by cram, ppx_expect, and jest"
  :url "http://github.com/lambdahands/opticlj"
  :license {:name "MIT"}
  :dependencies [;; Clojure Deps
                 [org.clojure/clojure "1.8.0"]
                 [com.googlecode.java-diff-utils/diffutils "1.3.0"]
                 ;; ClojureScript Deps
                 [org.clojure/clojurescript "1.9.908"]
                 [figwheel-sidecar "0.5.13"]
                 [com.cemerick/piggieback "0.2.1"]]
  :plugins      [[lein-cljsbuild "1.1.7"]
                 [lein-figwheel "0.5.13"]
                 [lein-doo "0.1.7"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :clean-targets ^{:protect false} ["target"]
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src/opticlj/cljs/" "test/opticlj/cljs/"]
                        :figwheel true
                        :compiler {:main opticlj.cljs.runner
                                   :output-to "target/test/main.js"
                                   :output-dir "target/test"
                                   :target :nodejs
                                   :npm-deps {:mkdirp "0.5.1"
                                              :diff   "3.3.1"}
                                   :install-deps true
                                   ;:source-map true
                                   :optimizations :none}}]}
  :figwheel {})
