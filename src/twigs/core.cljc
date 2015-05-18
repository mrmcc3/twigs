(ns twigs.core
  (:require [twigs.reference :as rf]
            [twigs.snapshot :as ss]))

(def url->ref rf/wrap-reference)

(def wrap-ss ss/wrap-snapshot)


