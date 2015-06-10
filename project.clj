(defproject mrmcc3/twigs "0.1.10-SNAPSHOT"
  :description "A simple way to use firebase in Clojure[Script]"
  :url "https://github.com/mrmcc3/twigs"
  :license {:name "MIT"}

  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]

  :dependencies [[org.clojure/clojure "1.7.0-RC1" :scope "provided"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                 [com.firebase/firebase-client-jvm "2.3.0" :scope "provided"]
                 [cljsjs/firebase "2.2.3-0"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :java-source-paths ["src/twigs/java"]


  :aliases {"test" ["do" "clean," "test" "twigs.core-test,"
                    "cljsbuild" "once" "browser-test-min"]}

  :deploy-repositories [["releases" :clojars]]

  :cljsbuild {
    :builds [{:id "browser-test-min"
              :source-paths ["src" "test"]
              :compiler {:main twigs.test-runner
                         :output-to "target/main.js"
                         :optimizations :advanced}}
             {:id "browser-test"
              :source-paths ["src" "test"]
              :compiler {:main twigs.test-runner
                         :output-to "target/main.js"
                         :output-dir "target/out"
                         :optimizations :none
                         :source-map true}}]}

  :profiles {
    :examples {
      :dependencies [[org.omcljs/om "0.8.8"] [sablono "0.3.4"]]
      :cljsbuild {
        :builds [{:id "hackernews"
                  :source-paths ["src" "examples/hackernews/src"]
                  :notify-command ["growlnotify" "-m"]
                  :compiler {
                    :main examples.hackernews.core
                    :asset-path "out"
                    :output-to "examples/hackernews/main.js"
                    :output-dir "examples/hackernews/out"
                    :optimizations :none
                    :source-map true}}]}}})

