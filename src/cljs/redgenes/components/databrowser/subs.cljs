(ns redgenes.components.databrowser.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

  (reg-sub
    :databrowser/whitelist
    (fn [db _]
      (:databrowser/whitelist db)))

  (reg-sub
    :databrowser/root
    (fn [db _]
      (:databrowser/root db)))

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
