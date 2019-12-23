(ns bluegenes.pages.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [goog.i18n.NumberFormat.Format]
            [bluegenes.pages.mymine.events :as evts]
            [bluegenes.pages.mymine.subs :as subs]
            [bluegenes.pages.mymine.views.modals :as modals]
            [bluegenes.pages.mymine.views.contextmenu :as m]
            [bluegenes.route :as route])
  (:import
   (goog.i18n NumberFormat)
   (goog.i18n.NumberFormat Format)))

(defn hide-context-menu [evt]
  (ocall (js/$ "#contextMenu") :hide))
;(dispatch [::evts/set-context-menu-target])

(defn attach-hide-context-menu []
  (ocall (js/$ "body") :on "click" hide-context-menu))

(defn tag-drag-events [entry]
  {:draggable true
   :on-drag-enter (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging-over entry]))
   :on-drag-over (fn [evt] (ocall evt :preventDefault) (ocall evt :stopPropagation))
   ;:on-drag-leave (fn [evt] (dispatch [::evts/dragging-over nil]) )
   :on-drag-start (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging entry]))
   :on-drag-end (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging? false]))
   :on-drop (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dropping-on entry]))})

(defn trigger-context-menu [data]
  {:on-context-menu (fn [evt]
                      (when-not (oget evt :nativeEvent :ctrlKey)
                        (do
                          ; Prevent the browser from showing its context menu
                          (ocall evt :preventDefault)
                          ; Stop evt propogation
                          (ocall evt :stopPropagation)
                          ; Set this item as the target of the context menu
                          (dispatch [::evts/set-context-menu-target data])
                          (dispatch [::evts/set-action-target data])
                          ; Show the context menu
                          (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                      :left (oget evt :pageX)
                                                                      :top (oget evt :pageY)})))))})

(defn draggable [{:keys [id file-type trail whoami] :as file-details}]
  {:draggable true
   :on-drag-start (fn [evt]
                    (dispatch [::evts/drag-start file-details]))})

(defn list-operations []
  (let [checked (subscribe [::subs/checked-ids])]
    (fn []
      (let [cant-perform?        (not (some? (not-empty @checked)))
            cant-operate?        (not (> (count @checked) 1))
            operation-properties (if (> (count @checked) 1)
                                   {:data-toggle "modal"
                                    :data-keyboard true
                                    :data-target "#myTestModal"}
                                   {})]

        [:nav.nav-list-operations
         [:ul
          [:li {:class (when cant-perform? "disabled")}
           [:a {:on-click (fn [] (when (not cant-perform?) (dispatch [::evts/copy-n])))}
            "Duplicate " [:svg.icon.icon-duplicate [:use {:xlinkHref "#icon-duplicate"}]]]]
          [:li {:class (when cant-perform? "disabled")}
           [:a {:on-click (fn [] (when (not cant-perform?)
                                   (dispatch [::evts/delete-lists])))}
            "Delete " [:svg.icon.icon-bin [:use {:xlinkHref "#icon-bin"}]]]]

          [:li {:class (when (empty? operation-properties) "disabled")}
           [:a (merge
                operation-properties
                {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :combine])))})
            "Combine " [:svg.icon.icon-venn-combine.venn
                        [:use {:xlinkHref "#icon-venn-combine"}]]]]

          [:li {:class (when (empty? operation-properties) "disabled")}
           [:a (merge
                operation-properties
                {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :intersect])))})
            "Intersect " [:svg.icon.icon-venn-intersection.venn [:use {:xlinkHref "#icon-venn-intersection"}]]]]
          [:li {:class (when (empty? operation-properties) "disabled")}
           [:a (merge
                operation-properties
                {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :difference])))})
            "Difference " [:svg.icon.icon-venn-disjunction.venn
                           [:use {:xlinkHref "#icon-venn-disjunction"}]]]]
          [:li {:class (when (empty? operation-properties) "disabled")}
           [:a (merge
                operation-properties
                {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :subtract])))})
            "Subtract " [:svg.icon.icon-venn-difference.venn [:use {:xlinkHref "#icon-venn-difference"}]]]]

          #_[:li {}
             [:a {:on-click (fn [] (dispatch [::evts/fetch-tree]))}
              [:span "Fetch " [:svg.icon.icon-venn-difference [:use {:xlinkHref "#icon-venn-difference"}]]]]]]]))))

(defn checked-card []
  (fn [{:keys [name id type] :as details}]
    ;(dispatch [::evts/toggle-checked id])
    [:div.mymine-card name [:span.pull-right [:span {:on-click (fn [] (dispatch [::evts/toggle-checked id]))} [:svg.icon.icon-close [:use {:xlinkHref "#icon-close"}]]]]]))

(defn checked-panel []
  (let [details (subscribe [::subs/checked-details])]
    (fn []
      [:div.details.open
       (if-not (empty? @details)
         (into [:div [:h3 "Selected Lists"]] (map (fn [i] [checked-card i]) @details))
         [:div [:h3 "Select one or more lists to perform an operation"]])])))

(defn checkbox [id]
  (let [checked-ids (subscribe [::subs/checked-ids])]
    (fn [id]
      (let [checked? (some? (some #{id} @checked-ids))]
        [:input
         {:type "checkbox"
          :checked checked?
          :on-click (fn [e]
                      (ocall e :stopPropagation)
                      (dispatch [::evts/toggle-checked id]))}]))))

(defn ico []
  (fn [file-type]
    (case file-type
      "list" [:svg.icon.icon-document-list
              [:use {:xlinkHref "#icon-document-list"}]]
      "tag" [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]
      [:svg.icon.icon-folder [:use {:xlinkHref "#icon-spyglass"}]])))

(defn row-list [{:keys [im-obj-id trail im-obj-type entry-id] :as file}]
  (let [details             (subscribe [::subs/one-list im-obj-id])
        context-menu-target (subscribe [::subs/context-menu-target])]
    (fn [{:keys []}]
      (let [{:keys [description authorized name type size timestamp] :as dets} @details]
        [:div.grid.grid-middle
         (merge {:class (when (= @context-menu-target file) "highlighted")}
                (tag-drag-events file)
                (trigger-context-menu (assoc dets :im-obj-type "list"))
                {:on-click (fn []
                             (dispatch [::evts/set-context-menu-target file]))})
         [:div.col-7 (merge {} (draggable file))
          [:div [checkbox im-obj-id] [ico im-obj-type]
           [:a {:href (route/href ::route/list {:title (:title dets)})}
            name]
           (when-not authorized
             [:svg.icon.icon-globe {:style {:fill "#939393"}} [:use {:xlinkHref "#icon-globe"}]])]
          [:div.description-text
           description]]
         [:div.col-1 {:class (str "tag-type type-" type)} type]
         [:div.col-1.list-size size]]))))

(defn row [{:keys [im-obj-type] :as item}]
  (fn []
    (case im-obj-type
      "list" [row-list item]
      [:div])))

(defn main []
  (let [context-menu-target (subscribe [::subs/context-menu-target])
        modal-kw            (subscribe [::subs/modal])
        items               (subscribe [::subs/untagged-items])]
    (r/create-class
     {:component-did-mount attach-hide-context-menu
      :reagent-render
      (fn []
        [:div.mymine.noselect
         [:div.files
          [list-operations]
          [:div.bottom
           (into [:div]
                 (map-indexed (fn [idx x]
                                ^{:key
                                  (str (or (:entry-id x)
                                           (str (:im-obj-type x)
                                                (:im-obj-id x))))}
                                [row (assoc x :index idx)])
                              @items))]]
          ;[checked-panel]]

         [modals/modal-list-operations @modal-kw]

         [modals/modal @context-menu-target]
         [modals/modal-copy @context-menu-target]
         [modals/modal-delete-item @context-menu-target]
         [modals/modal-lo @context-menu-target]
         [modals/modal-lo-intersect @context-menu-target]
         [modals/modal-rename-list @context-menu-target]
         [m/context-menu-container @context-menu-target]])})))
