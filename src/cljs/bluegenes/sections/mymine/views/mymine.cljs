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

(def context-menu-state (r/atom nil))

(defn hide-context-menu [evt] (ocall (js/$ "#contextMenu") :hide))

(defn attach-hide-context-menu [] (ocall (js/$ "body") :on "click" hide-context-menu))

(defn drag-events [trail index]
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

(defn click-events [trail index]
  {:on-mouse-down (fn [evt]
                    (stop-prop evt)
                    (let [ctrl-key? (oget evt :nativeEvent :ctrlKey)]
                      (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:force? true}])
                      ; TODO re-enable multi selected

                      #_(if ctrl-key?
                          (dispatch [:bluegenes.events.mymine/toggle-selected trail index])
                          (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:reset? true}]))))
   :on-context-menu (fn [evt]
                      (stop-prop evt)
                      (if (oget evt :nativeEvent :ctrlKey)
                        nil
                        (do
                          (ocall evt :preventDefault)
                          (dispatch [:bluegenes.events.mymine/set-context-menu-target trail])
                          ; Rather than toggle the selected row, force it to be selected
                          (dispatch [:bluegenes.events.mymine/toggle-selected trail index {:force? true}])
                          (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                      :left (oget evt :pageX)
                                                                      :top (oget evt :pageY)})))))})

(defmulti tree (juxt :file-type :editing?))






(defmethod tree [:folder nil] []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type editing? label children open trail size read-only?] :as item}]
      [:tr
       (cond-> {}
               ; Item is selected
               true (merge (click-events trail index))
               ; Item is allowed to be moved?
               (not read-only?) (merge (drag-events trail index))
               ; Selected?
               (some? (some #{trail} @selected-items)) (assoc :class "selected"))
       [:td
        [:div {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}
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

(defmethod tree [:folder true] []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type editing? label children open trail size read-only?] :as item}]
      [:tr
       (cond-> {}
               ; Item is selected
               true (merge (click-events trail index))
               ; Item is allowed to be moved?
               (not read-only?) (merge (drag-events trail index))
               ; Selected?
               (some? (some #{trail} @selected-items)) (assoc :class "selected"))
       [:td
        [:div {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}
               :on-click (fn [evt]
                           (stop-prop evt)
                           (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
         (if open
           [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
           [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
         #_[edit {:initial-value label
                  :on-submit (fn [value]
                               (dispatch [:bluegenes.events.mymine/update-value trail :label value]) (println "GOT" value))}]
         (when read-only? [:span.label [:i.fa.fa-lock] " Read Only"])]]
       [:td [:span]]
       [:td [:span]]])))

(defmethod tree [:list nil] []
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
                 ;  selected? (assoc :class (str "im-type inverted " type))
                 selected? (assoc :class (str "im-type box " type))
                 )

         [:td
          [:span.title {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}}
           [:svg.icon.icon-document-list.im-type
            {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
           [:a.title label]]]
         [:td [:span.sub type]]
         [:td [:span.sub (nf size)]]]))))


(defn folder-editing []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type editing? label children open trail size read-only?] :as item}]
      [:tr
       (cond-> {}
               ; Item is selected
               true (merge (click-events trail index))
               ; Item is allowed to be moved?
               (not read-only?) (merge (drag-events trail index))
               ; Selected?
               (some? (some #{trail} @selected-items)) (assoc :class "selected"))
       [:td
        [:div {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}
               :on-click (fn [evt]
                           (stop-prop evt)
                           (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
         (if open
           [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
           [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
         #_[edit {:initial-value label
                  :on-submit (fn [value]
                               (dispatch [:bluegenes.events.mymine/update-value trail :label value]) (println "GOT" value))}]
         (when read-only? [:span.label [:i.fa.fa-lock] " Read Only"])]]
       [:td [:span]]
       [:td [:span]]])))


(defn folder-row []
  (let [selected-items (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [index type editing? label children open trail size read-only?] :as item}]
      [:tr
       (cond-> {}
               ; Item is selected
               true (merge (click-events trail index))
               ; Item is allowed to be moved?
               (not read-only?) (merge (drag-events trail index))
               ; Selected?
               (some? (some #{trail} @selected-items)) (assoc :class "selected"))
       [:td
        [:div {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}
               :on-click (fn [evt]
                           (stop-prop evt)
                           (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
         (if open
           [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
           [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])
         #_[edit {:initial-value label
                  :on-submit (fn [value]
                               (dispatch [:bluegenes.events.mymine/update-value trail :label value]) (println "GOT" value))}]
         (when read-only? [:span.label [:i.fa.fa-lock] " Read Only"])]]
       [:td [:span]]
       [:td [:span]]])))

(defn folder-row-normal []
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
                 ;  selected? (assoc :class (str "im-type inverted " type))
                 selected? (assoc :class (str "im-type box " type))
                 )

         [:td
          [:span.title {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}}
           [:svg.icon.icon-document-list.im-type
            {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
           [:a.title label]]]
         [:td [:span.sub type]]
         [:td [:span.sub (nf size)]]]))))

(defn list-row []
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
                 ;  selected? (assoc :class (str "im-type inverted " type))
                 selected? (assoc :class (str "im-type box " type))
                 )

         [:td
          [:span.title {:style {:padding-left (str (* (dec (dec (dec (count trail)))) 20) "px")}}
           [:svg.icon.icon-document-list.im-type
            {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
           [:a.title label]]]
         [:td [:span.sub type]]
         [:td [:span.sub (nf size)]]]))))

(defn dispatch-edit [location key value]
  (dispatch [:bluegenes.events.mymine/update-value location key value]))

(defn edit-cell [{:keys [trail on-submit initial-value]}]
  (let [value (r/atom initial-value)]
    (fn []
      [:input.form-control.form-group-sm {:type "text"
                                          :value @value
                                          :ref (fn [v] (when v (ocall v :focus)))
                                          :on-change (fn [evt] (reset! value (oget evt :target :value)))
                                          :on-key-down (fn [evt]
                                                         (case (oget evt :keyCode)
                                                           27 (dispatch [:bluegenes.events.mymine/toggle-edit-mode trail])
                                                           13 (on-submit @value)
                                                           nil))}])))

(defn folder-cell []
  (fn [{:keys [file-type trail index label open editing?] :as item}]
    [:div.mymine-row
     {:on-click (fn [] (dispatch [:bluegenes.events.mymine/toggle-folder-open trail]))}
     [:span.shrink
      (if open
        [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
        [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])]
     [:span.grow
      (if editing?
        [edit-cell {:trail trail :initial-value label :on-submit (partial dispatch-edit trail :label)}]
        [:span label])]]))

(defn list-cell []
  (fn [{:keys [file-type trail index label open type] :as item}]
    [:div.mymine-row
     [:svg.icon.icon-document-list.im-type
      {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
     [:span.grow label]]))

(defn table-row []
  (let [selected (subscribe [:bluegenes.subs.mymine/selected])]
    (fn [{:keys [editing? level file-type type trail index read-only?] :as row}]

      (let [selected? (some? (some #{trail} @selected))]
        [:tr (-> {:class (clojure.string/join " " [(when selected? (str "im-type box " type))])}
                 (merge (click-events trail index))
                 (cond-> (not read-only?) (merge (drag-events trail index))))
         [:td {:style {:padding-left (str (* level 20) "px")}}
          (case file-type
            :folder [folder-cell row]
            :list [list-cell row])]
         [:td "Type"]
         [:td "Size"]]))))


(defn table-header []
  (fn [{:keys [label key sort-by]}]
    [:th
     {:on-click (fn [] (dispatch [:bluegenes.events.mymine/toggle-sort key]))}
     label
     (when (= key (:key sort-by))
       (if (:asc? sort-by)
         [:i.fa.fa-fw.fa-chevron-down]
         [:i.fa.fa-fw.fa-chevron-up]))]))



(defn modal [loc]
  (fn []
    [:div#relModal.modal.fade {:role "dialog"}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header [:h3 "Manage Relationships"]]
       [:div.modal-body
        [:h1 [:i.fa.fa-home]]]
       [:div.modal-footer
        [:div.btn-toolbar.pull-right
         [:button.btn.btn-default
          {:data-dismiss "modal"}
          "Cancel"]
         [:button.btn.btn-success
          {:data-dismiss "modal"
           :on-click (fn [] (dispatch [:rel-manager/apply-changes loc]))}
          "Apply Changes"]]]]]]))


(defmulti context-menu :file-type)

(defmethod context-menu :folder []
  (fn [{:keys [trail]}]
    [:ul.dropdown-menu
     [:li {:on-click
           (fn [] (dispatch [:bluegenes.events.mymine/toggle-edit-mode trail]))} [:a "Rename"]]
     [:li.divider]
     [:li [:a "New Folder..."]]]))

(defmethod context-menu :list []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "List"]]]))

(defmethod context-menu :default []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "Default"]]]))


(defn context-menu-container []
  (let [context-menu-target (subscribe [:bluegenes.subs.mymine/context-menu-target])]
    (fn []
      (let [{:keys [file-type label trail] :as target} @context-menu-target]
        [:div#contextMenu.dropdown.clearfix
         ^{:key (str "context-menu" trail)} [context-menu target]]))))

(defn main []
  (let [as-list  (subscribe [:bluegenes.subs.mymine/as-list])
        sort-by  (subscribe [:bluegenes.subs.mymine/sort-by])
        unfilled (subscribe [:bluegenes.subs.mymine/unfilled])]
    (r/create-class
      {:component-did-mount attach-hide-context-menu
       :reagent-render (fn []
                         [:div.container-fluid
                          [:div.mymine.noselect
                           [:table.table.mymine-table
                            [:thead
                             [:tr
                              [table-header {:label "Name" :key :label :sort-by @sort-by}]
                              [table-header {:label "Type" :key :type :sort-by @sort-by}]
                              [table-header {:label "Size" :key :size :sort-by @sort-by}]]]
                            (into [:tbody] (map-indexed (fn [idx x]
                                                          ^{:key (str (:trail x))} [table-row (assoc x :index idx)]) @as-list))]
                           [modal]
                           [context-menu-container]]])})))
