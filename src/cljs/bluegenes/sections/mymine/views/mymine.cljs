(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [goog.fx.DragDrop :as dd]))

(defn stop-prop [evt] (ocall evt :stopPropagation))

(defn drag-events [trail]
  {:draggable true
   :on-drag-start (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drag-start trail]))
   :on-drag-end (fn [evt]
                  (dispatch [:bluegenes.events.mymine/drag-end trail]))
   :on-drag-over (fn [evt]
                   (ocall evt :preventDefault)
                   (dispatch [:bluegenes.events.mymine/drag-over trail]))
   :on-drag-leave (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drag-over nil]))
   :on-drop (fn [evt]
              (dispatch [:bluegenes.events.mymine/drop trail]))})

(defmulti tree (fn [item] (:type item)))

(defmethod tree :folder [{:keys [type label children open trail size read-only?]}]
  [:tr
   (drag-events trail)
   [:td
    [:span {:style {:padding-left (str (* (dec (dec (count trail))) 20) "px")}
            :on-click (fn [evt]
                        (stop-prop evt)
                        (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
     (if open
       [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
       [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
     [:span.title.bold label]
     (when read-only? [:span.label [:i.fa.fa-lock] " Read Only"])]]
   [:td [:span "Today"]]])

(defmethod tree :list [{:keys [type label trail]}]
  [:tr
   (drag-events trail)
   [:td
    [:span.title {:style {:padding-left (str (* (dec (dec (count trail))) 20) "px")}}
     [:svg.icon.icon-list [:use {:xlinkHref "#icon-list"}]]
     [:a.title label]]]
   [:td [:span "Today"]]])

(defn table-header []
  (fn [{:keys [label key sort-by]}]
    [:th
     {:on-click (fn [] (dispatch [:bluegenes.events.mymine/toggle-sort key]))}
     label
     (when (= key (:key sort-by))
       (if (:asc? sort-by)
         [:i.fa.fa-fw.fa-chevron-down]
         [:i.fa.fa-fw.fa-chevron-up]))]))

(defn main []
  (let [as-list (subscribe [:bluegenes.subs.mymine/as-list])
        sort-by (subscribe [:bluegenes.subs.mymine/sort-by])]
    (fn []
      [:div.container-fluid
       [:div.mymine.noselect
        [:table.table
         [:thead
          [:tr
           [table-header {:label "Name" :key :label :sort-by @sort-by}]
           [:th "Last Modified"]]]
         (into [:tbody] (map (fn [x] [tree x]) @as-list))]]])))
