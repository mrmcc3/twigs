(ns twigs.core-test
  (:require #?@(:clj [[clojure.test :refer [is testing deftest]]
                      [clojure.core.async :as ca :refer [sub unsub unsub-all chan take! <!! put!]]]
                :cljs [[cljs.test :refer-macros [is testing deftest async]]
                       [cljs.core.async :as ca :refer [sub unsub unsub-all chan take! put!]]])
            [twigs.core :as tw]))

(defn async-test [test-fn data-ch]
  #? (:cljs
      (async done
        (take! data-ch
          (fn [data]
            (test-fn data)
            (done))))
      :clj
      (test-fn (<!! data-ch))))

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

(def base-url "https://twigs.firebaseio.com")

;; basic ref tests
(deftest twig-refs
  (let [url (str base-url "/a")
        r (tw/ref url)
        rr (tw/raw-ref url)
        q (tw/query url {:limit-to-first 10})
        rq (tw/raw-query url {:equal-to "sdkjalskj"})]
    (is (apply =
          (map #(.toString %)
               [rr (tw/raw-ref r) (tw/raw-ref q) (tw/raw-ref rq)])))
    (is (= r (tw/ref rr) (tw/ref q) (tw/ref rq)))
    (is (= (str r) url))
    (is (= (peek r) :a))
    (is (= (pop r) (empty r)))
    (is (not= (pop r) r))
    (is (= (conj r :b) (tw/ref (str url "/b"))))
    (is (= r (-> r (conj :c) pop)))))


(deftest once-raw-ref-success
  (let [rr (-> base-url (str "/read-only/data") tw/raw-ref)
        ch1 (chan) ch2 (chan)
        test-fn (fn [ss]
                  (is (= (-> ss tw/snapshot val deref)
                         {:float 1.5
                          :int 1
                          :string "hello world"})))]
    (tw/once rr #(put! ch1 %))
    (tw/once rr ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

(deftest once-raw-ref-failure
  (let [rr (-> base-url (str "/no-access") tw/raw-ref)
        ch1 (chan) ch2 (chan)
        test-fn (fn [err] (is true))]
    (tw/once rr (fn [_]) #(put! ch1 %))
    (tw/once rr (chan) ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

(deftest once-ref-success
  (let [r (-> base-url tw/ref (conj :read-only :data))
        ch1 (chan) ch2 (chan)
        test-fn (fn [[k {:keys [float int string]}]]
                  (is (= k :data))
                  (is (= @float 1.5))
                  (is (= @int 1))
                  (is (= @string "hello world")))]
    (tw/once r #(put! ch1 %))
    (tw/once r ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

(deftest once-ref-failure
  (let [r (-> base-url tw/ref (conj :no-access))
        ch1 (chan) ch2 (chan)
        test-fn (fn [err] (is true))]
    (tw/once r (fn [_]) #(put! ch1 %))
    (tw/once r (chan) ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

(deftest once-raw-query-success
  (let [rq (-> base-url (str "/read-only/data")
             (tw/raw-query {:order-by-value true :limit-to-first 2}))
        ch1 (chan) ch2 (chan)
        test-fn (fn [ss]
                  (is (= (-> ss tw/snapshot val deref)
                         {:float 1.5
                          :int 1})))]
    (tw/once rq #(put! ch1 %))
    (tw/once rq ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

;; deftest once on FBRef, TwigRef, FBQuery, TwigQuery
;; with and without error callback
; (deftest fb-ref-once
;   (let [url "https://twigs.firebaseio.com"
;         rr1 (-> url tw/raw-ref (.child "read-only"))
;         rr2 (-> url tw/raw-ref (.child "no-access"))
;         tr1 (-> url tw/ref (conj :read-only))
;         tr2 (-> url tw/ref (conj :no-access))
;         chs (into [] (take 9 (repeatedly #(chan))))
;         res-ch (ca/map vector chs)]
;     (tw/once rr1 (fn [_] (put! (nth chs 0) true)))
;     (tw/once rr1 (fn [_] (put! (nth chs 1) true))
;                  (fn [_] (put! (nth chs 1) false)))
;     (tw/once rr2 (fn [_] (put! (nth chs 2) false)))
;     (tw/once rr2 (fn [_] (put! (nth chs 2) false))
;                  (fn [_] (put! (nth chs 2) true)))
;     (tw/once (tw/raw-query rr1 {:order-by-key true :limit-to-first 1})
;              (fn [_] (put! (nth chs 3) true)))
;     (tw/once rr1 (fn [_] (put! (nth chs 4) true)))
;     (tw/once rr1 (fn [_] (put! (nth chs 5) true))
;                  (fn [_] (put! (nth chs 5) false)))
;     (tw/once rr2 (fn [_] (put! (nth chs 6) false)))
;     (tw/once rr2 (fn [_] (put! (nth chs 6) false))
;                  (fn [_] (put! (nth chs 6) true)))
;     (tw/once (tw/query rr1 {:order-by-value true :limit-to-last 1})
;              (fn [_] (put! (nth chs 7) true)))
;     (tw/once (tw/query tr1 {:order-by-key true :limit-to-last 1})
;              (fn [_] (put! (nth chs 8) true)))
;     ;; may as well assert data on instead of ignoring snapshot
;     ;; still need to test channel subs
;     #? (:cljs
;         (async done
;           (take! res-ch
;             (fn [res]
;               (is (every? true? res))
;               (done))))
;         :clj
;         (is (every? true? (<!! res-ch))))))

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
