(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [oops.core :refer [oget ocall oset!]]
            [goog.i18n.NumberFormat.Format]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            )
  (:import
    (goog.i18n NumberFormat)
    (goog.i18n.NumberFormat Format)))

(defn stop-prop [evt] (ocall evt :stopPropagation))

(def nff (NumberFormat. Format/DECIMAL))
(defn- nf [num] (.format nff (str num)))

(defn hide-context-menu [evt] (ocall (js/$ "#contextMenu") :hide))

(defn attach-hide-context-menu [] (ocall (js/$ "body") :on "click" hide-context-menu))

(defn drag-events [trail index]
  {:draggable true
   :on-drag-start (fn [evt]
                    (dispatch [::evts/drag-start trail]))
   :on-drag-end (fn [evt]
                  (dispatch [::evts/drag-end trail]))
   :on-drag-over (fn [evt]
                   (ocall evt :preventDefault)
                   (dispatch [::evts/drag-over trail]))
   :on-drag-leave (fn [evt]
                    (dispatch [::evts/drag-over nil]))
   :on-drop (fn [evt]
              (dispatch [::evts/drop trail]))})

(defn click-events [trail index]
  {:on-mouse-down (fn [evt]
                    (stop-prop evt)
                    (let [ctrl-key? (oget evt :nativeEvent :ctrlKey)]
                      (dispatch [::evts/toggle-selected trail {:force? true}])
                      ; TODO re-enable multi selected

                      #_(if ctrl-key?
                          (dispatch [::evts/toggle-selected trail index])
                          (dispatch [::evts/toggle-selected trail index {:reset? true}]))))
   :on-context-menu (fn [evt]

                      (if (oget evt :nativeEvent :ctrlKey)
                        nil
                        (do
                          ; Prevent the browser from showing its context menu
                          (ocall evt :preventDefault)
                          ; Force this item to be selected
                          (dispatch [::evts/toggle-selected trail index {:force? true}])
                          ; Set this item as the target of the context menu
                          (dispatch [::evts/set-context-menu-target trail])
                          ; Show the context menu
                          (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                      :left (oget evt :pageX)
                                                                      :top (oget evt :pageY)})))))})


(defn dispatch-edit [location key value]
  (dispatch [::evts/update-value location key value]))

(defmulti context-menu :file-type)

(defmethod context-menu :folder []
  (fn [{:keys [trail type]}]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameModal"}
      [:a "Rename"]]
     [:li.divider]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineNewFolderModal"}
      [:a "New Folder..."]]]))

(defmethod context-menu :list []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "List"]]]))

(defmethod context-menu :default []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "Default"]]]))

(defn context-menu-container []
  (fn [{:keys [file-type label trail] :as target}]
    [:div#contextMenu.dropdown.clearfix ^{:key (str "context-menu" trail)} [context-menu target]]))

(defn modal-new-folder []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)]
    (r/create-class
      {:component-did-mount (fn [this]
                              ; When modal is closed, clear the context menu target. This prevents the modal
                              ; from retamaining prior state that was cancelled / dismissed
                              (ocall (js/$ (r/dom-node this))
                                     :on "hidden.bs.modal"
                                     (fn [] (dispatch [::evts/set-context-menu-target nil])))
                              (ocall (js/$ (r/dom-node this))
                                     :on "shown.bs.modal"
                                     (fn [] (ocall @input-dom-node :select))))
       :reagent-render (fn [{:keys [file-type label trail]}]
                         [:div#myMineNewFolderModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "New Folder"]]
                            [:div.modal-body [:p "Please enter a new name for the folder"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value label) (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch-edit trail :label (ocall @input-dom-node :val))
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch-edit trail :label (ocall @input-dom-node :val)))}
                               "OK"]]]]]])})))

(defn modal []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)]
    (r/create-class
      {:component-did-mount (fn [this]
                              ; When modal is closed, clear the context menu target. This prevents the modal
                              ; from retamaining prior state that was cancelled / dismissed
                              (ocall (js/$ (r/dom-node this))
                                     :on "hidden.bs.modal"
                                     (fn [] (dispatch [::evts/set-context-menu-target nil])))
                              (ocall (js/$ (r/dom-node this))
                                     :on "shown.bs.modal"
                                     (fn [] (ocall @input-dom-node :select))))
       :reagent-render (fn [{:keys [file-type label trail]}]
                         [:div#myMineRenameModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Rename"]]
                            [:div.modal-body [:p "Please enter a new name for the folder"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value label) (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch-edit trail :label (ocall @input-dom-node :val))
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch-edit trail :label (ocall @input-dom-node :val)))}
                               "OK"]]]]]])})))

(defn folder-cell []
  (fn [{:keys [file-type trail index label open editing?] :as item}]
    [:div.mymine-row
     {:on-click (fn [] (when-not editing? (dispatch [::evts/toggle-folder-open trail])))}
     [:span.shrink
      (if open
        [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
        [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])]
     [:span.grow
      [:a label]]]))

(defn list-cell []
  (fn [{:keys [file-type trail index label open type] :as item}]
    [:div.mymine-row
     [:svg.icon.icon-document-list.im-type
      {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
     [:span.grow [:a label]]]))

(defn query-cell []
  (fn [{:keys [file-type trail index label open type] :as item}]
    [:div.mymine-row
     [:svg.icon.icon-document-list.im-type
      {:class (str type)} [:use {:xlinkHref "#icon-spyglass"}]]
     [:span.grow [:a label]]]))

(def clj-type type)

(defn table-row []
  (let [selected (subscribe [::subs/selected])]
    (fn [{:keys [editing? friendly-date-created level file-type type trail index read-only? size type] :as row}]
      (let [selected? (some? (some #{trail} @selected))]
        [:tr (-> {:class (clojure.string/join " " [(when selected? (str "im-type box " type))])}
                 ;(merge (click-events trail index))
                 (cond-> (not read-only?) (merge (drag-events trail index))))
         [:td {:style {:padding-left (str (* level 20) "px")}}
          (case file-type
            :folder [folder-cell row]
            :list [list-cell row]
            :query [query-cell row])]
         [:td type]
         [:td size]
         [:td friendly-date-created]]))))


(defn table-header []
  (fn [{:keys [label type key sort-by]}]
    (let [{:keys [asc?]} sort-by]
      [:th
       {:on-click (fn [] (dispatch [::evts/toggle-sort key type (not asc?)]))}
       label
       (when (= key (:key sort-by))
         (if asc?
           [:i.fa.fa-fw.fa-chevron-down]
           [:i.fa.fa-fw.fa-chevron-up]))])))





(defn in? [needle haystack]
  (some? (some #{needle} haystack)))

(defn item []
  (let [selected (subscribe [::subs/selected])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li
       {:class (when (in? trail @selected) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")
          :on-click (fn [x] (dispatch [::evts/toggle-folder-open trail]))}
         [:use {:xlinkHref "#icon-caret-right"}]]
        (if open
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder-open"}]]
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])]
       [:div
        {:on-click (fn [] (dispatch [::evts/toggle-selected trail {:force? true}]))} label]])))

(defn public-folder []
  (let [selected (subscribe [::subs/selected])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li
       {:class (when (in? :public @selected) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")
          :on-click (fn [x] (dispatch [::evts/toggle-folder-open trail]))}
         [:use {:xlinkHref "#icon-caret-right"}]]
        (if open
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-globe"}]]
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-globe"}]])]
       [:div
        {:on-click (fn [] (dispatch [::evts/toggle-selected :public {:force? true}]))} "Public"]])))



(defn file-browser []
  (let [files   (subscribe [::subs/with-public])
        folders (subscribe [::subs/folders])]
    (fn []
      (into [:ul
             [public-folder]] (map (fn [[k v]] ^{:key (str (:trail v))} [item [k v]]) @folders)))))


(defn breadcrumb []
  (let [selected (subscribe [::subs/selected])]
    (fn [coll]
      [:h4
       (if (= (first @selected) :public)
         [:ol.breadcrumb [:li.active [:a "Public Files (Read Only)"]]]
         (into [:ol.breadcrumb]
               (map (fn [{:keys [trail label]}]
                      (let [selected? (in? trail @selected)]
                        [:li {:class (when selected? "active")
                              :on-click (fn [] (dispatch [::evts/toggle-selected trail {:force? true}]))}
                         [:a label]]))
                    (filter #(= :folder (:file-type %)) coll))))])))

(defn main []
  (let [as-list             (subscribe [::subs/as-list])
        sort-by             (subscribe [::subs/sort-by])
        context-menu-target (subscribe [::subs/context-menu-target])
        files               (subscribe [::subs/files])
        bc                  (subscribe [::subs/breadcrumb])]
    (r/create-class
      {:component-did-mount attach-hide-context-menu
       :reagent-render
       (fn []
         [:div.mymine.noselect
          [:div.file-browser [file-browser]]
          [:div.files
           [breadcrumb @bc]
           [:table.table.mymine-table
            [:thead
             [:tr
              [table-header {:label "Name"
                             :key :label
                             :type :alphanum
                             :sort-by @sort-by}]
              [table-header {:label "Type"
                             :key :type
                             :type :alphanum
                             :sort-by @sort-by}]
              [table-header {:label "Size"
                             :key :size
                             :type :alphanum
                             :sort-by @sort-by}]
              [table-header {:label "Last Modified"
                             :key :dateCreated
                             :type :date
                             :sort-by @sort-by}]]]
            (into [:tbody]
                  (map-indexed (fn [idx x]
                                 ^{:key (str (:trail x))} [table-row (assoc x :index idx)]) @files))]]
          [:div.details "test"]
          [modal @context-menu-target]
          [modal-new-folder @context-menu-target]
          ;[context-menu-container @context-menu-target]
          ])})))
