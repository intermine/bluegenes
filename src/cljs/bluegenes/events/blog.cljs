(ns bluegenes.events.blog
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]))

;; MDRMine has no blog of its own; so we will be using the url of the github releases atom feed for the project as a default
;; and gives genuinely useful "what's new" content.
(def default-rss "https://github.com/ecrin-github/mdrmine/releases.atom")

(defn get-rss-from-db [db]
  (if-let [rss (not-empty (get-in db [:mines (:current-mine db) :rss]))]
    rss
    default-rss)), 

(reg-event-fx
 ::fetch-rss
 (fn [{db :db} [_]]
   (let [rss (get-rss-from-db db)]
     ;; Only fetch RSS if we haven't done it previously.
     (if (nil? (get-in db [:cache :rss rss]))
       {::fx/http {:uri "/api/rss/parse"
                   :method :get
                   :on-success [::fetch-rss-success rss]
                   :on-failure [::fetch-rss-failure rss]
                   :on-unauthorised [::fetch-rss-failure rss]
                   :query-params {:url rss}}}
       {}))))

(reg-event-db
 ::fetch-rss-success
 (fn [db [_ rss res]]
   (assoc-in db [:cache :rss rss] res)))

(reg-event-fx
 ::fetch-rss-failure
 (fn [{db :db} [_ rss res]]
   {:db (assoc-in db [:cache :rss rss] false)
    :log-error ["Fetch RSS failure" res]}))
