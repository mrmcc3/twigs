(defproject twigs "0.1.6-SNAPSHOT"
  :description "A simple way to use firebase in Clojure[Script]"
  :url "https://github.com/mrmcc3/twigs"
  :license {:name "MIT"}

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.firebase/firebase-client-jvm "2.3.0"]
                 [cljsjs/firebase "2.2.3-0"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :java-source-paths ["src/java"]

  :cljsbuild {
    :builds [{:id "test"
              :source-paths ["src" "test"]
              :compiler {:main twigs.test-runner
                         :output-to "target/test.js"
                         :output-dir "target/test"
                         :optimizations :none
                         :source-map true}}]})

