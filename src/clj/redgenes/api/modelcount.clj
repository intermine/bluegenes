(ns redgenes.api.modelcount
  (:require [ring.util.response :refer [response]]
            [redgenes.redis :refer [wcar*]]
            [taoensso.carmine :as car :refer (wcar)]))


(defn modelcount [paths]
    {:Genes 14 :Genes.proteins 2 :hi (wcar* (car/get "Foo"))}
  )
