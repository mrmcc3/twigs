(ns twigs.utils)

#?(:clj
   (defprotocol ConvertibleToClojure
     (-clj [o])))

#?(:clj
   (extend-protocol ConvertibleToClojure
     java.util.HashMap
     (-clj [o]
       (let [entries (.entrySet o)]
         (reduce (fn [m [^String k v]]
                   (assoc m (keyword k) (-clj v)))
                 {} entries)))

     java.util.List
     (-clj [o] (vec (map -clj o)))

     java.lang.Object
     (-clj [o] o)

     nil
     (-clj [_] nil)))

(defn ->clj [o]
  #?(:cljs (js->clj o :keywordize-keys true)
     :clj (-clj o)))
