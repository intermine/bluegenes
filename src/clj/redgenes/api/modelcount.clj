(ns redgenes.api.modelcount
  (:require [ring.util.response :refer [response]]
            [redgenes.redis :refer [wcar*]]
            [redgenes.mines :as mines]
            [clojure.string :refer [split trim]]
            [redgenes.api.modelcountcacher :as cacher]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.carmine :as car :refer (wcar)]))


(defn modelcount
  "given a set of paths separated by commas, we'll return the counts for each path. These paths need to be in this format:
  ```paths=[Gene, Gene.proteins, Gene.x.y]```
  don't include the last bit (i.e. no attributes like `Gene.id`) since it's looking up keys in the db without a great level of intelligence.
  "
  [paths mine]
  ;;TODO / note 3: I couldn't for the life of me get parameters to parse an array of values into a vector. Who knows why. I'd really rather *not* be splitting strings. I tried various ring middlewares and the only one that works for json parameters seemed to be ring.middleware.params/wrap-params. See the figwheel.clj for deets.
  ;;TODO 4: For security's sake we need to generate a schema with spec.
  (println "Retrieving details for" mine (wcar* (car/parse-map (car/hgetall (str "modelcount-" mine)))) (map #(trim %) (split paths #",")))

  (select-keys (wcar* (car/parse-map (car/hgetall (str "modelcount-" mine)))) (map #(trim %) (split paths #",")))
  )

  ;;;;;;;;;;;;; some longer term todo notes:
  ;;TODO 2: when this gets more advanced ensure it is self documenting in some way. swagger, openapi,

(defn cache
  "Method to manually kick off a cache-refresh. probably will be moved to a batch job when this is fully developed. "
  [mine]
  (cacher/load-model mine)
  )

(defn cacheall "All known mines? cache their results"
   []
   (cacher/load-all-models))
