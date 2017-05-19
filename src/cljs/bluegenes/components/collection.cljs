(ns bluegenes.components.collection
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.search :as search]))

(defn handle [e]
  (let [props (reagent/props e)
        node  (sel1 (reagent/dom-node e) :.im-target)]
    (go (<! (search/raw-query-rows {:root @(subscribe [:mine-url])}
                                   props
                                   {:format "json"})))))

(defn main []
  (reagent/create-class
    {:component-did-mount  handle
     :component-did-update handle
     :reagent-render       (fn [query]
                             [:div (str "query" query)])}))
