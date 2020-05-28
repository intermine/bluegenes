(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.components.tools.subs :as subs]))

(defn main []
  (into [:div.tools]
        (for [{{:keys [cljs human]} :names} @(subscribe [::subs/suitable-tools])]
          [:div.tool {:class cljs}
           [:h3 human]
           [:div {:id cljs}]])))
