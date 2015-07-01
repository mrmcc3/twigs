(ns twigs.core
  (:refer-clojure :exclude [ref])
  (:require [twigs.protocols :refer [-once -on! -off! ->raw-ref]]
            [twigs.reference :refer [->ref]]
            [twigs.query :refer [->query ->raw-query]]
            [twigs.snapshot :refer [->snapshot ss->clj*]]
            #?@(:cljs [[cljsjs.firebase]])))

;; refs

(def raw-ref ->raw-ref)

(defn ref [o]
  (->ref (raw-ref o)))

;; queries

(def raw-query ->raw-query)

(defn query
  ([o] (query o {}))
  ([o opts] (->query (atom (raw-query o opts)) (atom {}))))

(defn once
  ([q sub] (once q sub nil))
  ([q sub err] (-once q sub err)))

(defn on!
  ([q topic sub] (on! q topic sub nil))
  ([q topic sub err] (-on! q topic sub err)))

(def off! -off!)

;; snapshots

(def snapshot ->snapshot)

(def ss->clj ss->clj*)
