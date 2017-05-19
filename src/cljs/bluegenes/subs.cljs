(ns bluegenes.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            ; [bluegenes.components.databrowser.subs]
            [bluegenes.components.enrichment.subs]
            [bluegenes.mines :as mines]
            [clojure.string :refer [split]]
            [bluegenes.sections.querybuilder.subs]
            [bluegenes.components.search.subs]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

(reg-sub
  :short-name
  (fn [db]
    (:short-name db)))

(reg-sub
  :mine-url
  (fn [db]
    (let [mine-name (:mine-name db)
          url       (:url (:mine (mine-name (:mines db))))]
      (str "http://" url))))

(reg-sub :mine-default-organism
         (fn [db]
           (let [mine-name (:mine-name db)
                 organism  (:abbrev (mine-name (:mines db)))]
             organism)))

(reg-sub :mines
  (fn [db]
   (:mines db)))

(reg-sub
  :mine-name
  (fn [db]
    (:mine-name db)))

(reg-sub
  :current-mine-name
  (fn [db]
    (:current-mine db)))

(reg-sub
  :active-panel
  (fn [db _]
    (:active-panel db)))

(reg-sub
  :panel-params
  (fn [db _]
    (:panel-params db)))

(reg-sub
  :app-db
  (fn [db _] db))

(reg-sub
  :who-am-i
  (fn [db _]
    (:who-am-i db)))

(reg-sub
  :fetching-report?
  (fn [db _]
    (:fetching-report? db)))

(reg-sub
  :runnable-templates
  (fn [db _]
    (:templates (:report db))))

(reg-sub
  :collections
  (fn [db _]
    (:collections (:report db))))

(reg-sub
  :model
  (fn [db _]
    (let [current-mine (get-in db [:mines (get db :current-mine)])
          current-model (get-in current-mine [:service :model :classes])]
    current-model)))

(reg-sub
  :lists
  (fn [db [_]]
    (get-in db [:assets :lists])))

(reg-sub
  :listskw
  (fn [db [_]]
    (let [list-map (get-in db [:assets :lists])]
      (reduce (fn [total [minekw lists]]
                (doall (reduce (fn [col next-list]
                                 (assoc col (keyword (name minekw) (name (:name next-list))) next-list))
                               {} lists)))
              {} list-map))))

(reg-sub
  :summary-fields
  (fn [db _]
    (:summary-fields (:assets db))))

(reg-sub
  :report
  (fn [db _]
    (:report db)))

(reg-sub
  :progress-bar-percent
  (fn [db _]
    (:progress-bar-percent db)))

(reg-sub
  :saved-data
  (fn [db _]
    (:items (:saved-data db))))

(reg-sub
  :tooltip
  (fn [db]
    (get-in db [:tooltip :saved-data])))

(reg-sub
  :cache/organisms
  (fn [db]
    (get-in db [:cache :organisms])))

(reg-sub
  :toasts
  (fn [db]
    (get-in db [:toasts])))

(reg-sub
  :current-mine
  (fn [db]
    (get-in db [:mines (get db :current-mine)])))

(reg-sub
  :version
  (fn [db [_ mine-keyword]]
    (get-in db  [:assets :intermine-version mine-keyword])))

(reg-sub
  :current-lists
  :<- [:lists]
  :<- [:current-mine-name]
  (fn [[all-lists current-mine-name]]
    (get all-lists current-mine-name)))


(reg-sub
  :current-possible-values
  :<- [:current-mine]
  (fn [current-mine [_ path] ]
    (get-in current-mine [:possible-values path])))

(reg-sub
  :invalid-tokens?
  (fn [db]
    (get db :invalid-tokens?)))
