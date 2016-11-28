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
            [redgenes.components.icons :as icons]
            [oops.core :refer [oget ocall]]
            [redgenes.sections.lists.views.operations :as operations]))


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
    (fn [sort-icon]
      [:div
       {:style {:display "inline-block"}}
       [:input.form-control.input-lg.square
        {:type           "text"
         :value          @text-filter
         :style          {:display       "inline"
                          :width         "300px"
                          :padding       "5px"
                          :margin        0
                          :background    "white"
                          :border-radius "5px"
                          :border        "1px solid #d6d6d6"}
         :placeholder    "Name or description contains..."
         :data-content   [:span "Fil"]
         :data-placement "bottom"
         :data-trigger   "hover"
         :on-change      set-text-filter}]
       [:span.flags
        {:style {:font-size "1.5em"}}
        ; Public / Private
        sort-icon
        [popover
         [:i.fa
          {:data-content   [:span "Show only your lists."]
           :data-placement "bottom"
           :data-trigger   "hover"
           :class          (case (get @flag-filters :authorized)
                             true "fa-user" false "fa-globe" nil "fa-user disabled")
           :on-click       (fn [] (dispatch [:lists/toggle-flag-filter :authorized]))}]]
        ; Favourites
        [popover
         [:i.fa
          {:data-content   [:span "Show only your favourite lists."]
           :data-placement "bottom"
           :data-trigger   "hover"
           :class          (case (get @flag-filters :favourite)
                             true "fa-star" false "fa-star-o" nil "fa-star disabled")
           :on-click       (fn [] (dispatch [:lists/toggle-flag-filter :favourite]))}]]
        ]])))

(defn in [needle haystack] (some? (some #{needle} haystack)))

(defn list-row []
  (let [selected (subscribe [:lists/selected])]
    (fn [{:keys [source description tags authorized name type size title timestamp dateCreated] :as l}]
      (let [date-created (tf/parse dateCreated)]
        [:div.grid-middle.list-row
         {:class (if (in name @selected) "selected")}
         [:div.col
          [:input {:type      "checkbox"
                   :checked   (if (in name @selected) true)
                   :on-change (fn [e] (dispatch ^:flush-dom [:lists/select name (oget e :target :checked)]))}]]
         [:div.col-8
          [:h4
           [:span.flags
            (if authorized [:i.fa.fa-user] [:i.fa.fa-globe])
            (if (one-of? tags "im:favourite") [:i.fa.fa-star] [:i.fa.fa-star-o])]
           [:a.stress {:on-click (fn [] (dispatch [:lists/view-results l]))} title]
           [:span.row-count (str " (" size " rows)")]]
          [:div.description description]]
         [:div.col.type-style {:class (str "type-" type)} [:span type]]
         [:div.col.date-created [:span (if dateCreated (if (t/after? date-created (t/today-at-midnight))
                                                         "Today"
                                                         [:div
                                                          (tf/unparse (tf/formatter "MMM, dd") date-created) " "
                                                          (tf/unparse (tf/formatter "YYYY") date-created)]
                                                         ))]]]))))



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
       [:h3
        (let [message (cond-> []
                              (some? (:authorized @flag-filters)) (conj (if (:authorized @flag-filters) "private" "public"))
                              (some? (:favourite @flag-filters)) (conj (if (:favourite @flag-filters) "favourite" "non-favourite")))]
          [:span
           [:span (str "You have no " (clojure.string/join ", " message) " lists")]
           (if @text-filter
             [:span " containing the phrase "
              [:span.no-results @text-filter]])]
          )]
       [:a
        {:on-click (fn [] (dispatch [:lists/clear-filters nil]))}
        "Click here to clear your search"]])))

(defn list-table []
  (let [sort-order (subscribe [:lists/sort-order])
        selected   (subscribe [:lists/selected])]
    (fn [lists]
      [:div.list-container
       [:div.grid
        [:div.col
         [:button.btn
          {:on-click (fn [] (dispatch [:lists/toggle-select-all (map :name lists)]))}
          (if (empty? @selected)
            "Select All"
            "Deselect All")]]
        [:div.col-8
         [:span
          [:h3.list-title "Title"]
          [controls [sort-icon :title "fa-sort-alpha" (:title @sort-order)]]]]
        [:div.col [:h3 "Type "]]
        ;[:div.col [:h4 "Count"]]
        [:div.col [:h3 "Created"]]]

       [css-transition-group
        {:transition-name          "fade"
         :transition-enter-timeout 100
         :transition-leave-timeout 100}
        (if (empty? lists)
          [no-results]
          (map (fn [l] ^{:key (:name l)} [list-row l]) lists))]])))

(defn main []
  (let [filtered-lists (subscribe [:lists/filtered-lists])]
    (fn []
      [:div.list-section
       {:style {:width "100%"}}
       ; TODO Wait for /lists web service bug fixes [operations/operations-bar]
       [list-table @filtered-lists]])))
