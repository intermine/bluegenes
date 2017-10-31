(ns bluegenes.sections.mymine.views.modals
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [ocall oset! oget]]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [clojure.string :as s]))

(def operations
  {:combine {:title "Combine"
             :on-success (fn [list-name] (dispatch [::evts/lo-combine (s/trim list-name)]))
             :success-label "Save"
             :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
             :body "The new list will contain all items from all selected lists."}
   :intersect {:title "Intersect"
               :on-success (fn [list-name] (dispatch [::evts/lo-intersect (s/trim list-name)]))
               :success-label "Save"
               :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-intersect"}]]
               :body "The new list will contain only items that exist in all selected lists."}
   :difference {:title "Difference"
                :on-success (fn [list-name] (dispatch [::evts/lo-difference (s/trim list-name)]))
                :success-label "Save"
                :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-subtract"}]]
                :body "The new list will contain only items that are unique to each selected list."}
   :subtract {:title "Subtract"
              :on-success (fn [list-name] (dispatch [::evts/lo-subtract (s/trim list-name)]))
              :success-label "Save"
              :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-difference"}]]
              :body "The new list will contain only items from the first list that are not in the second list."}})

(defn empty-set []
  [:div.backdrop
   [:h4 "Empty"]])

(defn item []
  (fn [{:keys [name id type size] :as details}]
    [:div.mymine-card
     [:div.content
      [:span (:name details)]
      [:span.label.label-default.pull-right size]]]))

(defn item-list []
  (fn [items]
    (into [:div.backdrop] (map (fn [i] ^{:key (:id i)} [item i]) items))))

(defn swappable-item []
  (fn [{:keys [name id type size] :as details} bucket-index]
    [:div.mymine-card
     [:span.ico
      [:button.btn.btn-primary.btn-slim
       [:svg.icon.icon
        {:class (if (zero? bucket-index) "icon-circle-down" "icon-circle-up")
         :on-click (fn [] (dispatch [::evts/lo-move-bucket bucket-index id]))}
        [:use {:xlinkHref (if (zero? bucket-index) "#icon-circle-down" "#icon-circle-up")}]]]]
     [:span.content
      [:span (:name details)]
      [:span.label.label-default.pull-right size]]]))


(defn swappable-item-list []
  (fn [items bucket-index]
    (if (some? (not-empty items))
      (into [:div.backdrop] (map (fn [i] ^{:key (:id i)} [swappable-item i bucket-index]) items))
      [empty-set])
    ))

(defn modal-body-list-operations-ordered
  "This form covers list operations where the order does not matter (combine, intersect)"
  []
  (let [dom-node (atom nil)]
    (r/create-class
      {:component-did-mount (fn [this] (reset! dom-node (js/$ (r/dom-node this))))
       :reagent-render (let [sg              (subscribe [::subs/suggested-modal-state])
                             suggested-state (subscribe [::subs/suggested-modal-state-details])
                             checked-items   (subscribe [::subs/checked-details])
                             new-list-name   (r/atom nil)]
                         (fn [{:keys [errors body on-success state]}]
                           [:div
                            ;[:p body]
                            [:h4 "Keep items from these lists"]
                            [swappable-item-list (first @suggested-state) 0]
                            [:div.pull-right
                             [:button.btn.btn-primary
                              {:on-click (fn [] (dispatch [::evts/lo-reverse-order]))}
                              [:svg.icon.icon-swap-vertical.icon-2x [:use {:xlinkHref "#icon-swap-vertical"}]]]]
                            [:div.clearfix]
                            [:h4 "... that are not in these lists"]
                            [swappable-item-list (second @suggested-state) 1]
                            [:div.form-group
                             [:label "New List Name"]
                             [:input.form-control
                              {:type "text"
                               :value @state
                               :on-change (fn [evt] (reset! state (oget evt :target :value)))
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (when
                                                   (not (s/blank? @state))
                                                   (do
                                                     ; Call the succeess function with the value of the target
                                                     (on-success @state)
                                                     ; Clear the state for re-use
                                                     (reset! state "")
                                                     ; Find the modal parent and manually close it
                                                     (-> @dom-node (ocall :closest ".modal") (ocall :modal "hide"))))
                                              nil))}]
                             (when (true? (:name-taken? errors))
                               [:div.alert.alert-warning "A list with this name already exists"])]]))})))

(defn modal-body-list-operations-commutative
  "This form covers list operations where the order does not matter (combine, intersect)"
  []
  (let [dom-node (atom nil)]
    (r/create-class
      {:component-did-mount (fn [this] (reset! dom-node (js/$ (r/dom-node this))))
       :reagent-render (let [checked-items (subscribe [::subs/checked-details])
                             new-list-name (r/atom nil)]
                         (fn [{:keys [errors body on-success state]}]
                           [:div
                            [:p body]
                            [item-list @checked-items]
                            [:div.form-group
                             [:label "New List Name"]
                             [:input.form-control
                              {:type "text"
                               :value @state
                               :on-change (fn [evt] (reset! state (oget evt :target :value)))
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (when
                                                   (not (s/blank? @state))
                                                   (do
                                                     ; Call the succeess function with the value of the target
                                                     (on-success @state)
                                                     ; Clear the state for re-use
                                                     (reset! state "")
                                                     ; Find the modal parent and manually close it
                                                     (-> @dom-node (ocall :closest ".modal") (ocall :modal "hide"))))
                                              nil))}]
                             (when (true? (:name-taken? errors))
                               [:div.alert.alert-warning "A list with this name already exists"])]]))})))

(defn modal-list-operations []
  (let [state     (r/atom "")
        all-lists (subscribe [:lists/filtered-lists])
        disabled  (r/atom false)]
    (r/create-class
      {:component-did-update (fn [])
       :component-did-mount (fn [this]
                              ; When the modal is dismissed then clear the state
                              (ocall (js/$ (r/dom-node this)) :on "hidden.bs.modal" (fn [] (reset! state nil))))
       :reagent-render (fn [operation]
                         (let [{:keys [title icon body action on-success success-label] :as details} (get operations operation)
                               errors {:name-taken? (some? (not-empty (filter (comp #{(s/trim (or @state ""))} :name) @all-lists)))
                                       :name-blank? (s/blank? @state)}]
                           [:div#myTestModal.modal.fade
                            [:div.modal-dialog
                             [:div.modal-content
                              [:div.modal-header [:h2 title]]
                              [:div.modal-body
                               (case operation
                                 :combine [modal-body-list-operations-commutative (assoc details :state state :errors errors)]
                                 :intersect [modal-body-list-operations-commutative (assoc details :state state :errors errors)]
                                 :difference [modal-body-list-operations-commutative (assoc details :state state :errors errors)]
                                 :subtract [modal-body-list-operations-ordered (assoc details :state state :errors errors)]
                                 nil)]
                              [:div.modal-footer
                               [:div.btn-toolbar.pull-right
                                [:button.btn.btn-default
                                 {:data-dismiss "modal"}
                                 "Cancel"]
                                [:button.btn.btn-success.btn-raised
                                 {:disabled (some true? (vals errors))
                                  :data-dismiss "modal"
                                  :on-click (fn [evt]
                                              (do
                                                ; Call the succeess function with the value of the target
                                                (on-success @state)
                                                ; Clear the state for next re-use
                                                (reset! state "")))}
                                 success-label]]]]]]))})))


(defn dispatch-edit [location key value]
  (dispatch [::evts/rename-tag value]))

(def not-blank? (complement s/blank?))

(defn modal-delete-folder []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)]
    (r/create-class
      {:component-did-mount (fn [this]
                              ; When modal is closed, clear the context menu target. This prevents the modal
                              ; from retamaining prior state that was cancelled / dismissed
                              (ocall (js/$ (r/dom-node this))
                                     :on "hidden.bs.modal"
                                     (fn [] (dispatch [::evts/set-context-menu-target nil])))
                              #_(ocall (js/$ (r/dom-node this))
                                       :on "shown.bs.modal"
                                       (fn [] (ocall @input-dom-node :select))))
       :reagent-render (fn [{:keys [file-type label trail]}]
                         [:div#myMineDeleteFolderModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Are you sure you want to remove this tag from all associated items?"]]
                            [:div.modal-body [:p "Items will not be deleted"]
                             #_[:input.form-control
                                {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                                 :type "text"
                                 :on-key-up (fn [evt]
                                              (case (oget evt :keyCode)
                                                13 (do ; Detect "Return
                                                     (dispatch [::evts/delete-folder trail])
                                                     (ocall @modal-dom-node :modal "hide"))
                                                nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/delete-tag trail]))}
                               "Remove Tag"]]]]]])})))


(defn modal-delete-item []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)
        dets           (subscribe [::subs/details])]
    (r/create-class
      {:component-did-mount (fn [this]
                              ; When modal is closed, clear the context menu target. This prevents the modal
                              ; from retamaining prior state that was cancelled / dismissed
                              (ocall (js/$ (r/dom-node this))
                                     :on "hidden.bs.modal"
                                     (fn [] (dispatch [::evts/set-context-menu-target nil])))
                              #_(ocall (js/$ (r/dom-node this))
                                       :on "shown.bs.modal"
                                       (fn [] (ocall @input-dom-node :select))))
       :reagent-render (fn [{:keys [file-type label trail]}]
                         [:div#myMineDeleteModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Are you sure you want to delete this item?"]]
                            [:div.modal-body [:p "This action cannot be undone"]
                             #_[:input.form-control
                                {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                                 :type "text"
                                 :on-key-up (fn [evt]
                                              (case (oget evt :keyCode)
                                                13 (do ; Detect "Return
                                                     (dispatch [::evts/delete-folder trail])
                                                     (ocall @modal-dom-node :modal "hide"))
                                                nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/delete trail (:name @dets)]))}
                               "Remove Folder"]]]]]])})))


(defn modal-new-folder []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)
        menu-details   (subscribe [::subs/menu-details])]
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

                         (let [{:keys [file-type label trail]} @menu-details]
                           [:div#myMineNewFolderModal.modal.fade
                            {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                             :role "dialog"
                             :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                            [:div.modal-dialog
                             [:div.modal-content
                              [:div.modal-header [:h2 "New Tag"]]
                              [:div.modal-body [:p "Please enter a new name for the tag"]
                               [:input.form-control
                                {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                                 :type "text"
                                 :on-key-up (fn [evt]
                                              (case (oget evt :keyCode)
                                                13 (when (not-blank? (ocall @input-dom-node :val))
                                                     (do ; Detect "Return
                                                       (dispatch [::evts/store-tag (ocall @input-dom-node :val)])
                                                       (ocall @modal-dom-node :modal "hide")))
                                                nil))}]]
                              [:div.modal-footer
                               [:div.btn-toolbar.pull-right
                                [:button.btn.btn-default
                                 {:data-dismiss "modal"}
                                 "Cancel"]
                                [:button.btn.btn-success.btn-raised
                                 {:data-dismiss "modal"
                                  :on-click (fn [] (when (not-blank? (ocall @input-dom-node :val))
                                                     (dispatch [::evts/store-tag (ocall @input-dom-node :val)])))}
                                 "OK"]]]]]]))})))

(defn modal-copy []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)
        dets           (subscribe [::subs/details])
        menu-details   (subscribe [::subs/menu-details])]
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
                         [:div#myMineCopyModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Copy List"]]
                            [:div.modal-body [:p "Please enter a name for the new list"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch [::evts/copy trail (:name @dets) (ocall @input-dom-node :val)])
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/copy trail (:name @dets) (ocall @input-dom-node :val)]))}
                               "OK"]]]]]])})))


(defn modal-lo []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)
        dets           (subscribe [::subs/details])]
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
                         [:div#myMineLoModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Combine Lists"]]
                            [:div.modal-body [:p "Please enter a name for the new list"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch [::evts/lo-combine (ocall @input-dom-node :val)])
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/lo-combine (ocall @input-dom-node :val)]))}
                               "OK"]]]]]])})))

(defn modal-lo-intersect []
  (let [input-dom-node (r/atom nil)
        modal-dom-node (r/atom nil)
        dets           (subscribe [::subs/details])]
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
                         [:div#myMineLoIntersectModal.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Intersec Lists"]]
                            [:div.modal-body [:p "Please enter a name for the new list"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch [::evts/lo-intersect (ocall @input-dom-node :val)])
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/lo-intersect (ocall @input-dom-node :val)]))}
                               "OK"]]]]]])})))

(defn modal-rename-list []
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
       :reagent-render (fn [{:keys [file-type label trail] :as it}]
                         [:div#myMineRenameList.modal.fade
                          {:tab-index "-1" ; Allows "escape" key to work.... for some reason
                           :role "dialog"
                           :ref (fn [e] (when e (reset! modal-dom-node (js/$ e))))} ; Get a handle on our
                          [:div.modal-dialog
                           [:div.modal-content
                            [:div.modal-header [:h2 "Rename List"]]
                            [:div.modal-body [:p "Please enter a new name for the folder"]
                             [:input.form-control
                              {:ref (fn [e] (when e (do (oset! e :value label) (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch [::evts/rename-list (ocall @input-dom-node :val)])
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
                            [:div.modal-body [:p "Please enter a new name for the tag"]
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
