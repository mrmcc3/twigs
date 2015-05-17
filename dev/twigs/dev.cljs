(ns twigs.dev
  (:require [twigs.core :as tw]))

(enable-console-print!)

(def r
  (tw/ref "https://twigs.firebaseio.com/a/b"))

(println (pop r))

(println (conj r :c))

(println (= (empty r) (-> r pop pop)))
