(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [goog.i18n.NumberFormat.Format])
  (:import
    (goog.i18n NumberFormat)
    (goog.i18n.NumberFormat Format)))

(defn stop-prop [evt] (ocall evt :stopPropagation))

(def nff (NumberFormat. Format/DECIMAL))
(defn- nf [num] (.format nff (str num)))

(defn show-context-menu [evt]
  (ocall evt :preventDefault)
  (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                              :left    (oget evt :pageX)
                                              :top     (oget evt :pageY)})))

(defn hide-context-menu [evt]
  (ocall (js/$ "#contextMenu") :hide))

(defn attach-hide-context-menu [] (ocall (js/$ "body") :on "click" hide-context-menu))

(defn drag-events [trail index]
  {:draggable     true
   :on-drag-start (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drag-start trail]))
   :on-drag-end   (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drag-end trail]))
   :on-drag-over  (fn [evt]
                    (ocall evt :preventDefault)
                    (dispatch [:bluegenes.events.mymine/drag-over trail]))
   :on-drag-leave (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drag-over nil]))
   :on-drop       (fn [evt]
                    (dispatch [:bluegenes.events.mymine/drop trail]))})

(defn click-events [trail index]
  {:on-click        (fn [evt]
                      (let [ctrl-key? (oget evt :nativeEvent :ctrlKey)]
                        (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:single? true}])
                        ; TODO re-enable multi selected
                        #_(if ctrl-key?
                            (dispatch [:bluegenes.events.mymine/toggle-selected trail index])
                            (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:reset? true}]))))
   :on-context-menu (fn [evt]
                      (ocall evt :preventDefault)
                      ; Rather than toggle the selected row, force it to be selected
                      (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:force? true}])
                      (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                  :left    (oget evt :pageX)
                                                                  :top     (oget evt :pageY)})))})

(defmulti tree (fn [item] (:file-type item)))

(defmethod tree :folder []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type label children open trail size read-only?]}]
      [:tr
       (cond-> {}
               ; Item is selected
               true (merge (click-events trail index))
               ; Item is allowed to be moved?
               (not read-only?) (merge (drag-events trail index))
               ; Selected?
               (some? (some #{trail} @selected-items)) (assoc :class "selected"))
       [:td
        [:span {:style    {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}
                :on-click (fn [evt]
                            (stop-prop evt)
                            (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
         (if open
           [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
           [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
         [:span.title label]
         (when read-only? [:span.label [:i.fa.fa-lock] " Read Only"])]]
       [:td [:span]]
       [:td [:span]]])))

(defmethod tree :list []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type label trail read-only? size type] :as x}]
      (let [selected? (some? (some #{trail} @selected-items))]
        [:tr
         (cond-> {}
                 ; Item is selected
                 true (merge (click-events trail index))
                 ; Item is allowed to be moved?
                 (not read-only?) (merge (drag-events trail index))
                 ; Selected?
                 selected? (assoc :class (str "im-type inverted " type)))

         [:td
          [:span.title {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}}
           [:svg.icon.icon-document-list.im-type
            {:class (str type (when selected? " inverted"))} [:use {:xlinkHref "#icon-document-list"}]]
           [:a.title label]]]
         [:td [:span.sub type]]
         [:td [:span.sub (nf size)]]]))))

(defn table-row-wrapper []
  (r/create-class
    {:component-did-mount (fn [this]
                            #_(ocall (js/$ (r/dom-node this)) :on "contextmenu"
                                     (fn [e]
                                       (js/console.log "E" e)
                                       (ocall (js/$ "#contextMenu") :css
                                              (clj->js
                                                {:display "block"
                                                 :left    (oget e :pageX)
                                                 :top     (oget e :pageY)}))
                                       false)))
     :reagent-render      (fn [row]
                            [tree row])}))

(defn table-header []
  (fn [{:keys [label key sort-by]}]
    [:th
     {:on-click (fn [] (dispatch [:bluegenes.events.mymine/toggle-sort key]))}
     label
     (when (= key (:key sort-by))
       (if (:asc? sort-by)
         [:i.fa.fa-fw.fa-chevron-down]
         [:i.fa.fa-fw.fa-chevron-up]))]))

(defn context-menu []
  (fn []
    [:div#contextMenu.dropdown.clearfix
     [:ul.dropdown-menu
      [:li [:a "one"]]
      [:li [:a "two"]]]]))

(defn main []
  (let [as-list  (subscribe [:bluegenes.subs.mymine/as-list])
        sort-by  (subscribe [:bluegenes.subs.mymine/sort-by])
        unfilled (subscribe [:bluegenes.subs.mymine/unfilled])]
    (r/create-class

      {:component-did-mount attach-hide-context-menu
       :reagent-render      (fn []
                              [:div.container-fluid
                               [:div.mymine.noselect
                                [:table.table.mymine-table
                                 [:thead
                                  [:tr
                                   [table-header {:label "Name" :key :label :sort-by @sort-by}]
                                   [table-header {:label "Type" :key :type :sort-by @sort-by}]
                                   [table-header {:label "Size" :key :size :sort-by @sort-by}]]]
                                 (into [:tbody] (map-indexed (fn [idx x]
                                                               ^{:key (str (:trail x))} [table-row-wrapper (assoc x :index idx)]) @as-list))]
                                [context-menu]]])})))
