(ns twigs.snapshot
  (:require [twigs.protocols :refer [IRef ->raw-ref]]
            [twigs.utils :refer [->clj]])
  #?(:clj (:import [com.firebase.client DataSnapshot]
                   [clojure.lang IPending IDeref ILookup MapEntry
                                 Associative Seqable Counted])))

(defn ss->clj* [raw-ss]
  (->clj #?(:cljs (.exportVal raw-ss)
            :clj (.getValue raw-ss true))))

;; wrapper type around firebase snapshots

(deftype TwigSnapshot [ss d]
  Object
  (toString [_] "TwigSnapshot")

  IRef
  (->raw-ref [_] (->raw-ref ss))

  IPending
  (#?(:cljs -realized? :clj isRealized) [_] (realized? d))

  IDeref
  (#?(:cljs -deref :clj deref) [_] @d)

  #?(:cljs ICounted :clj Counted)
  #?(:cljs (-count [_] (.numChildren ss))
     :clj (count [_] (.getChildrenCount ss)))

  #?(:cljs ISeqable :clj Seqable)
  (#?(:cljs -seq :clj seq) [this]
     (if (.hasChildren ss)
       (let [snaps #?(:cljs (array) :clj (.getChildren ss))]
         #?(:cljs (.forEach ss (fn [css] (.push snaps css) false)))
         (map (fn [css]
                (let [k (-> css #?(:cljs .key :clj .getKey) keyword)]
                  #?(:cljs [k (-lookup this k)]
                     :clj (MapEntry. k (.valAt this k)))))
              snaps))))

  ILookup
  (#?(:cljs -lookup :clj valAt) [this k]
    (#?(:cljs -lookup :clj .valAt) this k
      (TwigSnapshot. (.child ss (name k))
                     (doto (delay nil) deref))))
  (#?(:cljs -lookup :clj valAt) [_ k nf]
    (if (.hasChild ss (name k))
      (let [css (.child ss (name k))]
        (TwigSnapshot. css
          (if (realized? d)
            (doto (delay (get @d (keyword k))) deref)
            (delay (ss->clj* css)))))
      nf)))

(defn ->snapshot [raw-ss]
  (let [k (-> raw-ss #?(:cljs .key :clj .getKey) keyword)
        ss (TwigSnapshot. raw-ss (delay (ss->clj* raw-ss)))]
    #?(:cljs [k ss] :clj (MapEntry. k ss))))

; (extend-protocol IRef
;   #? (:cljs js/FirebaseDataSnapshot :clj DataSnapshot)
;   (->raw-ref [ss] (#?(:cljs .ref :clj .getRef) ss)))
