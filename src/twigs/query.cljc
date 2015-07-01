(ns twigs.query
  (:require [twigs.protocols :refer [IPub IRef ->raw-ref -off!]]
            [twigs.snapshot :refer [->snapshot]]
            #?@(:cljs
                [[cljs.core.async :refer [put!]]
                 [cljs.core.async.impl.protocols :refer [WritePort]]
                 [twigs.reference :refer [TwigRef]]
                 [cljsjs.firebase]]
                :clj
                [[clojure.core.async :refer [put!]]
                 [clojure.core.async.impl.protocols :refer [WritePort]]]))
  #? (:clj (:import [clojure.lang IAtom]
                    [twigs.reference TwigRef]
                    [com.firebase.client Firebase Query
                     ValueEventListener ChildEventListener])))

(defn ^:private sub->cb [sub wrapper]
  (cond
    (satisfies? WritePort sub)
    (if wrapper
      #(put! sub (wrapper %))
      #(put! sub %))
    (ifn? sub)
    (if wrapper
      #(sub (wrapper %))
      sub)
    :else (throw
           #? (:cljs (js/Error. "Subscribers must be channels or callbacks")
               :clj (Exception. "Subscribers must be channels or callbacks")))))

(defn ^:private once* [r sub err-sub wrapper]
  (let [cb (sub->cb sub wrapper)
        err-cb (if err-sub (sub->cb err-sub nil))]
    #? (:cljs
        (if err-cb
          (.once r "value" cb err-cb)
          (.once r "value" cb))
        :clj
        (.addListenerForSingleValueEvent r
          (reify ValueEventListener
            (onDataChange [_ ss] (cb ss))
            (onCancelled [_ err] (if err-cb (err-cb err))))))
    sub))

(defn ^:private on* [r topic sub err-sub wrapper]
  (let [cb (sub->cb sub wrapper)
        err-cb (if err-sub (sub->cb err-sub nil))]
    #? (:cljs
        (if err-cb
          (.on r topic cb err-cb)
          (.on r topic cb))
        :clj
        (case topic
          "value"
          (.addValueEventListener r
            (reify ValueEventListener
              (onDataChange [_ ss] (cb ss))
              (onCancelled [_ err] (if err-cb (err-cb err)))))
          (.addChildEventListener r
            (reify ChildEventListener
              (onChildAdded [_ ss pc] (case topic "child_added" (cb ss pc) nil))
              (onChildChanged [_ ss pc] (case topic "child_changed" (cb ss pc) nil))
              (onChildMoved [_ ss pc] (case topic "child_moved" (cb ss pc) nil))
              (onChildRemoved [_ ss] (case topic "child_removed" (cb ss) nil))
              (onCancelled [_ err] (if err-cb (err-cb err)))))))))

(defn ^:private off* [r topic cb]
  #? (:cljs (.off r topic cb)
      :clj (.removeEventListener r cb)))

(extend-protocol IPub

  #? (:cljs js/Firebase :clj Firebase)
  (-once [r sub err] (once* r sub err nil))
  (-on! [r topic sub err] (on* r topic sub err nil))
  (-off! [r topic cb] (off* r topic cb))

  #? (:cljs object :clj Query)
  (-once [q sub err] (once* q sub err nil))
  (-on! [q topic sub err] (on* q topic sub err nil))
  (-off! [q topic cb] (off* q topic cb))

  TwigRef
  (-once [tr sub err]
    (once* (->raw-ref tr) sub err ->snapshot)))

(defn ->raw-query
  [r {:keys [order-by-child order-by-value order-by-key order-by-priority
             start-at end-at equal-to limit-to-first limit-to-last]}]
   (cond-> (->raw-ref r)
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

(deftype TwigQuery [q subs] ;; ^:mutable ^:unsynchronized-mutable instead of atom?
  IPub
  (-once [_ sub err]
    (once* @q sub err ->snapshot)
    nil)
  (-on! [_ topic sub err]
    (if-not (get-in @subs [topic sub])
      (swap! subs assoc-in [topic sub]
        [(on* @q topic sub err ->snapshot) err]))
    nil)
  (-off! [_ topic sub]
    (when-let [[cb _] (get-in @subs [topic sub])]
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
  (->raw-ref [_] (->raw-ref @q))

  ;; use reset! to change the queries options (orderBy, limitTo, etc..)
  #? (:clj IAtom :cljs IReset)
  (#? (:clj reset :cljs -reset!) [_ opts]
      (let [rr (#? (:cljs .ref :clj .getRef) @q)
            nq (->raw-query rr opts)]
        (doseq [[topic m] @subs
                [_ [cb err]] m]
          (off* @q topic cb)
          (on* nq topic cb err ->snapshot))
        (reset! q nq)
        nil)))

;; factory fn
(defn ->query [q subs]
  (TwigQuery. q subs))
