(ns twigs.core-test
  (:require #?@(:clj [[clojure.test :refer [is testing deftest]]
                      [clojure.core.async :refer [sub unsub unsub-all chan take! <!!]]]
                :cljs [[cljs.test :refer-macros [is testing deftest async]]
                       [cljs.core.async :refer [sub unsub unsub-all chan take!]]])
            [twigs.core :as tw]))

(deftest twig-refs
  (let [url "https://twigs.firebaseio.com/a"
        r (tw/url->ref url)]
    (is (= (str r) url))
    (is (= (peek r) :a))
    (is (= (pop r) (empty r)))
    (is (not= (pop r) r))
    (is (= (conj r :b) (tw/url->ref (str url "/b"))))
    (is (= r (-> r (conj :c) pop)))))

(deftest twig-query-snaps
  (let [r (tw/url->ref "https://twigs.firebaseio.com/a")
        q (tw/query r)
        ch (chan)
        do-test (fn [[k {:keys [b] :as a}]]
                  (unsub-all q)
                  (is (= k :a))
                  (is (= (count a) 1))
                  (is (= (count b) 2))
                  (is (not (realized? a)))
                  (is (not (realized? b)))
                  (is (= @b {:c "hello" :d "world"}))
                  (is (not (realized? a)))
                  (is (realized? b))
                  (is (realized? (:c b)))
                  (doseq [[k ss] b]
                    (is (or (= k :c) (= k :d)))
                    (is (realized? ss))
                    (is (or (= @ss "hello") (= @ss "world")))
                    (is (= (count ss) 0))
                    (is (nil? (seq ss)))
                    (is (nil? (get ss :nothing nil)))
                    (is (realized? (:nothing ss)))
                    (is (= (count (:nothing ss)) 0))
                    (is (nil? (deref (:nothing ss))))))]
    (sub q :value ch)
    #?(:cljs (async done (take! ch (fn [ss] (do-test ss) (done))))
       :clj (do-test (<!! ch)))))

