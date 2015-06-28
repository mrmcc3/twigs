(ns twigs.query
  #? (:cljs
      (:require [cljs.core.async :refer [put!]]
                [cljs.core.async.impl.protocols :refer [WritePort]]
                [cljsjs.firebase])
      :clj
      (:require [clojure.core.async :refer [put!]]
                [clojure.core.async.impl.protocols :refer [WritePort]]))
  #? (:clj
      (:import [clojure.lang IAtom]
               [com.firebase.client Firebase ValueEventListener ChildEventListener])))

;; returns a callback function when given a valid subscriber
(defn sub->cb [sub]
  (cond
    (satisfies? WritePort sub) #(put! sub %)
    (ifn? sub) sub
    :else (throw
           #? (:cljs (js/Error. "Subscribers must be channels or callbacks")
               :clj (Exception. "Subscribers must be channels or callbacks")))))

;; Provides an "on" function for Firebase's verbose Java API that's like the JS API. 
(defn on
  ([rq topic sub] (on rq topic sub nil))
  ([rq topic sub err-sub]
   (let [cb (sub->cb sub)
         err-cb (if err-sub (sub->cb err-sub))]
     #? (:cljs
         (if err-cb
           (.on rq topic cb err-cb)
           (.on rq topic cb))
         :cljs
         (case topic
           "value"
           (.addValueEventListener rq
             (reify ValueEventListener
               (onDataChange [_ ss] (cb ss))
               (onCancelled [_ err] (if err-cb (err-cb err)))))
           (.addChildEventListener rq
             (reify ChildEventListener
               (onChildAdded [_ ss pc] (case topic "child_added" (cb ss pc) nil))
               (onChildChanged [_ ss pc] (case topic "child_changed" (cb ss pc) nil))
               (onChildMoved [_ ss pc] (case topic "child_moved" (cb ss pc) nil))
               (onChildRemoved [_ ss] (case topic "child_removed" (cb ss) nil))
               (onCancelled [_ err (if err-cb (err-cb err))]))))))))

;; Provides a uniform "off" function
(defn off [rq topic cb]
  #? (:cljs (.off rq topic cb) :clj (.removeEventListener rq cb)))

;; Protocol for event publishers. allows subscriptions to be turned on/off"
(defprotocol IPub
  (sub [_ topic sub] [_ topic sub err-sub])
  (unsub [_] [_ topic] [_ topic sub]))

 ;; convenience function to construct a firebase query when given a reference
 ;;  and a map of options (order-by-..., start-at, end-at, equal-to, limit-to-...)
(defn raw-query
  [raw-ref {:keys [order-by-child order-by-value order-by-key order-by-priority
                   start-at end-at equal-to limit-to-first limit-to-last]}]
   (cond-> raw-ref
     order-by-child (.orderByChild order-by-child)
     order-by-value .orderByValue
     order-by-key .orderByKey
     order-by-priority .orderByPriority
     start-at (.startAt start-at)
     end-at (.endAt end-at)
     equal-to (.equalTo equal-to)
     limit-to-first (.limitToFirst limit-to-first)
     limit-to-last (.limitToLast limit-to-last)))

;; A TwigQuery is a publisher of events at a given firebase reference
;; with a set of query options. each query keeps track of its own
;; subscribers (queries are stateful)
(deftype TwigQuery [^:mutable ^:unsynchronized-mutable q subs]
  IPub

  ;; you can subscribe to events using sub with a topic of ("value"
  ;; "child_added" "child_removed" "child_changed" or "child_moved")
  ;; and either a channel or a callback. The channel or callback will
  ;; be supplied with a RAW firebase snapshot. You can also supply
  ;; an optional error channel or callback for cancelations
  (sub [this topic sub] (sub this topic sub nil))
  (sub [_ topic sub err-sub]
    (if-not (get-in @subs [topic sub])
      (swap! subs assoc-in [topic sub]
             (on q topic sub err-sub)))
    nil)

  ;; you can unsubscribe everything, a topic or a topic+sub with unsub
  (unsub [_ topic sub]
    (when-let [cb (get-in @subs [topic sub])]
      (off q topic cb)
      (swap! subs update-in [topic] dissoc sub))
      nil)
  (unsub [this topic]
    (doseq [[sub _] (get @subs topic)]
      (unsub this topic sub)))
  (unsub [this]
    (doseq [[topic m] @subs
            [sub _] m]
      (unsub this topic sub)))

  ;; you can use reset! to change the queries options (orderBy, limitTo, etc..)
  #? (:clj IAtom :cljs IReset)
  (#? (:clj reset :cljs -reset!) [this opts]
      (let [raw-ref (#? (:cljs .ref :clj .getRef) q)
            nq (raw-query raw-ref opts)]
        (doseq [[topic m] @subs
                [_ cb] m]
          (off q topic cb)
          (on nq topic cb))
        (set! q nq)
        nil)))

(defn query
  "construct a TwigQuery from a TwigRef and query options"
  ([twig-ref] (query twig-ref {}))
  ([twig-ref opts]
   (TwigQuery. (raw-query (.-ref twig-ref) opts) (atom {}))))

