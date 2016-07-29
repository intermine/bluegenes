(ns re-frame-boiler.components.search
  (:require [re-frame.core :as re-frame :refer [subscribe]]))

(defn main []
  (let [_ nil]
    (fn []
      [:input.form-control.input-lg
       {:type "text"
        :placeholder "Search"}])))