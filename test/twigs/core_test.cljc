(ns twigs.core-test
  (:require #?@(:clj [[clojure.test :refer [is testing deftest]]
                      [clojure.core.async :as ca :refer [sub unsub unsub-all chan take! <!! put!]]]
                :cljs [[cljs.test :refer-macros [is testing deftest async]]
                       [cljs.core.async :as ca :refer [sub unsub unsub-all chan take! put!]]])
            [twigs.core :as tw]))

(defn async-test
  ([test-fn data-ch]
   (async-test test-fn data-ch (fn [])))
  ([test-fn data-ch done-cb]
   #? (:cljs
       (async done
         (take! data-ch
           (fn [data]
             (test-fn data)
             (done-cb)
             (done))))
       :clj
       (do
         (test-fn (<!! data-ch))
         (done-cb)))))

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

;; once tests

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

(deftest once-query-success
  (let [q (-> base-url (str "/read-only/data")
             (tw/query {:order-by-value true :equal-to "hello world"}))
        ch1 (chan) ch2 (chan)
        test-fn (fn [[_ ss]]
                  (is (= 1 (count ss)))
                  (is (= "hello world" @(:string ss))))]
    (tw/once q #(put! ch1 %))
    (tw/once q ch2)
    (async-test test-fn ch1)
    (async-test test-fn ch2)))

;; on/off tests

(deftest on-raw-ref-success
  (let [rr (-> base-url (str "/read-only/data") tw/raw-ref)
        ch1 (chan) ch2 (chan)
        cb #(put! ch1 %)
        test-fn (fn [ss]
                  (is (= (-> ss tw/snapshot val deref)
                         {:float 1.5
                          :int 1
                          :string "hello world"})))
        off1 (tw/on! rr "value" cb)
        off2 (tw/on! rr "value" ch2)]
    (async-test test-fn ch1 #(tw/off! rr "value" off1))
    (async-test test-fn ch2 #(tw/off! rr "value" off2))))

(deftest on-raw-ref-failure
  (let [rr (-> base-url (str "/no-access") tw/raw-ref)
        ch1 (chan) ch2 (chan)
        cb #(put! ch1 %)
        test-fn (fn [_] (is true))
        off1 (tw/on! rr "value" (fn [_]) cb)
        off2 (tw/on! rr "value" (chan) ch2)]
    (async-test test-fn ch1 #(tw/off! rr "value" off1))
    (async-test test-fn ch2 #(tw/off! rr "value" off2))))

(deftest on-raw-query-success
  (let [rq (-> base-url (str "/read-only/data")
             (tw/raw-query {:order-by-value true :limit-to-first 1}))
        ch1 (chan) ch2 (chan)
        cb #(put! ch1 %)
        test-fn (fn [ss]
                  (is (= (-> ss tw/snapshot val deref)
                         {:int 1})))
        off1 (tw/on! rq "value" cb)
        off2 (tw/on! rq "value" ch2)]
    (async-test test-fn ch1 #(tw/off! rq "value" off1))
    (async-test test-fn ch2 #(tw/off! rq "value" off2))))

(deftest on-query-success
  (let [q (-> base-url (str "/read-only/data")
             (tw/query {:order-by-value true :limit-to-first 1}))
        ch1 (chan) ch2 (chan)
        cb #(put! ch1 %)
        test-fn (fn [[_ ss]] (is (= @ss {:int 1})))]
    (tw/on! q "value" cb)
    (tw/on! q "value" ch2)
    (async-test test-fn ch1 #(tw/off! q "value" cb))
    (async-test test-fn ch2 #(tw/off! q "value" ch2))))

;; off! topic and off! all tests on twig-query

;; reset! test on twig-query

;; more in depth ss tests. count, realized?, seq
