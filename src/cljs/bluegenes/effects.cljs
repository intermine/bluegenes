(ns bluegenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [dispatch subscribe reg-fx]]
            [cljs.core.async :refer [<! close!]]
            [cljs-http.client :as http]))

;; See bottom of namespace for effect registrations and examples  on how to use them

; Handles the I/O of the quicksearch results used for autosuggesting
; values in the main search box
; TODO: This can easily be retired by using the :im-chan effect below
(reg-fx
 :suggest
 (fn [{:keys [c search-term source]}]
   (if (= "" search-term)
     (dispatch [:handle-suggestions nil])
     (go (dispatch [:handle-suggestions (<! c)])))))

"The :im-chan side effect is used to read a value from a channel that represents an HTTP request
and dispatches events depending on the status of that request's response."
(reg-fx :im-chan
        (let [previous-requests (atom {})]
          (fn [{:keys [on-success on-failure chan abort]}]
            ; This http request can be closed early to prevent asynchronous http race conditions
            (when abort
              ; Look for a request stored in the state keyed by whatever value is in 'abort'
              ; and close it
              (some-> @previous-requests (get abort) close!)
              ; Swap the old value for the new value
              (swap! previous-requests assoc abort chan))
            (go
              (let [{:keys [statusCode] :as response} (<! chan)]
                (if (and statusCode (= statusCode 401))
                  (dispatch [:flag-invalid-tokens])
                  (dispatch (conj on-success response))))))))

(defn http-fxfn
  "The :http side effect is similar to :im-chan but is more generic and is used
  to support calls to BlueGene's web services. It uses cljs-http: https://github.com/r0man/cljs-http"
  [{:keys [method
           uri
           headers
           timeout
           json-params
           transit-params
           form-params
           multipart-params
           response-format
           on-success
           on-error
           on-unauthorised
           on-progress-upload
           progress]}]
  (let [token nil
        http-fn (case method
                  :get http/get
                  :post http/post
                  :delete http/delete
                  :put http/put
                  http/get)]
    (let [c (http-fn uri (cond-> {}
                           json-params (assoc :json-params json-params)
                           transit-params (assoc :transit-params transit-params)
                           form-params (assoc :form-params form-params)
                           multipart-params (assoc :multipart-params multipart-params)
                           headers (update :headers #(merge % headers))
                           (and token @token) (update :headers assoc "X-Auth-Token" @token)
                           progress (assoc :progress progress)))]
      (go (let [{:keys [status body] :as response} (<! c)]
            (cond
              (<= 200 status 399) (when on-success (dispatch (conj on-success body)))
              (<= 400 status 499) (when on-unauthorised (dispatch (conj on-unauthorised response)))
              (>= 500 status 599) (when on-error (dispatch (conj on-error response)))
              :else nil)))
      (when on-progress-upload
        (go-loop []
          (let [report (<! progress)]
            (when (= :upload (:direction report))
              (dispatch (conj on-progress-upload report))))
          (recur))))))

;;; Register the effects
(reg-fx ::http http-fxfn)

;;; Examples
(comment
  (reg-event-fx :some/event
                (fn [world]
                  {:bluegenes.effects/http
                   ; or... ::fx/http if the namespace is referred
                   {:method :get
                    :uri "/mymine"
                    :json-params {:value 1 :another 2}
                    :on-success [:some-success-event]
                    :on-error [:some-failure-event]}})))

(comment
  (reg-event-fx :do-a/query
                (fn [world]
                  {:bluegenes.effects/im-chan
                   ; or... ::fx/im-chan if the namespace is referred
                   {:chan (imcljs.fetch/rows service query options)
                    :on-success [:save-query-results-event]
                    :on-error [:warn-user-about-error-event]}})))
