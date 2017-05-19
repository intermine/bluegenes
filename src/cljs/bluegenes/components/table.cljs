(ns bluegenes.components.table
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [oops.core :refer [ocall oapply oget oset!]]))

(defn handle [expanded? e]
  (let [props (reagent/props e)
        node  (sel1 (reagent/dom-node e) :.im-target)]
    (if @expanded?
      (-> (ocall js/imtables "loadTable"
                 node
                 (clj->js {:start 0 :size 25})
                 (clj->js {:service   (:service props)
                           :query     (:query props)
                           :TableCell {:IndicateOffHostLinks false}}))
          (ocall "then" (fn [success] nil) (fn [error] (.error js/console error)))))))

(defn main [_ & [expanded]]
  (reagent/create-class
    (let [expanded? (reagent.core/atom expanded)]
      {:component-did-mount  (partial handle expanded?)
       :component-did-update (partial handle expanded?)
       :reagent-render       (fn [{:keys [service query]}]
                               [:div
                                {:on-click #(reset! expanded? true)}
                                (if @expanded?
                                  [:div.imtables [:div.im-target]]
                                  [:div (:title query)])])})))
