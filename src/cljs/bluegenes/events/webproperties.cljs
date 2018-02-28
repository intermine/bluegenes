(ns bluegenes.events.webproperties
  (:require [re-frame.core :refer [reg-event-db reg-event-fx subscribe]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]))

(defn web-properties-to-bluegenes
  "Map intermine web properties to bluegenes properties"
  [web-properties]
  {:name                         (get-in web-properties [:project :title])
   ;;todo - apparently this is in the model?
   :default-object-types         [:Gene :Protein]
   :default-organism             (get-in web-properties [:genomicRegionSearch :defaultOrganisms])
   ;;todo - apparently this is in the model?
   ;;https://github.com/intermine/intermine/issues/1631#issuecomment-316986480
   :default-selected-object-type :Gene
   :regionsearch-example         (get-in web-properties [:genomicRegionSearch :defaultSpans])
   :idresolver-example           {:Gene    (get-in web-properties [:bag :example :identifiers])
                                  ;; there should be details implemented for non-gene ID
                                  ;;defaults,
                                  ;; but we commented out the service for some reason
                                  ;; on intermine's side.
                                  ;; hopefully, it will return.
                                  ; :Protein "Q8T3M3,FBpp0081318,FTZ_DROME"
}
   :default-query-example        {;;to be done; need json queries though.
                                  ;;https://github.com/intermine/intermine/issues/1770
}})


; Fetch web properties
(reg-event-db
 :assets/success-fetch-web-properties
 (fn [db [_ mine-kw web-properties]]
   (let [original-properties    (get-in db [:mines (:current-mine db)])
         fetched-properties     (web-properties-to-bluegenes web-properties)]
      (assoc-in db [:mines mine-kw] (merge original-properties fetched-properties)))))

(reg-event-fx
 :assets/fetch-web-properties
 (fn [{db :db}]
   {:db db
    :im-chan {:chan (fetch/web-properties (get-in db [:mines (:current-mine db) :service]))
              :on-success [:assets/success-fetch-web-properties (:current-mine db)]}}))
