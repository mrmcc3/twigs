(ns twigs.core
  (:refer-clojure :exclude [ref])
  #?(:cljs
      (:require [cljsjs.firebase])))

#?(:cljs
    (deftype TwigRef [fb-ref]

      Object
      (toString [_] (.toString fb-ref))

      IStack
      (-peek [_] (-> fb-ref .key keyword))
      (-pop [_] (TwigRef. (.parent fb-ref)))

      ICollection
      (-conj [_ c] (TwigRef. (.child fb-ref (name c))))

      IEmptyableCollection
      (-empty [_] (TwigRef. (.root fb-ref)))

      IEquiv
      (-equiv [_ other]
        (if (instance? TwigRef other)
          (identical? (str fb-ref) (str other))
          false))))

(defn ref [url]
  #?(:cljs (TwigRef. (js/Firebase. url))))

