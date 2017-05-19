(ns bluegenes.api.modelcount
  (:require [ring.util.response :refer [response]]
            [bluegenes.redis :refer [wcar*]]
            [bluegenes.mines :as mines]
            [bluegenes.whitelist :as config]
            [clojure.string :refer [split trim starts-with?]]
            [bluegenes.api.modelcountcacher :as cacher]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.carmine :as car :refer (wcar)]))

(defn all-counts "get all cached counts for a given mine" [mine]
  (wcar* (car/parse-map (car/hgetall (str "modelcount-" mine)))))

(defn modelcount
  "given a set of paths separated by commas, we'll return the counts for each path. These paths need to be in this format:
  ```paths=[Gene, Gene.proteins, Gene.x.y]```
  don't include the last bit (i.e. no attributes like `Gene.id`) since it's looking up keys in the db without a great level of intelligence.
  "
  [paths mine]
  ;;TODO / note 3: I couldn't for the life of me get parameters to parse an array of values into a vector. Who knows why. I'd really rather *not* be splitting strings. I tried various ring middlewares and the only one that works for json parameters seemed to be ring.middleware.params/wrap-params. See the figwheel.clj for deets.

  ;;This next line doesn't have to be hgetall. We could individually select keys via hget or hmget if it's faster and matters.
  (let [responts (all-counts mine)]
    (if (= paths "top")
      ;;give all the paths at the top level, which incidentally appear on the whitelist
      (select-keys responts (map #(name %) config/whitelist))
      ;;give only selected paths
      (select-keys responts (map #(trim %) (split paths #","))))
  ))

  ;;TODO 2: when this gets more advanced ensure it is self documenting in some way. swagger, openapi,
  ;;etc. right now that would be massive overkill.

(defn cache
  "Method to manually kick off a cache-refresh for a single mine"
  [mine]
  (cacher/load-model mine)
  )

(defn cacheall "All known mines? cache their results"
   []
   (cacher/load-all-models))

(defn modelcount-children "You may want to know the counts of all of the *children* of a given path."
  [parent mine]
  (let [responts (all-counts mine)
        wanted-keys (filter (fn [k] (starts-with? k parent)) (keys responts))]
    (select-keys responts wanted-keys)
))

;;I can't use 'response' as a variable name because it means stuff in routing server responses. So I used "responts" instead. It's a thing of beauty. 
