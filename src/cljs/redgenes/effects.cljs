(ns redgenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [accountant.core :as accountant]
            [cljs.core.async :refer [<!]]))

(reg-fx
  :navigate
  (fn [url]
    (accountant/navigate! (str "#/" url))))

(reg-fx
  :im-operation
  (fn [{:keys [on-success on-failure response-format op params]}]
    (go (dispatch (conj on-success (<! (op)))))))