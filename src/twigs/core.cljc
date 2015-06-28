(ns twigs.core
  (:require [twigs.reference]
            [twigs.snapshot]
            [twigs.query]))

;; get a TwigRef from url
(def reference twigs.reference/reference)

;; get a TwigSnapshot from raw snapshot
(def snapshot twigs.snapshot/snapshot)

;; get a raw query from a raw ref (convenience)
(def raw-query twigs.query/raw-query)

;; subscribe a callback/channel to raw query or ref using
;; the global firebase registry. on returns callback for off
(def on twigs.query/on)
(def off twigs.query/off) ;; must provide callback (from on)

;; get a TwigQuery from a TwigRef
;; reset! gives the ability to change query options on the fly
(def query twigs.query/query)

;; similar to on/off except each query tracks subscribers
;; can unsub (locally) by topic or everything.
(def sub twigs.query/sub)
(def unsub twigs.query/unsub)

(def tr (reference "https://drft.firebaseio.com"))

(defn cb [ss]
  (println "here"))

(on tr "value" cb)



