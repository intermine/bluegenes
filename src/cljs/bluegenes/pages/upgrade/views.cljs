(ns bluegenes.pages.upgrade.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.components.idresolver.subs :as subs]
            [bluegenes.components.idresolver.events :as evts]
            [bluegenes.components.idresolver.views :as idresolver]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.route :as route]))

(defn main []
  (let [resolution-response @(subscribe [::subs/resolution-response])
        resolution-error @(subscribe [::subs/resolution-error])
        {:keys [upgrade-list]} @(subscribe [:panel-params])]
    [:div.container.idresolverupload
     [:div.wizard.list-upgrade
      [:a.btn.btn-link.back-button
       {:href (route/href ::route/lists)}
       [icon "chevron-left"]
       "Abandon changes and return to Lists"]
      (cond
        resolution-error [:div.wizard-body
                          [:h3.upgrade-header
                           [:span "Failed to resolve identifiers for: "]
                           [:span.list-name upgrade-list]]
                          [:code (if (= resolution-error "")
                                   ;; It's weird, but really the only way to check for missing web service as older versions will redirect to webapp.
                                   "This mine doesn't support upgrading lists from BlueGenes. Please contact the maintainers to update their InterMine version, or upgrade the list from the legacy webapp instead."
                                   (if-let [err (not-empty (get-in resolution-error [:body :error]))]
                                     err
                                     "Please check your connection and try again later."))]]
        (nil? resolution-response) [:div.wizard-body
                                    [:div.wizard-loader [loader "IDENTIFIERS"]]]
        :else [:div.wizard-body
               [:h3.upgrade-header [:span "Upgrading:"] [:span.list-name upgrade-list]]
               [:hr.upgrade-divider]
               [idresolver/main :upgrade? true]
               [:hr.upgrade-divider]
               [:div.upgrade-footer
                [:button.btn.btn-primary.btn-raised.btn-lg
                 {:on-click #(dispatch [::evts/upgrade-list upgrade-list])}
                 (str "Upgrade and save " upgrade-list)]]])]]))
