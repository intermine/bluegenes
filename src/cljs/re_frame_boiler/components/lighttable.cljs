(ns re-frame-boiler.components.lighttable
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]))


(defn table []
  (fn [results]
    [:table.table.small
     (into [:tbody]
           (map (fn [row]
                  (into [:tr]
                        (map (fn [value]
                               [:td (str (apply str (take 100 value)) "...")]) row))) (:results results)))]))

(defn handler [state e]
  (let [query (reagent/props e)
        node  (sel1 (reagent/dom-node e) :.im-target)]
    (go (reset! state (<! (search/raw-query-rows {:root "www.flymine.org/query"}
                                                 query
                                                 {:size   5
                                                  :format "json"}))))))

(defn main []
  (let [state (reagent/atom nil)]
    (reagent/create-class
      {:component-did-mount (partial handler state)
       :reagent-render      (fn [query]
                              [:div [table @state]])})))