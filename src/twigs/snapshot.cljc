(ns twigs.snapshot
  #?(:clj (:import [com.firebase.client DataSnapshot]
                   [clojure.lang IPending IDeref ILookup MapEntry
                                 Associative Seqable Counted])))

;; wrapper type around firebase datasnapshot.

;; This is perhaps the main motivation for this library. It's tempting to just handle
;; all firebase snapshots with (-> ss .val js->clj) to get clojure
;; datastructures and all their goodness.

;; However I think it's reasonable to assume that firebase doesn't directly give
;; you raw data because .val can be expensive and wasteful (walking down a
;; firebase datasnapshot before calling .val removes a lot of uneeded work).
;; Otherwise why not just return the data?

;; we represent a firebase datasnapshot as a tuple [k v]
;; k is ss.key()
;; v is a TwigSnapshot which is a delayed clojure datastructure (derefable).
;; v is also seqable, countable and supports lookup

(deftype TwigSnapshot [ss d]
  Object
  (toString [_] "TwigSnapshot")

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
                (let [k (#?(:cljs .key :clj .getKey) css)]
                  #?(:cljs [k (-lookup this k)]
                     :clj (MapEntry. k (.valAt this k)))))
              snaps))))

  ILookup
  (#?(:cljs -lookup :clj valAt) [this k]
    (#?(:cljs -lookup :clj .valAt) this k
      (TwigSnapshot. (.child ss (name k)) (delay nil))))
  (#?(:cljs -lookup :clj valAt) [_ k nf]
    (if (.hasChild ss (name k))
      (let [css (.child ss (name k))]
        (TwigSnapshot. css
          (if (realized? d)
            (doto (delay (get @d (keyword k)) deref))
            (delay (-> css #?(:cljs .exportVal :clj (.getValue true))
                       #?(:cljs (js->clj :keywordize-keys true)))))))
      nf)))

(defn wrap-snapshot [raw-ss]
  (let [k (-> raw-ss #?(:cljs .key :clj .getKey) keyword)
        ss (TwigSnapshot. raw-ss
             (delay (-> raw-ss #?(:cljs .exportVal :clj (.getValue true))
                        #?(:cljs (js->clj :keywordize-keys true)))))]
    #?(:cljs [k ss] :clj (MapEntry. k ss))))

