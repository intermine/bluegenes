(ns bluegenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [dispatch subscribe reg-fx]]
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

(def method-map {:get http/get
                 :post http/post
                 :delete http/delete})

(reg-fx
  :http
  (fn [{:keys [uri on-success on-failure params method] :as o}]
    (let [http-fn (method method-map)]
      (go (let [{:keys [status body]} (<! (http-fn uri (when (not= method :get) {:transit-params params})))]
            (cond
              (and on-success (<= 200 status 399)) (dispatch (conj on-success body))
              (and on-failure (>= status 400)) (dispatch (conj on-failure body))
              :else nil))))))

(reg-fx
  ::http
  (let [token nil]
    (fn [{:keys [method
                 uri
                 headers
                 timeout
                 json-params
                 transit-params
                 multipart-params
                 response-format
                 on-success
                 on-error
                 on-unauthorised
                 on-progress-upload
                 progress]}]
      (let [http-fn (case method
                      :get http/get
                      :post http/post
                      :delete http/delete
                      :put http/put
                      http/get)]
        (let [c (http-fn uri (cond-> {}
                                     json-params (assoc :json-params json-params)
                                     transit-params (assoc :transit-params transit-params)
                                     multipart-params (assoc :multipart-params multipart-params)
                                     headers (update :headers #(merge % headers))
                                     (and token @token) (update :headers assoc "X-Auth-Token" @token)
                                     progress (assoc :progress progress)))]
          (go (let [{:keys [status body] :as response} (<! c)]
                (cond
                  (<= 200 status 399) (when on-success (dispatch (conj on-success response)))
                  (<= 400 status 499) (when on-unauthorised (dispatch (conj on-unauthorised response)))
                  (>= 500 status 599) (when on-error (dispatch (conj on-error response)))
                  :else nil)))
          (when on-progress-upload
            (go-loop []
                     (let [report (<! progress)]
                       (when (= :upload (:direction report))
                         (dispatch (conj on-progress-upload report))))
                     (recur))))))))