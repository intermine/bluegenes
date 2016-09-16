(ns redgenes.api.modelcount
  (:require [ring.util.response :refer [response]]
            [redgenes.redis :refer [wcar*]]
            [taoensso.carmine :as car :refer (wcar)]))


(defn modelcount [paths]
    (println (str  "paths " (type (first paths)) " " paths))
    (println "pathypoos" (map keyword (clojure.string/split paths #",")))
    (select-keys (wcar* (car/get "model-count")) (map keyword (clojure.string/split paths #",")))
  )
