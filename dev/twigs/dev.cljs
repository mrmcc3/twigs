(ns twigs.dev
  (:require [twigs.core :as tw]))

(enable-console-print!)

(def r
  (tw/url->ref "https://twigs.firebaseio.com/a/b"))

(println (pop r))

(println (conj r :c))

(println (= (empty r) (-> r pop pop)))

;;(-> r .-ref
;;    (.set #js {:c "hello " :d "world!"}))

(defn handler [ss]

  ;; we can now destructure snapshots
  (let [[k {:keys [c] :as v}] (tw/wrap-ss ss)]

    (println k) ;; "b"
    (println @c) ;; "hello". remember to deref values

    ;; snapshots are now sequable
    (-> v keys println)
    (->> v vals (map deref) (reduce str) println)))

(-> r .-ref (.on "value" handler))
