(ns bluegenes.events.registry
  (:require [re-frame.core :refer [reg-event-fx dispatch]]
            [imcljs.fetch :as fetch]
            [clojure.string :as string]
            [bluegenes.utils :refer [read-registry-mine]]
            [bluegenes.version :as version]
            [bluegenes.config :refer [server-vars read-default-ns]]
            [bluegenes.route :as route]))

(reg-event-fx
 ;; these are the intermines we'll allow users to switch to
 ::load-other-mines
 ;; `?id` is only used in a few cases when initiated from a message.
 (fn [_ [_ ?id]]
   (cond-> {}
     (not (:hide-registry-mines @server-vars))
     (assoc :im-chan {:chan (fetch/registry false)
                      :on-success [::success-fetch-registry]
                      :on-failure [::failure-fetch-registry]})

     ?id (assoc :dispatch [:messages/remove ?id]))))

(def protocol (delay (.. js/window -location -protocol)))

(defn compatible-protocol?
  "If the active protocol is secure, it will return false for insecure URLs.
  If the active protocol is insecure, it will return true for all URLs."
  [url]
  (if (= @protocol "https:")
    (not (string/starts-with? url "http:"))
    true))

(reg-event-fx
 ::failure-fetch-registry
 (fn [_ [_]]
   {:dispatch [:messages/add
               {:markup (fn [id]
                          [:span "Unable to contact " [:strong "InterMine registry"]
                           ". It's either down or there is a problem with your connection. You will not be able to switch to other mines in the registry. "
                           [:a {:role "button"
                                :on-click #(dispatch [::load-other-mines id])}
                            "Click here"]
                           " to try again."])
                :style "warning"
                :timeout 10000}]}))

(reg-event-fx
 ::success-fetch-registry
 (fn [{db :db} [_ mines]]
   (let [;; they *were* in an array, but a map would be easier to reference mines
         [registry registry-external]
         (reduce (fn [[compatible incompatible] mine]
                   (if (and (>= (js/parseInt (:api_version mine) 10)
                                version/minimum-intermine)
                            (compatible-protocol? (:url mine)))
                     [(assoc compatible (-> mine :namespace keyword) mine) incompatible]
                     [compatible (assoc incompatible (-> mine :namespace keyword) mine)]))
                 [{} {}]
                 mines)
         db-with-registry (assoc db
                                 :registry registry
                                 :registry-external registry-external)
         current-mine (:current-mine db)
         default-ns (read-default-ns)
         config-mines (get-in db [:env :mines])]
     (cond
       ;; Don't do anything special if the mine is from config.
       (contains? config-mines current-mine)
       {:db db-with-registry}
       ;; Change to the default mine if the target mine does not exist.
       (not (contains? registry current-mine))
       {:db (-> db-with-registry
                (assoc :current-mine default-ns)
                (assoc-in [:mines default-ns] (get-in db [:env :mines default-ns])))
        :dispatch [:messages/add
                   {:markup [:span "Your mine has been changed to the default as your selected mine " [:em (name current-mine)] " was not present in the registry."]
                    :style "warning"
                    :timeout 0}]
        :change-route (name default-ns)}
       ;; Fill in the mine details if it's missing.
       ;; (This happens when we load a registry mine at boot.)
       (nil? (get-in db-with-registry [:mines current-mine]))
       {:db (assoc-in db-with-registry [:mines current-mine]
                      (read-registry-mine (get registry current-mine)))}
       ;; If we ended up here it means we used a registry mine and the data is
       ;; already present. Yay!
       :else {:db db-with-registry}))))
