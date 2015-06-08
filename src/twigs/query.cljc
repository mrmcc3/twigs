(ns twigs.query
  #?(:cljs (:require [twigs.snapshot :refer [wrap-snapshot]]
                     [cljs.core.async :as ca]
                     [cljsjs.firebase])
     :clj (:require [twigs.snapshot :refer [wrap-snapshot]]
                    [clojure.core.async :as ca]))
  #?(:clj (:import [com.firebase.client Firebase])))

;; A TwigQuery is a publisher of events at a given firebase reference
;; and with a set of query options.

;; you can subscribe to events using the standard core.async sub function
;; with a topic of :value :child_added :child_removed :child_changed or :child_moved

;; Each query keeps track of its own registered subscribers (queries are stateful)
;; there is no global registry of subscribers.

;; WIP still needs clj impl.

#?(:cljs
   (deftype TwigQuery [q subs]
     ca/Pub
     (sub* [_ topic ch close?] ;; could be ch-or-fn for callback style ??
       (let [cb #(ca/put! ch (wrap-snapshot %))
             t (name topic)]
         (when-not (get-in @subs [t ch])
           (.on q t cb)
           (swap! subs assoc-in [t ch] cb))))
     (unsub* [_ topic ch]
       (let [t (name topic)]
         (when-let [cb (get-in @subs [t ch])]
           (.off q t cb)
           (swap! subs update-in [t] dissoc ch))))
     (unsub-all* [this]
       (doseq [[topic _] @subs]
         (ca/unsub-all* this topic)))
     (unsub-all* [this topic]
       (doseq [[ch _] (get @subs topic)]
         (ca/unsub* this topic ch)))))

(defn query [twig-ref
             {:keys [order-by-child order-by-value order-by-key order-by-priority
                     start-at end-at equal-to limit-to-first limit-to-last]}]
  #?(:cljs
  (let [q (cond-> (.-ref twig-ref)
            order-by-child (.orderByChild order-by-child)
            order-by-value .orderByValue
            order-by-key .orderByKey
            order-by-priority .orderByPriority
            start-at (.startAt start-at)
            end-at (.endAt end-at)
            equal-to (.equalTo equal-to)
            limit-to-first (.limitToFirst limit-to-first)
            limit-to-last (.limitToLast limit-to-last))]
    (TwigQuery. q (atom {})))))

