(ns bluegenes.pages.reportpage.components.toc
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.pages.reportpage.subs :as subs]))

;; TODO subscribe to categories data, probably from a safer place in app-db

(defn main []
  (let [categories @(subscribe [:bluegenes.pages.admin.subs/categories])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])
        title @(subscribe [::subs/report-title])]
    [:div.toc-container
     [:h4.toc-title title
      [:code.start {:class (str "start-" rootClass)} rootClass]]
     (into [:ul.toc
            [:li.active "Summary"]]
           (for [{:keys [category children]} categories]
             [:<>
              [:li category]
              (when (seq children)
                (into [:ul]
                      (for [{:keys [label]} children]
                        [:li label])))]))]))
