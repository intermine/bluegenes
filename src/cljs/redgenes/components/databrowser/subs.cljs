(ns redgenes.components.databrowser.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

  (reg-sub
    :databrowser/whitelist
    (fn [db _]
      (:databrowser/whitelist db)))

  (reg-sub
    :databrowser/whitelisted-model
    (fn [db _]
      ;;We'll use these filtered values repeatedly so let's do it in one place
      (let [root (:databrowser/root db)
            model (:collections (root (:model (:assets db))))
          ;  model (:model (:assets db))
            whitelist (:databrowser/whitelist db)]
        (.log js/console "model" (clj->js model))
        ; (keep (fn [vals]
        ;   (cond (contains? whitelist (first vals)) vals)
        ; ) model)
        (select-keys model whitelist)
  )))
