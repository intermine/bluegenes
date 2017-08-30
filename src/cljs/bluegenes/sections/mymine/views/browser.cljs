(ns bluegenes.sections.mymine.views.browser
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]))

(defmulti node (comp :file-type second))

(defn leaf []
  (let [hovering? (r/atom false)]
    (fn [x]
      (println "Xc" x)
      [:li [node x]])))

(defn pad [depth] (str (* depth 20) "px"))

(defn map-children [children parent-depth]
  (when (not-empty children)
    (into [:ul] (map (fn [l] [leaf (update-in l [1] assoc :depth (inc parent-depth)) l]) children))))

(defmethod node :folder [a]
  (let [open? (r/atom false)]
    (fn [[key {:keys [label file-type children depth] :as x}]]
      [:div
       [:div.hoverable
        [:div
         {:style    {:margin-left (pad depth)}
          :on-click (fn [] (swap! open? not))}
         [:svg.icon.icon-caret-right [:use {:xlinkHref "#icon-caret-right"}]]
         [:span label]]]
       (when @open?
         (map-children children depth))])))

(defmethod node :list [a]
  (let [selected-items (subscribe [::subs/op-selected-items])]
    (fn [[key {:keys [label file-type children depth id] :as x}]]
      (let [selected? (some? (some #{id} @selected-items))]
        [:div
         [:div.hoverable
          {:class (when selected? "highlighted")}
          [:div
           {:style    {:margin-left (pad depth)}
            :on-click (fn [] (dispatch [::evts/op-select-item id]))}
           [:svg.icon.icon-document-list [:use {:xlinkHref "#icon-document-list"}]]
           [:span label]]]
         (map-children children depth)]))))

(defmethod node :default [a]
  (fn [[key {:keys [label file-type children depth] :as x}]]
    [:div
     [:div.hoverable
      [:div
       {:style {:margin-left (pad depth)}}
       [:svg.icon]
       [:span label]]]
     (map-children children depth)]))

(defn main []
  (let [wp       (subscribe [::subs/with-public])
        selected (subscribe [::subs/op-selected-items])
        my-tree (subscribe [::subs/my-tree])]
    (fn []
      [:div.row
       [:pre (str @selected)]
       [:div.col-sm-6
        (into
          [:ul.mymine-browser]
          (map (fn [x] [leaf x]) @my-tree))]])))


