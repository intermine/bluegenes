(ns bluegenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [accountant.core :as accountant]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

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
    (go
      (let [{:keys [statusCode] :as response} (<! chan)]
        (if (and statusCode (= statusCode 401))
          (dispatch [:flag-invalid-tokens])
          (dispatch (conj on-success response)))))))

(reg-fx
  :im-operation
  (fn [{:keys [on-success on-failure response-format op params]}]
    (go (dispatch (conj on-success (<! (op)))))))

(reg-fx
  :im-operation-n
  (fn [v]
    (doall (map (fn [{:keys [on-success on-failure response-format op params]}]
                  (go (dispatch (conj on-success (<! (op)))))) v))))

(def method-map {:get  http/get
                 :post http/post})

(reg-fx
  :http
  (fn [{:keys [uri on-success on-failure params method] :as o}]
    (let [http-fn (method method-map)]
      (go (let [{:keys [status body]} (<! (http-fn uri (when (not= method :get) {:transit-params params})))]
            (cond
              (<= 200 status 399) (dispatch (conj on-success body))
              :else (dispatch (conj on-failure body))))))))