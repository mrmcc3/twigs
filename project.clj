(defproject twigs "0.1.4"
  :description "A nicer way to use firebase in Clojure[Script]"
  :url "https://github.com/mrmcc3/twigs"
  :license {:name "MIT"}

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3297"]
                 [com.firebase/firebase-client-jvm "2.2.4"]
                 [cljsjs/firebase "2.2.3-0"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :java-source-paths ["src/java"]

  :clean-targets ^{:protect false} ["target" "dev/public/out" "dev/public/main.js"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "dev"]
              :notify-command ["growlnotify" "-m"]
              :compiler {:main twigs.dev
                         :output-to "dev/public/main.js"
                         :output-dir "dev/public/out"
                         :asset-path "out"
                         :optimizations :none
                         :source-map true}}]})

