(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]))

(defmulti tree (fn [item] (:type item)))

(defmethod tree :folder [{:keys [type label children open trail]}]
  [:tr
   [:td
    [:span {:style {:padding-left (str (* (dec (count trail)) 40) "px")}
            :on-click (fn [evt]
                        (ocall evt :stopPropagation)
                        (dispatch [:bluegenes.events.mymine/toggle-folder-open trail])
                        )}
     (if open
       [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
       [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
     label]]
   [:td [:span "Today"]]])

(defmethod tree :list [{:keys [type label trail]}]
  [:tr
   [:td
    [:span {:style {:padding-left (str (* (dec (count trail)) 40) "px")}}
     [:svg.icon.icon-list [:use {:xlinkHref "#icon-list"}]]
     label]]
   [:td [:span "Today"]]])

(defn main []
  (let [drive (subscribe [:bluegenes.subs.mymine/tree])]
    (fn []
      [:div.container-fluid
      [:div.mymine
       [:table.table.table-striped
        [:thead
         [:tr
          [:th "Name"]
          [:th "Last Modified"]
          ]]
        (into [:tbody] (map (fn [x] [tree x]) @drive))]]])))
