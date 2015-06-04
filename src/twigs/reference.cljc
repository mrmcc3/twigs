(ns twigs.reference
  (:refer-clojure :exclude [ref])
  (:require [twigs.snapshot :refer [wrap-snapshot]]
            #?@(:cljs [cljsjs.firebase]))
  #?(:clj (:import [clojure.lang IPersistentCollection IPersistentStack]
                   [com.firebase.client Firebase])))

;; wrapper type around firebase references

#?(:cljs
   (deftype TwigRef [ref watches]
     Object
     (toString [_] (.toString ref))

     IStack
     (-peek [_] (-> ref .key keyword))
     (-pop [_] (TwigRef. (.parent ref) watches))

     ICollection
     (-conj [_ c] (TwigRef. (.child ref (name c)) watches))

     IEmptyableCollection
     (-empty [_] (TwigRef. (.root ref) watches))

     IEquiv ;; two twig references with the same url are equal
     (-equiv [_ other]
       (if (instance? TwigRef other)
         (= (str ref) (str other))
         false))

     ;; alpha!
     IWatchable
     (-add-watch [this k f]
       (if-let [g (get watches k)]
         (.off ref "value" g))
       (.on ref "value" (fn [ss] (f (wrap-snapshot ss))))
       (set! (.-watches this) (assoc watches k f)))
     (-remove-watch [this k]
       (if-let [f (get watches k)]
         (.off ref "value" f))
       (set! (.-watches this) (dissoc watches k))))

   :clj
   (deftype TwigRef [ref]
     Object
     (toString [_] (.toString ref))

     IPersistentStack
     (peek [_] (-> ref .getKey keyword))
     (pop [_] (TwigRef. (.getParent ref)))

     IPersistentCollection
     (cons [_ c] (TwigRef. (.child ref (name c))))
     (empty [_] (TwigRef. (.getRoot ref)))
     (equiv [_ other]
       (if (instance? TwigRef other)
         (= (str ref) (str other))
         false))))

(defn wrap-reference [url]
  #?(:cljs (TwigRef. (js/Firebase. url) {})
     :clj (TwigRef. (Firebase. url))))

