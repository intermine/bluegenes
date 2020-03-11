(ns bluegenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as rf :refer [dispatch subscribe reg-fx reg-cofx]]
            [cljs.core.async :refer [<! close!]]
            [cljs-http.client :as http]
            [cognitect.transit :as t]
            [bluegenes.titles :refer [db->title]]))

;; Cofx and fx which you use from event handlers to read/write to localStorage.

(reg-cofx
 :local-store
 (fn [coeffects key]
   (let [key (str key)
         value (t/read (t/reader :json) (.getItem js/localStorage key))]
     (assoc coeffects :local-store value))))

(reg-fx
 :persist
 (fn [[key value]]
   (let [key (str key)]
     (if (some? value)
       (.setItem js/localStorage key (t/write (t/writer :json-verbose) value))
       ;; Specifying `nil` as value removes the key instead.
       (.removeItem js/localStorage key)))))

;; Examples (we do not endorse storing your cats in localStorage)

(comment
  (reg-event-fx
   :read-cats
   [(inject-cofx :local-store :my-cats)]
   (fn [{db :db, my-cats :local-store} [_]]
     {:db (update db :cat-park into my-cats)})))

(comment
  (reg-event-fx
   :write-cats
   (fn [_ [_ my-cats]]
     {:persist [:my-cats my-cats]})))

;; Interceptor and effect to set the title of the web page.

(def document-title
  "Interceptor that updates the document title based on db."
  (rf/->interceptor
   :id :document-title
   :after (fn [context]
            (let [db (get-in context [:effects :db])
                  title (db->title db)]
              (assoc-in context [:effects :document-title] title)))))

(reg-fx
 :document-title
 (fn [title]
   (set! (.-title js/document) title)))

(comment
  "To update the document title, add the interceptor to any event that may
  update the db and warrant a change in the title. Make sure to place it
  innermost if there are multiple interceptors (ie. `[foo document-title]`)."
  (reg-event-fx
   :my-event
   [document-title]
   (fn [] ...)))

;; See bottom of namespace for effect registrations and examples  on how to use them

;; The :im-chan side effect is used to read a value from a channel that
;; represents an HTTP request and dispatches events depending on the status of
;; that request's response.
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
              (let [{:keys [statusCode status] :as response} (<! chan)
                    ;; `statusCode` is part of the response body from InterMine.
                    ;; `status` is part of the response map created by cljs-http.
                    s (or statusCode status)
                    ;; Response can be nil or "" when offline.
                    valid-response? (and (some? response)
                                         (not= response ""))]
                ;; Note that `s` can be nil for successful responses, due to
                ;; imcljs applying a transducer on success. The proper way to
                ;; check for null responses (which don't have a status code)
                ;; is to check if the response itself is nil.
                (cond
                  ;; This first clause will intentionally match on s=nil.
                  (and valid-response?
                       (< s 400)) (dispatch (conj on-success response))
                  (and valid-response?
                       (= s 401)) (if on-failure
                                    (dispatch (conj on-failure response))
                                    (dispatch [:flag-invalid-token]))
                  :else (cond
                          on-failure (dispatch (conj on-failure response))
                          ;; If `abort` is specified, it's possible that this request's
                          ;; channel was closed by a subsequent request. In this case,
                          ;; there's no error and we don't want to log it.
                          (and abort (nil? response)) nil
                          :else
                          (.error js/console "Failed imcljs request" response))))))))

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
              (<= 500 status 599) (when on-error (dispatch (conj on-error response)))
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

(reg-fx :message
        (fn [{:keys [id timeout] :or {timeout 5000}}]
          (when (pos? timeout)
            (.setTimeout js/window #(dispatch [:messages/remove id]) timeout))))
