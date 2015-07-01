(ns twigs.reference
  (:require [twigs.protocols :refer [IRef]]
            #?@(:cljs [[cljsjs.firebase]]))
  #? (:clj (:import [clojure.lang IPersistentCollection IPersistentStack]
                    [com.firebase.client Firebase Query])))

;; thin wrapper type around firebase references

(deftype TwigRef [ref]
  Object
  (toString [_] (.toString ref))

  IRef
  (->raw-ref [_] ref)

  #?@(:cljs
      [IStack
       (-peek [_] (-> ref .key keyword))
       (-pop [_] (TwigRef. (.parent ref)))

       ICollection
       (-conj [_ c] (TwigRef. (.child ref (name c))))

       IEmptyableCollection
       (-empty [_] (TwigRef. (.root ref)))

       IEquiv
       (-equiv [_ other]
         (if (instance? TwigRef other)
           (= (str ref) (str other))
           false))]
      :clj
      [IPersistentStack
       (peek [_] (-> ref .getKey keyword))
       (pop [_] (TwigRef. (.getParent ref)))

       IPersistentCollection
       (cons [_ c] (TwigRef. (.child ref (name c))))
       (empty [_] (TwigRef. (.getRoot ref)))
       (equiv [_ other]
         (if (instance? TwigRef other)
           (= (str ref) (str other))
           false))]))

(defn ->ref [r]
  (TwigRef. r))

(extend-protocol IRef
  #?(:cljs string :clj String)
  (->raw-ref [s] #?(:cljs (js/Firebase. s) :clj (Firebase. s)))
  #?(:cljs js/Firebase :clj Firebase)
  (->raw-ref [r] r)
  #?(:cljs object :clj Query)
  (->raw-ref [q] #?(:cljs (.ref q) :clj (.getRef q))))
