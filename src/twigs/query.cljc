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
     (sub* [_ topic ch close?]
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

(defn query [twig-ref opts]
  #?(:cljs (TwigQuery. (.-ref twig-ref) (atom {}))))

