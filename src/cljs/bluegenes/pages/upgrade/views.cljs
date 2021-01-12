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
        {:keys [upgrade-list]} @(subscribe [:panel-params])]
    [:div.container.idresolverupload
     [:div.wizard.list-upgrade
      [:a.btn.btn-link.back-button
       {:href (route/href ::route/lists)}
       [icon "chevron-left"]
       "Abandon changes and return to Lists"]
      (if (nil? resolution-response)
        [:div.wizard-body
         [:div.wizard-loader [loader "IDENTIFIERS"]]]
        [:div.wizard-body
         [:h3.upgrade-header [:span "Upgrading:"] [:span.list-name upgrade-list]]
         [:hr.upgrade-divider]
         [idresolver/main :upgrade? true]
         [:hr.upgrade-divider]
         [:div.upgrade-footer
          [:button.btn.btn-primary.btn-raised.btn-lg
           {:on-click #(dispatch [::evts/upgrade-list upgrade-list])}
           (str "Upgrade and save " upgrade-list)]]])]]))
