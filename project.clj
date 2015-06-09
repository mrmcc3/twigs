(defproject mrmcc3/twigs "0.1.7-SNAPSHOT"
  :description "A simple way to use firebase in Clojure[Script]"
  :url "https://github.com/mrmcc3/twigs"
  :license {:name "MIT"}

  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.firebase/firebase-client-jvm "2.3.0"]
                 [cljsjs/firebase "2.2.3-0"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :java-source-paths ["src/twigs/java"]

  :aliases {"test" ["do"
                    "clean,"
                    "test" "twigs.core-test,"
                    "cljsbuild" "once" "browser-test-min"]}

  :repositories [["clojars" {:url "http://clojars.org/repo/"
                             :username [:gpg :env/clojars_user]
                             :passphrase [:gpg :env/clojars_pass]}]]

  :cljsbuild {
    :builds [{:id "browser-test-min"
              :source-paths ["src" "test"]
              :compiler {:main twigs.browser-test-runner
                         :output-to "target/main.js"
                         :optimizations :advanced}}
             {:id "browser-test"
              :source-paths ["src" "test"]
              :compiler {:main twigs.browser-test-runner
                         :output-to "target/main.js"
                         :output-dir "target/out"
                         :optimizations :none
                         :source-map true}}]})

