(ns bluegenes.components.tools.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.effects :as fx]
            [bluegenes.route :as route]))

(reg-event-fx
 ::fetch-tools
 (fn [{db :db} [_]]
   {:db db
    ::fx/http {:method :get
               :on-success [::store-tools]
               :uri (str "/api/tools/all")}}))

(reg-event-db
 ::store-tools
 (fn [db [_ tools]]
   (assoc-in db [:tools :all] (:tools tools))))

(reg-event-fx
 ::navigate-query
 (fn [{db :db} [_ query source]]
   (let [navigate [::route/navigate ::route/list {:title (:title query), :mine source}]
         history+ [:results/history+ {:source source, :type :query, :value query}]
         new-source? (not= source (:current-mine db))]
     {:dispatch (if new-source? navigate history+)
      ;; Use :dispatch-after-boot since [:results :queries] is cleared when switching mines.
      :db (cond-> db
            new-source? (update :dispatch-after-boot (fnil conj []) (conj history+ true)))})))
