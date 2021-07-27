(ns bluegenes.route
  (:require [re-frame.core :refer [subscribe dispatch reg-sub reg-event-db reg-event-fx reg-fx]]
            [oops.core :refer [ocall]]
            [reitit.core :as r]
            [reitit.coercion :as rc]
            [reitit.coercion.spec :as rcs]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [bluegenes.config :refer [read-default-ns]]
            [clojure.string :as str]))

;; # Quickstart guide:
;; (aka. I just want to route something but don't want to read all this code!)
;; - Don't forget to import the route namespace!
;; ```
;; (require '[bluegenes.route :as route])
;; ```
;; - Href
;; ```
;; [:a {:href (route/href ::route/upload-step {:step "save"})} "My anchor"]
;; ```
;; - Dispatch (for on-click and event handlers)
;; ```
;; (dispatch [::route/navigate ::route/upload-step {:step "save"}])
;; ```
;; ## Order of arguments: `route params query`
;; Only route is required, the rest are optional. Although you will get a
;; warning if you use a route that expects params, without specifying params.
;; Note that the `:mine` param is an exception to this, since it's the parent
;; of all routes, so it gets injected automatically from db if not specified.
;;
;; There is also an event you can dispatch to step back in the history.
;; ```
;; (dispatch [::route/go-back])
;; ```

;; Based on the official reitit frontend-re-frame example: (2019.06.18)
;; https://github.com/metosin/reitit/tree/master/examples/frontend-re-frame

;;; Events ;;;

(reg-event-fx
 ::navigate
 (fn [{db :db} [_ route & [params query]]]
   {::navigate! {:k route
                 :query query
                 :params (update params :mine #(or % (:current-mine db)))}}))

(reg-event-fx
 ::go-back
 (fn [{db :db} [_]]
   {::go-back! {}}))

;; This event handler is for internal use by router.
;; Do not dispatch unless you know what you're doing!
(reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match   (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Subscriptions ;;;

(reg-sub
 ::current-route
 (fn [db]
   (:current-route db)))

;;; Effects ;;;
;; You should dispatch the event handlers above instead of using the effects
;; directly. Such is the way of re-frame!

(reg-fx
 ::navigate!
 (fn [{:keys [k params query]}]
   (rfe/push-state k params query)))

(reg-fx
 ::go-back!
 (fn [_]
   (.back js/window.history)))

;;; Utility functions ;;;

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (let [current-mine (subscribe [:current-mine-name])]
     (rfe/href k (update params :mine #(or % @current-mine)) query))))

(defn force-controllers-rerun
  "Force controllers to rerun on the next router start or navigation, whichever
  comes first. This would be equivalent to the URL path changing to blank, and
  then to the new path (which would be the same path in the case of a router start).
  Depends on an implementation detail of reitit.frontend.controllers/apply-controllers,
  wherein it does a simple equality check of the controller maps before applying."
  [db]
  (update-in db [:current-route :controllers]
             (partial mapv #(assoc % ::force-rerun true))))

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
  ["/"
   [":mine"
    {:controllers
     [{:parameters {:path [:mine]}
       :start (fn [{{:keys [mine]} :path}]
                (dispatch [:set-current-mine mine]))}]}
    [""
     {:name ::home
      :controllers
      [{:start (fn []
                 (dispatch [:home/clear])
                 (dispatch [:set-active-panel :home-panel
                            nil
                            [:bluegenes.events.blog/fetch-rss]]))}]}]
    ["/admin"
     {:name ::admin
      :controllers
      [{:start (fn []
                 (dispatch [:set-active-panel :admin-panel
                            nil
                            [:bluegenes.pages.admin.events/init]])
                 (dispatch [:bluegenes.components.tools.events/fetch-tools]))}]}]
    ["/tools"
     {:name ::tools
      :controllers
      [{:start (fn []
                 (dispatch [:set-active-panel :tools-panel
                            nil
                            [:bluegenes.pages.tools.events/init]]))}]}]
    ["/profile"
     {:name ::profile
      :controllers
      [{:start #(dispatch [:set-active-panel :profile-panel
                           nil
                           [:bluegenes.pages.profile.events/load-profile]])}]}]
    ["/debug/:panel"
     {:name ::debug
      :controllers
      [{:parameters {:path [:panel]}
        :start (fn [{{:keys [panel]} :path}]
                 (dispatch [:set-active-panel :debug-panel
                            nil
                            [:bluegenes.pages.developer.events/panel panel]]))}]}]
    ["/templates"
     {:name ::templates
      :controllers
      [{:start #(dispatch [:set-active-panel :templates-panel])}]}]
    ["/templates/:template"
     {:name ::template
      :controllers
      [{:parameters {:path [:template]}
        :start (fn [{{:keys [template]} :path}]
                 (dispatch [:template-chooser/open-template (keyword template)]))
        :stop (fn []
                (dispatch [:template-chooser/deselect-template]))}]}]
    ["/upload"
     [""
      {:name ::upload
       :controllers
       [{:start #(dispatch [:set-active-panel :upload-panel])}]}]
     ["/:step"
      {:name ::upload-step
       :controllers
       [{:parameters {:path [:step]}
         :start (fn [{{:keys [step]} :path}]
                  (dispatch [:set-active-panel :upload-panel
                             {:step (keyword step)}]))}]}]]
    ["/upgrade"
     {:name ::upgrade
      :controllers
      [{:parameters {:query [:name]}
        :start (fn [{{:keys [name]} :query}]
                 (dispatch [:set-active-panel :upgrade-panel
                            {:upgrade-list name}
                            [:bluegenes.components.idresolver.events/resolve-identifiers {:name name}]]))}]}]
    ["/search"
     {:name ::search
      :controllers
      [{:parameters {:query [:keyword]}
        :start (fn [{{:keys [keyword]} :query}]
                 (dispatch [:search/start-scroll-handling])
                 (dispatch [:set-active-panel :search-panel
                            nil
                            [:search/begin-search keyword]]))
        :stop #(dispatch [:search/stop-scroll-handling])}]}]
    ["/querybuilder"
     {:name ::querybuilder
      :controllers
      [{:start #(dispatch [:set-active-panel :querybuilder-panel
                           nil
                           [:bluegenes.pages.querybuilder.events/load-querybuilder]])}]}]
    ["/results/:title"
     {:name ::results
      :controllers
      [{:parameters {:path [:title]}
        :start (fn [{{:keys [title]} :path}]
                 ;; We have to clear the previous query and tools entity, as
                 ;; otherwise we may show old results and tools from the
                 ;; previous invocation. This way, we'll only show the new
                 ;; results and tools once they're ready.
                 (dispatch [:results/clear])
                 (dispatch [:widgets/reset])
                 (dispatch [:clear-ids-tool-entity])
                 (dispatch [:viz/clear])
                 (dispatch [:set-active-panel :results-panel
                            nil
                            [:results/view-list title]])
                 (dispatch [:results/listen-im-table-changes]))
        :stop (fn []
                (dispatch [:results/unlisten-im-table-changes]))}]}]
    ["/regions"
     {:name ::regions
      :controllers
      [{:start (fn []
                 (dispatch [:regions/fetch-organisms])
                 (dispatch [:set-active-panel :regions-panel]))}]}]
    ["/lists"
     {:name ::lists
      :controllers
      [{:start (fn []
                 (dispatch [:set-active-panel :lists-panel
                            nil
                            [:lists/initialize]]))}]}]
    ["/report/:type/:id"
     {:name ::report
      :controllers
      [{:parameters {:path [:mine :type :id]}
        :start (fn [{{:keys [mine type id]} :path}]
                 (let [id (js/parseInt id 10)]
                   (dispatch [:clear-ids-tool-entity])
                   (dispatch [:viz/clear])
                   (dispatch [:set-active-panel :reportpage-panel
                              {:type type, :id id, :format "id", :mine mine}
                              [:load-report mine type id]])))
        :stop #(dispatch [:bluegenes.pages.reportpage.events/stop-scroll-handling])}]}]
    ["/share/:lookup"
     {:name ::share
      :controllers
      [{:parameters {:path [:mine :lookup]}
        :start (fn [{{:keys [mine lookup]} :path}]
                 (dispatch [:handle-permanent-url mine lookup]))}]}]
    ["/resetpassword"
     {:name ::resetpassword
      :controllers
      [{:parameters {:query [:token]}
        :start (fn [{{:keys [token]} :query}]
                 (dispatch [:bluegenes.events.auth/clear-reset-password-page])
                 (dispatch [:set-active-panel :reset-password-panel
                            {:token token}]))}]}]]])
;; You can do initialisations by adding a :start function to :controllers.
;; :start (fn [& params] (js/console.log "Entering page"))
;; Teardowns can also be done by using the :stop key.
;; :stop  (fn [& params] (js/console.log "Leaving page"))])

(defn on-navigate [new-match]
  ;; - Put side-effects you want to run on every page load/change here!
  ;; Make sure there are no hanging popovers.
  (ocall (js/$ ".popover") "remove")
  ;; When clicking a bar in a vega-lite chart, it shows the results in a new
  ;; page and the tooltip lingers.
  (ocall (js/$ "#vg-tooltip-element") "remove")
  ;; Track the page (new-match has :data, so can use anything from `routes`).
  (try
    (js/ga "send" "pageview" (:path new-match))
    (catch js/Error _))
  ;; - Handle actual navigation.
  (if new-match
    (dispatch [::navigated new-match])
    ;; We end up here when there are no matches (empty or invalid path).
    ;; This does not apply when the path references a nonexistent mine.
    (let [paths (str/split (.. js/window -location -pathname) #"/")
          target-mine (-> paths
                          (second)
                          (or (name (read-default-ns))))]
      (dispatch [::navigate ::home {:mine target-mine}])
      (when (> (count paths) 2)
        (dispatch [:messages/add
                   {:markup [:span "You have been redirected to the home page as the path "
                             [:em (->> paths (drop 2) (str/join "/"))]
                             " could not be resolved."]
                    :style "warning"
                    :timeout 0}])))))

(def router
  (rf/router
   routes
   {:data {:coercion rcs/coercion}}))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))
