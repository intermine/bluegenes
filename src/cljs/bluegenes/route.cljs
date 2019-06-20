(ns bluegenes.route
  (:require [re-frame.core :refer [dispatch reg-sub reg-event-db reg-event-fx reg-fx]]
            [oops.core :refer [ocall]]
            [reitit.core :as r]
            [reitit.coercion :as rc]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

;; Based on the official reitit frontend-re-frame example: (2019.06.18)
;; https://github.com/metosin/reitit/tree/master/examples/frontend-re-frame

;;; Events ;;;

(reg-event-fx
 ::navigate
 (fn [db [_ route & [params query]]]
   {::navigate! {:k route
                 :params params
                 :query query}}))

(reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   ;;; Put side-effects you want to run on every page load/change here!
   ;; Make sure there are no hanging popovers.
   (ocall (js/$ ".popover") "remove")
   ;; Track the page (new-match has :data, so use anything from `routes`).
   (js/ga "send" "pageview" (:template new-match))
   (let [old-match   (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Subscriptions ;;;

(reg-sub
 ::current-route
 (fn [db]
   (:current-route db)))

;;; Effects ;;;

(reg-fx
 ::navigate!
 (fn [{:keys [k params query]}]
   (rfe/push-state k params query)))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))

;; The majority of the routes fire a `:set-active-panel` but ours is slightly
;; different from what's in the re-frame boilerplate. Our `:set-active-panel`
;; event takes some extra values:
;; ```
;; [:set-active-panel
;; :panel-name
;; {some data to store in {:db {:panel-params}}
;; :some-event-to-fire-after-the-route-has-dispatched])
;; ```
;; An alternative way to handling routing would be to replace
;; `:set-active-panel` with a router component, which displays the view (which
;; would be added to the route data map) corresponding to `::current-route`.
;; This means we would have to move the logic handling buffering and forwarding
;; from the `:set-active-panel` event handler into a top-level `:controllers`
;; for all our routes. Doing this would fix the discrepancy we currently have
;; between `:set-active-panel` and the router events.
(def routes
  [["/"
    {:name ::home
     :controllers
     [{:start #(dispatch [:set-active-panel :home-panel])}]}]
   ["/debug/:panel"
    {:name ::debug
     :controllers
     [{:parameters {:path [:panel]}
       :start (fn [{{:keys [panel]} :path}]
                (dispatch [:set-active-panel :debug-panel
                           nil
                           [:bluegenes.pages.developer.events/panel panel]]))}]}]
   ["/help"
    {:name ::help
     :controllers
     [{:start #(dispatch [:set-active-panel :help-panel])}]}]
   ["/templates"
    {:name ::templates
     :controllers
     [{:start #(dispatch [:set-active-panel :templates-panel])}]}]
   ["/upload"
    {:name ::upload
     :controllers
     [{:start #(dispatch [:set-active-panel :upload-panel])}]}]
   ["/upload/:step"
    {:name ::upload-step
     :controllers
     [{:parameters {:path [:step]}
       :start (fn [{{:keys [step]} :path}]
                (dispatch [:set-active-panel :upload-panel
                           {:step (keyword step)}]))}]}]
   ["/explore"
    {:name ::explore
     :controllers
     [{:start #(dispatch [:set-active-panel :explore-panel])}]}]
   ["/search"
    {:name ::search
     :controllers
     [{:start #(dispatch [:set-active-panel :search-panel])}]}]
   ["/querybuilder"
    {:name ::querybuilder
     :controllers
     [{:start #(dispatch [:set-active-panel :querybuilder-panel
                          nil
                          [:qb/make-tree]])}]}]
   ["/results"
    {:name ::results
     :controllers
     [{:start #(dispatch [:set-active-panel :results-panel
                          nil
                          [:results/load-history 0]])}]}]
   ["/results/:title"
    {:name ::results-title
     :controllers
     [{:parameters {:path [:title]}
       :start (fn [{{:keys [title]} :path}]
                (dispatch [:set-active-panel :results-panel
                           nil
                           ; URL PARAMETERS ARE ALWAYS STRINGS! Parse as Integer because
                           ; we use the value as a location in a collection (nth [a b c d] "2")
                           [:results/load-history title]]))}]}]
   ["/regions"
    {:name ::regions
     :controllers
     [{:start #(dispatch [:set-active-panel :regions-panel])}]}]
   ["/mymine"
    {:name ::mymine
     :controllers
     [{:start #(dispatch [:set-active-panel :mymine-panel])}]}]
   ["/reportpage/:mine/:type/:id"
    {:name ::report
     :controllers
     [{:parameters {:path [:mine :type :id]}
       :start (fn [{{:keys [mine type id]} :path}]
                (dispatch [:set-active-panel :reportpage-panel
                           {:type type :id id :mine mine}
                           [:load-report mine type id]]))}]}]])
;; You can do initialisations by adding a :start function to :controllers.
;; :start (fn [& params] (js/console.log "Entering page"))
;; Teardowns can also be done by using the :stop key.
;; :stop  (fn [& params] (js/console.log "Leaving page"))])

(defn on-navigate [new-match]
  (when new-match
    (dispatch [::navigated new-match])))

(def router
  (rf/router
   routes
   {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))
