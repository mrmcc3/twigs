(ns twigs.core
  (:require [twigs.reference]
            [twigs.snapshot]
            [twigs.query]))

(def url->ref twigs.reference/wrap-reference)

(def wrap-ss twigs.snapshot/wrap-snapshot)

(def query twigs.query/query)

