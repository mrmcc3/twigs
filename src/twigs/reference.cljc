(ns twigs.reference
  (:refer-clojure :exclude [ref])
  #?(:cljs (:require [cljsjs.firebase])
     :clj (:import [clojure.lang IPersistentCollection IPersistentStack]
                   [com.firebase.client Firebase])))

;; wrapper type around firebase references

#?(:cljs
   (deftype TwigRef [ref]
     Object
     (toString [_] (.toString ref))

     IStack
     (-peek [_] (-> ref .key keyword))
     (-pop [_] (TwigRef. (.parent ref)))

     ICollection
     (-conj [_ c] (TwigRef. (.child ref (name c))))

     IEmptyableCollection
     (-empty [_] (TwigRef. (.root ref)))

     IEquiv ;; two twig references with the same url are equal
     (-equiv [_ other]
       (if (instance? TwigRef other)
         (= (str ref) (str other))
         false)))

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
  (TwigRef. (#?(:cljs js/Firebase. :clj Firebase.) url)))

