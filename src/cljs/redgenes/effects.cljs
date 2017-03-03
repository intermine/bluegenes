(ns redgenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [accountant.core :as accountant]
            [cljs.core.async :refer [<!]]))

(reg-fx
  :suggest
  (fn [{:keys [c search-term source]}]
    (if (= "" search-term)
      (dispatch [:handle-suggestions nil])
      (go (dispatch [:handle-suggestions (<! c)])))))

(reg-fx
  :navigate
  (fn [url]
    (accountant/navigate! url)))

(reg-fx
  :im-chan
  (fn [{:keys [on-success on-failure response-format chan params]}]
    (go (dispatch (conj on-success (<! chan))))))

(reg-fx
  :im-operation
  (fn [{:keys [on-success on-failure response-format op params]}]
    (go (dispatch (conj on-success (<! (op)))))))

(reg-fx
  :im-operation-n
  (fn [v]
    (doall (map (fn [{:keys [on-success on-failure response-format op params]}]
                  (go (dispatch (conj on-success (<! (op)))))) v))))
