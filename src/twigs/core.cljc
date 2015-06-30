(ns twigs.core
  (:refer-clojure :exclude [ref])
  (:require [twigs.protocols :refer [-once -on! -off! -raw-ref]]
            [twigs.reference :refer [twig-ref]]
            [twigs.query :refer [twig-query raw-query*]]
            #?@(:cljs [[cljsjs.firebase]])))

;; refs

(def raw-ref -raw-ref)

(defn ref [o]
  (twig-ref (raw-ref o)))

;; queries

(def raw-query raw-query*)

(defn query
  ([o] (query o {}))
  ([o opts] (twig-query (atom (raw-query o opts)) (atom {}))))

(defn once
  ([q sub] (once q sub nil))
  ([q sub err] (-once q sub err)))

(defn on!
  ([q topic sub] (on! q topic sub nil))
  ([q topic sub err] (-on! q topic sub err)))

(def off! -off!)
