(ns bluegenes.config
  (:require [cljs.reader :as reader]))

(def debug?
  ^boolean js/goog.DEBUG)

(when debug?
  (enable-console-print!))

;; These use delay so that the variables are only read on the first deref, and
;; then cached for subsequent derefs. This ensures that the values returned
;; will always be consistent throughout the session, even if the variables are
;; mutated.
(def server-vars (delay (some-> js/serverVars reader/read-string)))
(def init-vars (delay (some-> js/initVars reader/read-string)))

(defn read-default-ns []
  (keyword (:bluegenes-default-namespace @server-vars)))
