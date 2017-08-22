(ns bluegenes.sections.mymine.views.browser
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [bluegenes.subs.mymine :as subs]))

(defmulti node (comp :file-type second))

(defn leaf []
  (let [hovering? (r/atom false)]
    (fn [x]
      [:li [node x]])))

(defn pad [depth] (str (* depth 20) "px"))

(defn map-children [children parent-depth]
  (when (not-empty children)
    (into [:ul] (map (fn [l] [leaf (update-in l [1] assoc :depth (inc parent-depth)) l]) children))))

(defmethod node :folder [a]
  (fn [[key {:keys [label file-type children depth] :as x}]]
    [:div
     [:div.hoverable
      [:div
       {:style {:margin-left (pad depth)}}
       [:svg.icon.icon-caret-right [:use {:xlinkHref "#icon-caret-right"}]]
       [:span label]]]
     (map-children children depth)]))

(defmethod node :list [a]
  (fn [[key {:keys [label file-type children depth] :as x}]]
    [:div
     [:div.hoverable
      [:div
       {:style {:margin-left (pad depth)}}
       [:svg.icon.icon-document-list [:use {:xlinkHref "#icon-document-list"}]]
       [:span label]]]
     (map-children children depth)]))

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
  (let [wp (subscribe [::subs/with-public])]
    (fn [] [:ul.mymine-browser [leaf [:root (:root @wp)]]])))




;
;(defmulti node (comp :file-type second))
;
;(defn leaf []
;  (let [hovering? (r/atom false)]
;    (fn [x]
;      [:li [node x]])))
;
;(defn pad [depth] (str (* depth 20) "px"))
;
;(defn map-children [children parent-depth]
;  (when (not-empty children)
;    (into [:ul] (map (fn [l] [leaf (update-in l [1] assoc :depth (inc parent-depth)) l]) children))))
;
;(defmethod node :folder [a]
;  (fn [[key {:keys [label file-type children depth] :as x}]]
;    [:div
;     [:div.hoverable
;      [:div
;       {:style {:margin-left (pad depth)}}
;       [:svg.icon.icon-caret-right [:use {:xlinkHref "#icon-caret-right"}]]
;       [:span label]]]
;     (map-children children depth)]))
;
;(defmethod node :list [a]
;  (fn [[key {:keys [label file-type children depth] :as x}]]
;    [:div
;     [:div.hoverable
;      [:div
;       {:style {:margin-left (pad depth)}}
;       [:svg.icon.icon-document-list [:use {:xlinkHref "#icon-document-list"}]]
;       [:span label]]]
;     (map-children children depth)]))
;
;(defmethod node :default [a]
;  (fn [[key {:keys [label file-type children depth] :as x}]]
;    [:div
;     [:div.hoverable
;      [:div
;       {:style {:margin-left (pad depth)}}
;       [:svg.icon]
;       [:span label]]]
;     (map-children children depth)]))



