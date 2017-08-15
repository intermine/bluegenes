(ns bluegenes.sections.mymine.views.modals
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [ocall oset! oget]]
            [bluegenes.events.mymine :as evts]))


(defn dispatch-edit [location key value]
  (dispatch [::evts/update-value location key value]))

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
                              {:ref (fn [e] (when e (do (oset! e :value "") (reset! input-dom-node (js/$ e)))))
                               :type "text"
                               :on-key-up (fn [evt]
                                            (case (oget evt :keyCode)
                                              13 (do ; Detect "Return
                                                   (dispatch [::evts/new-folder trail (ocall @input-dom-node :val)])
                                                   (ocall @modal-dom-node :modal "hide"))
                                              nil))}]]
                            [:div.modal-footer
                             [:div.btn-toolbar.pull-right
                              [:button.btn.btn-default
                               {:data-dismiss "modal"}
                               "Cancel"]
                              [:button.btn.btn-success.btn-raised
                               {:data-dismiss "modal"
                                :on-click (fn [] (dispatch [::evts/new-folder trail (ocall @input-dom-node :val)]))}
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
