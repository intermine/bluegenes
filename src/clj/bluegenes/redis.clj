(ns bluegenes.redis
  (:require [taoensso.carmine :as car :refer (wcar)]))

  (def server1-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}}) ; See `wcar` docstring for opts
  (defmacro wcar* [& body] `(car/wcar server1-conn ~@body))
