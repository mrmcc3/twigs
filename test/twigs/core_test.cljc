(ns twigs.core-test
  (:require #?@(:clj [[clojure.test :refer [is testing deftest]]
                      [clojure.core.async :as ca :refer [sub unsub unsub-all chan take! <!! put!]]]
                :cljs [[cljs.test :refer-macros [is testing deftest async]]
                       [cljs.core.async :as ca :refer [sub unsub unsub-all chan take! put!]]])
            [twigs.core :as tw]))

;; assumes the following data at https://twigs.firebaseio.com
;; {
;;   "no-access": "read+writes will fail",
;;   "read-only": {
;;     "data": {
;;       "float": 1.5,
;;       "int": 1,
;;       "string": "hello world"
;;     }
;;   }
;; }
;;
;; with the following security rules
;; {
;;   "rules": {
;;     "write-only": {
;;       ".write": true,
;;       ".validate": "newData.val() == 'hello world'"
;;     },
;;     "read-only": { ".read": true }
;;   }
;; }

;; ref tests
(deftest twig-refs
  (let [url "https://twigs.firebaseio.com/a"
        r (tw/ref url)
        rr (tw/raw-ref url)
        q (tw/query url {:limit-to-first 10})
        rq (tw/raw-query url {:equal-to "sdkjalskj"})]
    (is (= (.toString rr)
           (.toString (tw/raw-ref r))
           (.toString (tw/raw-ref q))
           (.toString (tw/raw-ref rq))))
    (is (= r
           (tw/ref rr)
           (tw/ref q)
           (tw/ref rq)))
    (is (= (str r) url))
    (is (= (peek r) :a))
    (is (= (pop r) (empty r)))
    (is (not= (pop r) r))
    (is (= (conj r :b) (tw/ref (str url "/b"))))
    (is (= r (-> r (conj :c) pop)))))


;; deftest once on FBRef, TwigRef, FBQuery, TwigQuery
;; with and without error callback
(deftest fb-ref-once
  (let [url "https://twigs.firebaseio.com"
        rr1 (-> url tw/raw-ref (.child "read-only"))
        rr2 (-> url tw/raw-ref (.child "no-access"))
        tr1 (-> url tw/ref (conj :read-only))
        tr2 (-> url tw/ref (conj :no-access))
        chs (into [] (take 9 (repeatedly #(chan))))
        res-ch (ca/map vector chs)]
    (tw/once rr1 (fn [_] (put! (nth chs 0) true)))
    (tw/once rr1 (fn [_] (put! (nth chs 1) true))
                 (fn [_] (put! (nth chs 1) false)))
    (tw/once rr2 (fn [_] (put! (nth chs 2) false)))
    (tw/once rr2 (fn [_] (put! (nth chs 2) false))
                 (fn [_] (put! (nth chs 2) true)))
    (tw/once (tw/raw-query rr1 {:order-by-key true :limit-to-first 1})
             (fn [_] (put! (nth chs 3) true)))
    (tw/once rr1 (fn [_] (put! (nth chs 4) true)))
    (tw/once rr1 (fn [_] (put! (nth chs 5) true))
                 (fn [_] (put! (nth chs 5) false)))
    (tw/once rr2 (fn [_] (put! (nth chs 6) false)))
    (tw/once rr2 (fn [_] (put! (nth chs 6) false))
                 (fn [_] (put! (nth chs 6) true)))
    (tw/once (tw/query rr1 {:order-by-value true :limit-to-last 1})
             (fn [_] (put! (nth chs 7) true)))
    (tw/once (tw/query tr1 {:order-by-key true :limit-to-last 1})
             (fn [_] (put! (nth chs 8) true)))
    ;; may as well assert data on instead of ignoring snapshot
    ;; still need to test channel subs
    #? (:cljs
        (async done
          (take! res-ch
            (fn [res]
              (is (every? true? res))
              (done))))
        :clj
        (is (every? true? (<!! res-ch))))))

;; deftest on for FBRef, FBQuery, TwigQuery
;; assert failure for TwigRef
;; test all query options with and without error callback

;; deftest off for FBRef, FBQuery, TwigQuery
;; assert failure for TwigRef

; (deftest twig-query-snaps
;   (let [r (tw/reference "https://twigs.firebaseio.com/a")
;         q (tw/query r)
;         ch (chan)
;         do-test (fn [[k {:keys [b] :as a}]]
;                   (tw/unsub q)
;                   (is (= k :a))
;                   (is (= (count a) 1))
;                   (is (= (count b) 2))
;                   (is (not (realized? a)))
;                   (is (not (realized? b)))
;                   (is (= @b {:c "hello" :d "world"}))
;                   (is (not (realized? a)))
;                   (is (realized? b))
;                   (is (realized? (:c b)))
;                   (doseq [[k ss] b]
;                     (is (or (= k :c) (= k :d)))
;                     (is (realized? ss))
;                     (is (or (= @ss "hello") (= @ss "world")))
;                     (is (= (count ss) 0))
;                     (is (nil? (seq ss)))
;                     (is (nil? (get ss :nothing nil)))
;                     (is (realized? (:nothing ss)))
;                     (is (= (count (:nothing ss)) 0))
;                     (is (nil? (deref (:nothing ss))))))]
;     (tw/sub q "value" ch)
;     #?(:cljs (async done (take! ch (fn [ss] (do-test ss) (done))))
;        :clj (do-test (<!! ch)))
;     ))
;
