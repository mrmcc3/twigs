(ns twigs.query
  (:require [twigs.protocols :refer [IPub IRef -raw-ref -off!]]
            #?@(:cljs
                [[cljs.core.async :refer [put!]]
                 [cljs.core.async.impl.protocols :refer [WritePort]]
                 [cljsjs.firebase]]
                :clj
                [[clojure.core.async :refer [put!]]
                 [clojure.core.async.impl.protocols :refer [WritePort]]]))
  #? (:clj (:import [clojure.lang IAtom]
                    [com.firebase.client Firebase Query
                     ValueEventListener ChildEventListener])))

;; returns a callback function when given a valid subscriber (channel or callback)
(defn ^:private sub->cb [sub]
  (cond
    (satisfies? WritePort sub) #(put! sub %)
    (ifn? sub) sub
    :else (throw
           #? (:cljs (js/Error. "Subscribers must be channels or callbacks")
               :clj (Exception. "Subscribers must be channels or callbacks")))))

(defn ^:private once* [raw-q-or-r sub err-sub]
  (let [cb (sub->cb sub)
        err-cb (if err-sub (sub->cb err-sub))]
    #? (:cljs
        (if err-cb
          (.once raw-q-or-r "value" cb err-cb)
          (.once raw-q-or-r "value" cb))
        :clj
        (.addListenerForSingleValueEvent raw-q-or-r
          (reify ValueEventListener
            (onDataChange [_ ss] (cb ss))
            (onCancelled [_ err] (if err-cb (err-cb err))))))
    sub))

(defn ^:private on* [raw-q-or-r topic sub err-sub]
  (let [cb (sub->cb sub)
        err-cb (if err-sub (sub->cb err-sub))]
    #? (:cljs
        (if err-cb
          (.on raw-q-or-r topic cb err-cb)
          (.on raw-q-or-r topic cb))
        :clj
        (case topic
          "value"
          (.addValueEventListener raw-q-or-r
            (reify ValueEventListener
              (onDataChange [_ ss] (cb ss))
              (onCancelled [_ err] (if err-cb (err-cb err)))))
          (.addChildEventListener raw-q-or-r
            (reify ChildEventListener
              (onChildAdded [_ ss pc] (case topic "child_added" (cb ss pc) nil))
              (onChildChanged [_ ss pc] (case topic "child_changed" (cb ss pc) nil))
              (onChildMoved [_ ss pc] (case topic "child_moved" (cb ss pc) nil))
              (onChildRemoved [_ ss] (case topic "child_removed" (cb ss) nil))
              (onCancelled [_ err] (if err-cb (err-cb err)))))))))

(defn ^:private off* [raw-q-or-r topic cb]
  #? (:cljs (.off raw-q-or-r topic cb)
      :clj (.removeEventListener raw-q-or-r cb)))

(extend-protocol IPub

  #? (:cljs object :clj Firebase)
  (-once [r sub err] (once* r sub err))
  (-on! [r topic sub err] (on* r topic sub err))
  (-off! [r topic cb] (off* r topic cb))

  #?@(:clj [
    Query
    (-once [q sub err] (once* q sub err))
    (-on! [q topic sub err] (on* q topic sub err))
    (-off! [q topic cb] (off* q topic cb))]))


(defn raw-query*
  [r {:keys [order-by-child order-by-value order-by-key order-by-priority
             start-at end-at equal-to limit-to-first limit-to-last]}]
   (cond-> (-raw-ref r)
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
(deftype TwigQuery [q subs] ;; ^:mutable ^:unsynchronized-mutable instead of atoms?
  IPub
  (-once [_ sub err]
    (once* @q sub err)
    nil)
  (-on! [_ topic sub err]
    (if-not (get-in @subs [topic sub])
      (swap! subs assoc-in [topic sub]
        (on* @q topic sub err)))
    nil)
  (-off! [_ topic sub]
    (when-let [cb (get-in @subs [topic sub])]
      (off* @q topic cb)
      (swap! subs update-in [topic] dissoc sub))
    nil)
  (-off! [this topic]
   (doseq [[sub _] (get @subs topic)]
     (-off! this topic sub)))
  (-off! [this]
    (doseq [[topic m] @subs
            [sub _] m]
      (-off! this topic sub)))

  IRef
  (-raw-ref [_] (-raw-ref @q))

  ; you can use reset! to change the queries options (orderBy, limitTo, etc..)
  ;; need to store error callbacks as well
  #? (:clj IAtom :cljs IReset)
  (#? (:clj reset :cljs -reset!) [_ opts]
      (let [rr (#? (:cljs .ref :clj .getRef) @q)
            nq (raw-query* rr opts)]
        (doseq [[topic m] @subs
                [_ cb] m]
          (off* @q topic cb)
          (on* nq topic cb nil))
        (reset! q nq)
        nil)))

(defn twig-query [q subs]
  (TwigQuery. q subs))
