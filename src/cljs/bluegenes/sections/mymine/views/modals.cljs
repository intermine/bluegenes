(ns bluegenes.sections.mymine.views.modals
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [ocall oset! oget]]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [clojure.string :as s]))


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
                            [:div.modal-header [:h2 "Are you sure you want to remove this folder?"]]
                            [:div.modal-body [:p "Contents of the folder will be moved to the 'unsorted' folder."]
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
