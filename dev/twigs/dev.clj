(ns twigs.dev
  (:require [twigs.core :as tw]))

(def r
  (tw/url->ref "https://twigs.firebaseio.com/a/b"))

(println (pop r))

(println (conj r :c))

(println (= (empty r) (-> r pop pop)))

