(ns bluegenes.events.blog
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]))

(def default-rss "https://intermineorg.wordpress.com/?feed=rss")

(defn get-rss-from-db [db]
  (get-in db [:mines (:current-mine db) :rss] default-rss))

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
