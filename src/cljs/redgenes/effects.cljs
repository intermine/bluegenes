(ns redgenes.effects
  (:require [re-frame.core :refer [reg-fx]]
            [accountant.core :as accountant]))

(reg-fx
  :navigate
  (fn [url]
    (accountant/navigate! (str "#/" url))))