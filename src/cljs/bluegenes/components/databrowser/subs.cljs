(ns bluegenes.components.databrowser.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

  (reg-sub
    :databrowser/whitelist
    (fn [db _]
      (:databrowser/whitelist db)))

  (reg-sub
    :databrowser/viewport
    (fn [db _]
      (:databrowser/viewport db)))

  (reg-sub
    :databrowser/root
    (fn [db _]
      (if (some? (:databrowser/root db))
        (:databrowser/root db)
        "InterMine")
      ))

  (reg-sub
    :databrowser/whitelisted-model
    (fn [db [_ start-from]]
      ;;We'll use these filtered values repeatedly so let's do it in one place
      (let [model (if (some? start-from)
              (:collections (start-from (:model (:assets db))))
              (:model (:assets db)))
            whitelist (:databrowser/whitelist db)]
        (select-keys model whitelist)
  )))

  (reg-sub
    :databrowser/model-counts
    (fn [db [_ mine]]
      (mine (:databrowser/model-counts db))))

(reg-sub
 :databrowser/node-locations
 (fn [db [_ node-name]]
   (node-name (:databrowser/node-locations db))))
