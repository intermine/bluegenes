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
      [:div.list-filter-controls
       [:div.flags
        ; Public / Private
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
        ]
        [:h3 "Title"]
        [:input.form-control.input-lg.square.text-filter
          {:type           "text"
          :value          @text-filter
          :placeholder    "Name or description contains..."
          :data-content   [:span "Fil"]
          :data-placement "bottom"
          :data-trigger   "hover"
          :on-change      set-text-filter}]
          sort-icon
       ])))

(defn in [needle haystack] (some? (some #{needle} haystack)))

(defn list-row []
  (let [selected (subscribe [:lists/selected])]
    (fn [{:keys [source description tags authorized name type size title timestamp dateCreated] :as l}]
      (let [date-created (tf/parse dateCreated)
            selected? (in name @selected)]
        [:tr {:class (cond selected? "selected")
              :on-click (fn [e]
                          (dispatch ^:flush-dom [:lists/select name (not selected?)]))}
          [:td
            [:input {:type      "checkbox"
                     :checked   selected?  }]]
          [:td.list-description
            [:h4 [:span.flags
              (if authorized [:i.fa.fa-user] [:i.fa.fa-globe])
              (if (one-of? tags "im:favourite") [:i.fa.fa-star] [:i.fa.fa-star-o])]
              [:a.stress {:on-click (fn [] (dispatch [:lists/view-results l]))} title]
              [:span.row-count (str " (" size " rows)")]]
            [:div.description description]]
          [:td.type-style {:class (str "type-" type)} type]
          [:td.date-created
           (if dateCreated (if (t/after? date-created (t/today-at-midnight))
             "Today"
             [:span
              (tf/unparse (tf/formatter "MMM, dd") date-created) " "
              (tf/unparse (tf/formatter "YYYY") date-created)]
             ))]]))))



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
      [:tr [:td {:col-span 4}
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
        "Click here to clear your search"]]])))

(defn select-all-checkbox [filtered-lists]
  (let [lists (subscribe [:lists/filtered-lists])
        ;selected (subscribe [:lists/selected])
        ]
;    (.log js/console "%c@lists" "color:hotpink;font-weight:bold;" (clj->js @lists))
;    (.log js/console "%c@selected" "color:hotpink;font-weight:bold;" (clj->js @selected) (clj->js (some? @selected)))
    [:input
     {:type "checkbox"
      :on-change (fn [e]
        (dispatch [:lists/toggle-select-all (map :name filtered-lists)]))
      ;  :checked (if (some? @selected)
      ;             (= (count @lists) (count @selected))
      ;             false)
    }]
))


(defn list-table []
  (let [sort-order (subscribe [:lists/sort-order])
        selected   (subscribe [:lists/selected])]
    (fn [lists]
      [:table.list-container
       [:thead
        [:tr
          [:th [select-all-checkbox lists]]
          [:th.list-description
            [controls [sort-icon :title "fa-sort-alpha" (:title @sort-order)]]
           ]
          [:th.type-style [:h3 "Type "]]
          [:th.date-created [:h3 "Created"]]]]

       [css-transition-group
        {:transition-name          "fade"
         :component "tbody"
         :class "list-group-body"
         :transition-enter-timeout 100
         :transition-leave-timeout 100}
        (if (empty? lists)
          [no-results]
          (map (fn [l] ^{:key (:name l)} [list-row l]) lists))]])))



(defn main []
  (let [filtered-lists (subscribe [:lists/filtered-lists])]
    (fn []
      [:div.list-section
       [operations/operations-bar]
       [list-table @filtered-lists]])))
