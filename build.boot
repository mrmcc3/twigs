
(def +version+ "0.1.7-SNAPSHOT")

(set-env!
  :source-paths #{"src" "test"}
  :resource-paths #{"resources"}
  :dependencies '[[org.clojure/clojure "1.7.0-RC1"]
                  [org.clojure/clojurescript "0.0-3308"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                  [com.firebase/firebase-client-jvm "2.3.0"]
                  [cljsjs/firebase "2.2.3-0"]
                  [adzerk/bootlaces "0.1.11" :scope "test"]
                  [adzerk/boot-test "1.0.4" :scope "test"]
                  [adzerk/boot-cljs "0.0-3269-2" :scope "test"]])

(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :refer :all]
         '[adzerk.boot-cljs :refer :all])

(bootlaces! +version+)

(task-options!
  pom {:project     'mrmcc3/twigs
       :version     +version+
       :description "A simple way to use firebase from Clojure[Script]"
       :url         "https://github.com/mrmcc3/twigs"
       :scm         {:url "https://github.com/mrmcc3/twigs"}
       :license     {"MIT" "http://opensource.org/licenses/MIT"}}
  test {:namespaces '#{twigs.core-test}}
  speak {:theme "woodblock"})

(deftask test-all []
  (comp (javac) (cljs :optimizations :advanced) (test)))

