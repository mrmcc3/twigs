(ns twigs.dev
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [twigs.core :as tw]
            [cljs.core.async :as ca]))

(enable-console-print!)

(def r
  (tw/url->ref "https://twigs.firebaseio.com/a/b"))

(println (pop r))

(println (conj r :c))

(println (= (empty r) (-> r pop pop)))

(def ch (ca/chan))

(def q (tw/query r {:order-by-value true :limit-to-first 2}))

(ca/sub q :value ch)

(go-loop []
  (let [[k ss] (ca/<! ch)]
    (println (map first ss))
    (recur)))

