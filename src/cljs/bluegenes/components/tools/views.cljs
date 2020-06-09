(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.components.tools.subs :as subs]))

(defn main []
  (let [suitable-tools @(subscribe [::subs/suitable-tools])]
    (into [:div.tools]
          (for [{{:keys [cljs human]} :names} suitable-tools]
            [:div.tool {:class cljs}
             [:h3 human]
             [:div {:id cljs}]]))))
