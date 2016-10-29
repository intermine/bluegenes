(ns redgenes.sections.lists.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.lists.events]
            [redgenes.sections.lists.subs]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [oops.core :refer [oget]]))


(def time-formatter (tf/formatter "HH:mm"))
(def date-formatter (tf/formatter "dd/MM/YYYY"))
(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))
(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(defn set-text-filter [e]
  (dispatch [:lists/set-text-filter (.. e -target -value)]))


(defn controls []
  (let [flag-filters (subscribe [:lists/flag-filters])
        text-filter  (subscribe [:lists/text-filter])]
    (fn []
      [:div
       [:span.flags
        {:style {:font-size "1.5em"}}
        ; Public / Private
        [:i.fa
         {:class    (case (get @flag-filters :authorized)
                      true "fa-user" false "fa-globe" nil "fa-user disabled")
          :on-click (fn [] (dispatch [:lists/toggle-flag-filter :authorized]))}]
        ; Favourites
        [:i.fa
         {:class    (case (get @flag-filters :favourite)
                      true "fa-star" false "fa-star-o" nil "fa-star disabled")
          :on-click (fn [] (dispatch [:lists/toggle-flag-filter :favourite]))}]]
       [:span [:input.form-control.input-lg.square
               {:type        "text"
                :value       @text-filter
                :style       {:display "inline"
                              :width   "200px"}
                :placeholder "Filter with text..."
                :on-change   set-text-filter}]]])))

(defn list-row []
  (fn [{:keys [description tags authorized name type size title timestamp dateCreated] :as l}]
    (let [date-created (tf/parse dateCreated)]
      [:div.grid-middle.list-row
       [:div.col-8 [:div
                    [:h4
                     [:span.flags
                      (if authorized [:i.fa.fa-user] [:i.fa.fa-globe])
                      (if (one-of? tags "im:favourite") [:i.fa.fa-star] [:i.fa.fa-star-o])]
                     [:span.stress
                      {:on-click (fn [] (dispatch [:lists/view-results l]))}
                      title]]
                    [:span description]]]
       [:div.col [:h4 type]]
       [:div.col [:h4 size]]
       [:div.col [:span (if dateCreated (if (t/after? date-created (t/today-at-midnight))
                                          "Today"
                                          [:div
                                           [:div (tf/unparse (tf/formatter "MMM, dd") date-created)]
                                           [:span (tf/unparse (tf/formatter "YYYY") date-created)]]))]]])))

; TODO Make workey
(defn sort-icon []
  (fn [kw class-chunk value]
    [:i.fa
     {:class    (if value
                  (str class-chunk "-" (name value) " stress ")
                  (str "fa-sort disabled"))
      :on-click (fn [] (dispatch [:lists/toggle-sort kw]))}]))

(defn no-results []
  (let [text-filter  (subscribe [:lists/text-filter])
        flag-filters (subscribe [:lists/flag-filters])]
    (fn []
      [:div
       [:h1
        (let [message (cond-> []
                              (some? (:authorized @flag-filters)) (conj (if (:authorized @flag-filters) "private" "public"))
                              (some? (:favourite @flag-filters)) (conj (if (:favourite @flag-filters) "favourite" "non-favourite")))]
          [:span
           [:span (str "You have no " (clojure.string/join ", " message) " lists")]
           (if @text-filter
             [:span " containing the phrase " [:span.stress
                                               {:style {:font-style "italic"}} @text-filter]])]
          )]
       [:a
        {:on-click (fn [] (dispatch [:lists/clear-filters nil]))}
        "Click here to clear your search"]])))

(defn list-table []
  (let [sort-order (subscribe [:lists/sort-order])]
    (fn [lists]
      [:div.list-container
       [:div.grid
        [:div.col-8
         [:h4 "Title "
          [sort-icon :title "fa-sort-alpha" (:title @sort-order)]
          [controls]]]
        [:div.col [:h4 "Type "]]
        [:div.col [:h4 "Count"]]
        [:div.col [:h4 "Created"]]]
       [css-transition-group
        {:transition-name          "fade"
         :transition-enter-timeout 100
         :transition-leave-timeout 100}
        (if (empty? lists)
          [no-results]
          (map (fn [l]
                 ^{:key (:name l)} [list-row l]) lists))]])))

(defn main []
  (let [filtered-lists (subscribe [:lists/filtered-lists])]
    (fn []
      [:div.list-section
       {:style {:width "100%"}}
       [list-table @filtered-lists]])))