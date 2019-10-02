(ns bluegenes.events.registry
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]))

;; this is not crazy to hardcode. The consequences of a mine that is lower than
;; the minimum version using bluegenes could potentially result in corrupt lists
;; so it *should* be hard to change.
;;https://github.com/intermine/intermine/issues/1482
(def min-intermine-version 27)

(reg-event-fx
 ;; these are the intermines we'll allow users to switch to
 ::load-other-mines
 (fn [{db :db}]
   {:im-chan
    {:chan (fetch/registry false)
     :on-success [::success-fetch-registry]}}))

(reg-event-fx
 ::success-fetch-registry
 (fn [{db :db} [_ mines]]
   (let [;; they *were* in an array, but a map would be easier to reference mines
         registry (into {} (comp (filter #(>= (js/parseInt (:api_version %) 10)
                                              min-intermine-version))
                                 (map (juxt (comp keyword :namespace) identity)))
                        mines)
         current-mine (:current-mine db)
         db-with-registry (assoc db :registry registry)]
     (cond
       ;; Don't do anything special if the mine is :default.
       (= current-mine :default)
       {:db db-with-registry}
       ;; Change to :default mine if the target mine does not exist.
       (not (contains? registry current-mine))
       {:db (assoc db-with-registry :current-mine :default)
        :dispatch [:messages/add
                   {:markup [:span (str "Your mine has been changed to the default as your selected mine '" (name current-mine) "' was not present in the registry.")]
                    :style "warning"}]}
       ;; Fill in the mine details if it's missing.
       ;; (This happens when we use a registry mine.)
       (nil? (get-in db-with-registry [:mines current-mine]))
       (let [{reg-url :url reg-name :name} (get registry current-mine)]
         {:db (assoc-in db-with-registry [:mines current-mine]
                        {:service {:root reg-url}
                         :name reg-name
                         :id current-mine})})
       ;; If we ended up here it means we used a registry mine and the data is
       ;; already present. Yay!
       :else {:db db-with-registry}))))
