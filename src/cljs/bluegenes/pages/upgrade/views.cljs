(ns bluegenes.pages.upgrade.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.components.idresolver.subs :as subs]
            [bluegenes.components.idresolver.events :as evts]
            [bluegenes.components.idresolver.views :as idresolver]
            [bluegenes.components.loader :refer [loader]]))

(defn main []
  (let [resolution-response @(subscribe [::subs/resolution-response])
        in-progress? @(subscribe [::subs/in-progress?])]
    [:div.container.idresolverupload
     [:div.wizard
      [:div.wizard-body
       [idresolver/main]
       #_(if (nil? resolution-response)
           (if in-progress?
             [:div.wizard-loader [loader]]
             [:h1 "Nothing is running!"]) ;; TODO make prettier
           [idresolver/main])]]]))
