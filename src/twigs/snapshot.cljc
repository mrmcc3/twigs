(ns twigs.snapshot
  #?(:clj (:import [clojure.lang IPending IDeref ILookup
                                 Associative Seqable Counted])))

;; wrapper type around firebase datasnapshot.

;; This is the main motivation for this library. It's tempting to just handle
;; all firebase snapshots with (-> ss .val js->clj) to get clojure
;; datastructures and all their goodness.

;; However I think it's reasonable to assume that firebase doesn't directly give
;; you raw data because .val can be expensive and wasteful (walking down a
;; firebase datasnapshot before calling .val removes a lot of uneeded work).
;; Otherwise why not just return the data?

;; we represent a firebase datasnapshot as a tuple [k v]
;; k is ss.key()
;; v is a TwigSnapshot which is a delayed clojure datastructure (derefable).
;; v is also associative, seqable, countable and supports lookup

#?(:cljs
   (deftype TwigSnapshot [ss d]
     Object
     (toString [_] "TwigSnapshot")

     IPending
     (-realized? [_] (realized? d))

     IDeref
     (-deref [_] @d)

     ILookup
     (-lookup [this k] (-lookup this k nil))
     (-lookup [_ k nf]
       (if (.hasChild ss (name k))
         (let [css (.child ss (name k))]
           (TwigSnapshot. css
                          (if (realized? d)
                            (doto (delay (get @d (keyword k)) deref))
                            (delay (-> css .exportVal
                                       (js->clj :keywordize-keys true))))))
         nf))

     IAssociative
     (-contains-key? [_ k] (.hasChild ss k))

     ISeqable
     (-seq [this]
       (if (.hasChildren ss)
         (let [snaps (array)]
           (.forEach ss (fn [css]
                          (.push snaps (.key css)) false))
           (map (fn [k] [k (-lookup this k)]) snaps))))

     ICounted
     (-count [_] (.numChildren ss)))

   :clj
   (deftype TwigSnapshot [ss d]
     Object
     (toString [_] "TwigSnapshot")

     IPending
     (isRealized [_] (realized? d))

     IDeref
     (deref [_] @d)))

(defn wrap-snapshot
  "converts a firebase snapshot to a twig snapshot."
  [raw-ss]
  #?(:cljs
      (let [n (.key raw-ss)
            ss (TwigSnapshot. raw-ss
                 (delay (-> raw-ss .exportVal
                            (js->clj :keywordize-keys true))))]
            [(keyword n) ss])))

