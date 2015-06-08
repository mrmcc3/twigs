(ns twigs.test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [twigs.core-test]))

(enable-console-print!)

(run-tests 'twigs.core-test)
