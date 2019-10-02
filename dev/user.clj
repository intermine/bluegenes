;; This namespace starts figwheel as part of the aliases:
;;     lein dev
;;     lein repl
(ns user
  (:require [figwheel-sidecar.repl-api :refer [start-figwheel!]]))

(start-figwheel!)
