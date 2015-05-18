(require '[twigs.core :as tw])

(import '[com.firebase.client ValueEventListener])

(def r
  (tw/url->ref "https://twigs.firebaseio.com/a/b"))

(println (pop r))

(println (conj r :c))

(println (= (empty r) (-> r pop pop)))

(def listener
  (reify ValueEventListener
    (onDataChange [_ ss]
      ;; we can now destructure snapshots
      (let [[k {:keys [c] :as v}] (tw/wrap-ss ss)]

        (println k) ;; "b"
        (println @c) ;; "hello". remember to deref values
        (println (realized? c)) ;; true
        (println (count v)) ;; 2

        ;; snapshots are now sequable
        (-> v keys println)
        (->> v vals (map deref) (reduce str) println)))
    (onCancelled [_ _])))

(-> r .ref (.addListenerForSingleValueEvent listener))
