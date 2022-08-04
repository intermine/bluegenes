(ns bluegenes.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as rf :refer [dispatch reg-fx reg-cofx]]
            [cljs.core.async :as async :refer [<! close! put!]]
            [cljs-http.client :as http]
            [cognitect.transit :as t]
            [bluegenes.titles :refer [db->title]]
            [bluegenes.utils :refer [encode-file]]
            [oops.core :refer [ocall oget oset!]]
            [goog.dom :as gdom]
            [goog.style :as gstyle]
            [goog.fx.dom :as gfx]
            [bluegenes.config :refer [server-vars]]
            [clojure.string :as str]))

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
   (set! (.-title js/document) title)
   (js/gtag "event" "page_view" (clj->js {:page_title title}))))

(comment
  "To update the document title, add the interceptor to any event that may
  update the db and warrant a change in the title. Make sure to place it
  innermost if there are multiple interceptors (ie. `[foo document-title]`)."
  (reg-event-fx
   :my-event
   [document-title]
   (fn [] ...)))

;; See bottom of namespace for effect registrations and examples  on how to use them

;; Artificial HTTP response to indicate that a request has been aborted.
(def abort-response
  {:status 408
   :success false
   :body :abort})
;; The :im-chan side effect is used to read a value from a channel that
;; represents an HTTP request and dispatches events depending on the status of
;; that request's response.
(reg-fx :im-chan
        (let [previous-requests (atom {})
              active-requests (atom #{})]
          (fn [{:keys [on-success on-failure on-unauthorised chan chans abort abort-active]}]
            ;; `abort` should be used when you have a request which may be sent
            ;; multiple times, and you want new requests to replace pending
            ;; requests of the same `abort` value.
            (when abort
              (some-> @previous-requests (get abort) (doto (put! abort-response) (close!)))
              (swap! previous-requests assoc abort chan))

            ;; `abort-active` is different from `abort` in that instead of being
            ;; used with a request, it's more something you call to cancel all
            ;; active requests, without invoking their `on-failure` function.
            (when abort-active
              (doseq [req @active-requests]
                (put! req abort-response)
                (close! req)))

            ;; === ***WARNING*** READ THE BELOW LINE ***WARNING*** ===
            ;; If you change anything here, make the same change for `chans` below.
            (when chan
              (swap! active-requests conj chan)
              (go
                (let [{:keys [statusCode status] :as response} (<! chan)
                      ;; `statusCode` is part of the response body from InterMine.
                      ;; `status` is part of the response map created by cljs-http.
                      s (or statusCode status)
                      ;; Response can be nil or "" when offline.
                      valid-response? (and (some? response)
                                           (not= response ""))]
                  (swap! active-requests disj chan)
                  ;; Note that `s` can be nil for successful responses, due to
                  ;; imcljs applying a transducer on success. The proper way to
                  ;; check for null responses (which don't have a status code)
                  ;; is to check if the response itself is nil.
                  (cond
                    ;; This first clause will intentionally match on s=nil.
                    (and valid-response?
                         (< s 400)) (dispatch (conj on-success response))
                    (and valid-response?
                         (= s 401)) (if on-unauthorised
                                      (dispatch (conj on-unauthorised response))
                                      (dispatch [:flag-invalid-token]))
                    :else (cond
                            ;; Don't invoke `on-failure` if request was aborted.
                            (and (= s 408) (= (:body response) :abort)) nil

                            on-failure (dispatch (conj on-failure response))

                            :else
                            (.error js/console "Failed imcljs request" response))))))

            ;; Similar to `chan` except there are multiple channels.
            ;; This *should* give the same effect for a single channel when
            ;; wrapped like [chan], but this should be tested liberally before
            ;; you use it to supersede the above.
            (when chans
              (swap! active-requests into chans)
              (go
                (let [all-res (<! (->> (async/merge chans)
                                       (async/reduce conj [])))
                      all-s (map (some-fn :statusCode :status) all-res)
                      all-valid-response? (every? (every-pred some? #(not= % "")) all-res)]
                  (swap! active-requests #(apply disj %1 %2) chans)
                  (cond
                    (and all-valid-response?
                         (every? #(< % 400) all-s)) (dispatch (conj on-success all-res))
                    (and all-valid-response?
                         (some #(= % 401) all-s)) (if on-unauthorised
                                                    (dispatch (conj on-unauthorised all-res))
                                                    (dispatch [:flag-invalid-token]))
                    :else (cond
                            (and (some #(= % 408) all-s)
                                 (some #(= (:body %) :abort) all-res)) nil
                            on-failure (dispatch (conj on-failure all-res))
                            :else
                            (.error js/console "Failed imcljs request" all-res)))))))))

(defn http-fxfn
  "The :http side effect is similar to :im-chan but is more generic and is used
  to support calls to BlueGene's web services. It uses cljs-http: https://github.com/r0man/cljs-http"
  [{:keys [method
           uri
           headers
           timeout
           query-params
           json-params
           transit-params
           form-params
           multipart-params
           response-format
           on-success
           on-failure
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
    (let [c (http-fn (if (str/starts-with? uri "/")
                       ;; Path only = API call to BG backend.
                       (str (:bluegenes-deploy-path @server-vars) uri)
                       ;; Full address = API call to external service.
                       uri)
                     (cond-> {}
                       query-params (assoc :query-params query-params)
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
              (<= 500 status 599) (when on-failure (dispatch (conj on-failure response)))
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
                    :uri "/api"
                    :json-params {:value 1 :another 2}
                    :on-success [:some-success-event]
                    :on-failure [:some-failure-event]}})))

(comment
  (reg-event-fx :do-a/query
                (fn [world]
                  {:bluegenes.effects/im-chan
                   ; or... ::fx/im-chan if the namespace is referred
                   {:chan (imcljs.fetch/rows service query options)
                    :on-success [:save-query-results-event]
                    :on-failure [:warn-user-about-error-event]}})))

;; Switching mines is usually so quick that we don't need a loader.
;; But if it were to take a long time, we'll show a loader.
(reg-fx
 :mine-loader
 (let [timer (atom nil)
       showing? (atom false)
       clear-timer! (fn []
                      (when-let [active-timer @timer]
                        (.clearTimeout js/window active-timer))
                      (when @showing?
                        (dispatch [:hide-mine-loader])
                        (reset! showing? false)))]
   (fn [state]
     (case state
       true (do (clear-timer!)
                (reset! timer
                        (.setTimeout js/window
                                     #(do (dispatch [:show-mine-loader])
                                          (reset! showing? true))
                                     4000)))
       false (clear-timer!)))))

(reg-fx
 :hide-intro-loader
 (fn [_]
   (some-> (ocall js/document :getElementById "wrappy")
           (ocall :remove))))

(reg-fx :message
        (fn [{:keys [id timeout] :or {timeout 5000}}]
          (when (pos? timeout)
            (.setTimeout js/window #(dispatch [:messages/remove id]) timeout))))

;; Simple retry effect for when you're waiting for a value to be ready in
;; app-db, usually as a result of an HTTP request. Usage:
;;
;;     {:retry {:event     ; Event vector in its entirety.
;;              :timeout   ; Milliseconds to wait before retrying (optional).
;;              :max-tries ; Maximum amount of retry attempts (optional).
;;              }}
;;
;; When it finally succeeds, make sure to run the effect with `:success?` set
;; to `true` to clear the retry count for the event ID.
;;
;;     {:retry {:event [:my-event-to-retry {:more "data"}]
;;              :success? true}}
;;
(reg-fx
 :retry
 (let [retries (atom {})]
   (fn [{:keys [timeout max-tries success?] [event-id :as event] :event
         :or {timeout 2000 max-tries 5}}]
     (cond
       ;; Success; no more retries needed.
       success? (swap! retries dissoc event-id)
       ;; Still retries remaining.
       (< (get @retries event-id 0) max-tries)
       (do (swap! retries update event-id (fnil inc 0))
           (.setTimeout js/window
                        #(dispatch event)
                        timeout))
       ;; Exhausted all retries.
       :else (swap! retries dissoc event-id)))))

;; Only works on the template page, where each template element has an ID.
(reg-fx
 :scroll-to-template
 (fn [{:keys [id delay]}]
   (let [scroll! #(gstyle/scrollIntoContainerView (gdom/getElement id) nil true)]
     (if (number? delay)
       (js/setTimeout scroll! delay)
       (scroll!)))))

;; Nice resource for more easing functions: https://gist.github.com/gre/1650294
;; Update: Looks like there are some easing functions in `goog.fx.easing`.
(defn- ease-in-out-cubic [t]
  (if (< t 0.5)
    (* 4 t t t)
    (+ 1 (* (- t 1)
            (- (* 2 t) 2)
            (- (* 2 t) 2)))))

(reg-fx
 :scroll-to-top
 (fn [{:keys [ms]
       :or {ms 500}}]
   (let [doc-elem (gdom/getDocumentScrollElement)
         current-scroll (clj->js ((juxt #(oget % :x) #(oget % :y))
                                  (gdom/getDocumentScroll)))]
     (doto (gfx/Scroll. doc-elem current-scroll #js [0 0] ms ease-in-out-cubic)
       (.play)))))

;; Currently this only logs to console, but in the future we can decide to
;; include more information and perhaps log to a reporting service.
(reg-fx
 :log-error
 (fn [[error-string data-map]]
   (.error js/console
           (str "bluegenes: " error-string)
           (clj->js data-map))))

(reg-fx
 :external-redirect
 (fn [url]
   (.assign js/window.location url)))

;; This is ONLY for use during boot, prior to the router being started.
(reg-fx
 :change-route
 (fn [new-path]
   (.replaceState js/window.history nil "" (str (:bluegenes-deploy-path @server-vars) "/" new-path))))

;; filename - string including extension
;; filetype - string to be appended to 'text/' forming a mime type
;; data     - string representing the contents of the file
(reg-fx
 :download-file
 (fn [{:keys [filename filetype data]}]
   (let [a (ocall js/document :createElement "a")
         url (encode-file data filetype)]
     (oset! a [:style :display] "none")
     (oset! a :href url)
     (oset! a :download filename)
     (ocall js/document.body :appendChild a)
     (ocall a :click)
     (ocall js/window.URL :revokeObjectURL url)
     (ocall js/document.body :removeChild a))))

;; Uses the gtag client to send event data for GA4. This will do nothing if a Google Analytics ID isn't configured.
;; https://developers.google.com/tag-platform/gtagjs/reference#event
(reg-fx
 :track-event
 (fn [[event-name params-map]]
   (if params-map
     (js/gtag "event" event-name (clj->js params-map))
     (js/gtag "event" event-name))))
