(ns twigs.test-runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            [twigs.core-test]))

(enable-console-print!)

(run-all-tests #"twigs.*-test")
