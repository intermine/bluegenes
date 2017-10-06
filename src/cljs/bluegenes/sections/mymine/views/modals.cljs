(ns bluegenes.sections.mymine.views.modals
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [ocall oset! oget]]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [clojure.string :as s]))

(def operations
  {:combine {:title "Combine Lists"
             :on-success (fn [list-name] (dispatch [::evts/lo-combine list-name]))
             :success-label "Save"
             :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
             :body "The new list will contain all items from all selected lists."}
   :intersect {:title "Intersect Lists"
               :on-success (fn [list-name] (dispatch [::evts/lo-intersect list-name]))
               :success-label "Save"
               :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
               :body "The new list will contain only items that exist in all selected lists."}
   :subtract {:title "Subtract Lists"
              :on-success (fn [list-name] (println "TODO - THIS"))
              :success-label "Save"
              :icon [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
              :body "The new list will contain only items that are unique to each selected list."}})


(defn item []
  (fn [{:keys [name id type size] :as details}]
    [:div.mymine-card
     [:span (:name details)]
     [:span.label.label-default.pull-right size]]))

(defn item-list []
  (fn [items]
    (into [:div] (map (fn [i] ^{:key (:id i)} [item i]) items))))

(defn modal-body-list-operations-commutative
  "This form covers list operations where the order does not matter (combine, intersect)"
  []
  (let [dom-node (atom nil)]
    (r/create-class
      {:component-did-mount (fn [this] (reset! dom-node (js/$ (r/dom-node this))))
       :reagent-render (let [checked-items (subscribe [::subs/checked-details])
                             new-list-name (r/atom nil)]
                         (fn [{:keys [body on-success state]}]
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
                                              13 (do
                                                   ; Call the succeess function with the value of the target
                                                   (on-success @state)
                                                   ; Clear the state for re-use
                                                   (reset! state nil)
                                                   ; Find the modal parent and manually close it
                                                   (-> @dom-node (ocall :closest ".modal") (ocall :modal "hide")))
                                              nil))}]]]))})))

(defn modal-list-operations []
  (let [state (r/atom nil)]
    (r/create-class
      {:component-did-mount (fn [this]
                              ; When the modal is dismissed then clear the state
                              (ocall (js/$ (r/dom-node this)) :on "hidden.bs.modal" (fn [] (reset! state nil))))
       :reagent-render (fn [operation]
                         (let [{:keys [title icon body action on-success success-label] :as details} (get operations operation)]
                           [:div#myTestModal.modal.fade
                            [:div.modal-dialog
                             [:div.modal-content
                              [:div.modal-header [:h2 title]]
                              [:div.modal-body
                               (case operation
                                 :combine [modal-body-list-operations-commutative (assoc details :state state)]
                                 :intersect [modal-body-list-operations-commutative (assoc details :state state)]
                                 :subtract [modal-body-list-operations-commutative (assoc details :state state)]
                                 nil)]
                              [:div.modal-footer
                               [:div.btn-toolbar.pull-right
                                [:button.btn.btn-default
                                 {:data-dismiss "modal"}
                                 "Cancel"]
                                [:button.btn.btn-success.btn-raised
                                 {:data-dismiss "modal"
                                  :on-click (fn [evt]
                                              (do
                                                ; Call the succeess function with the value of the target
                                                (on-success @state)
                                                ; Clear the state for next re-use
                                                (reset! state nil)))}
                                 success-label]]]]]]))})))


(defn dispatch-edit [location key value]
  (dispatch [::evts/update-value location key value]))

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
                                :on-click (fn [] (dispatch [::evts/delete-folder trail]))}
                               "Remove Folder"]]]]]])})))


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
                              [:div.modal-header [:h2 "New Folder"]]
                              [:div.modal-body [:p "Please enter a new name for the folder"]
                               [:input.form-control
                                {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                                 :type "text"
                                 :on-key-up (fn [evt]
                                              (case (oget evt :keyCode)
                                                13 (when (not-blank? (ocall @input-dom-node :val))
                                                     (do ; Detect "Return
                                                       (dispatch [::evts/new-folder trail (ocall @input-dom-node :val)])
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
                                                     (dispatch [::evts/new-folder trail (ocall @input-dom-node :val)])))}
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
                                                   (js/console.log "IT" it)
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
