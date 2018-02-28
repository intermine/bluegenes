(ns bluegenes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [accountant.core :as accountant]
            [re-frame.core :refer [dispatch]]))

(defn app-routes
  "When called, define client side URL routes and capture them in the browser history.
  The majority of the routes fire a :set-active-panel but ours is slightly different
  from what's in the re-frame boilerplate. Our :set-active-panel event takes some extra values:
  [:set-active-panel
  :panel-name
  {some data to store in {:db {:panel-params}}
  :some-event-to-fire-after-the-route-has-dispatched]"
  []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
            (dispatch [:set-active-panel :home-panel]))

  (defroute "/debug" []
            (dispatch [:set-active-panel :debug-panel]))

  (defroute "/help" []
            (dispatch [:set-active-panel :help-panel]))

  (defroute "/templates" []
            (dispatch [:set-active-panel :templates-panel]))

  (defroute "/upload" []
            (dispatch [:set-active-panel :upload-panel]))

  (defroute "/upload/:step" {step :step}
            (dispatch [:set-active-panel :upload-panel {:step (keyword step)}]))

  (defroute "/explore" []
            (dispatch [:set-active-panel :explore-panel]))

  (defroute "/search" []
            (dispatch [:set-active-panel :search-panel]))

  (defroute "/querybuilder" []
            (dispatch [:set-active-panel
                       :querybuilder-panel
                       nil
                       [:qb/make-tree]]))

  (defroute "/results" []
            (dispatch [:set-active-panel :results-panel]
                      nil
                      [:results/load-history 0]))

  (defroute "/results/:idx" [idx]
            (dispatch [:set-active-panel :results-panel
                       nil
                       ; URL PARAMETERS ARE ALWAYS STRINGS! Parse as Integer because
                       ; we use the value as a location in a collection (nth [a b c d] "2")
                       [:results/load-history (js/parseInt idx)]]))

  (defroute "/regions" []
            (dispatch [:set-active-panel :regions-panel]))

  (defroute "/mymine" []
            (dispatch [:set-active-panel :mymine-panel]))

  (defroute "/reportpage/:mine/:type/:id" [mine type id]
            (dispatch [:set-active-panel :reportpage-panel
                       {:type type :id id :mine mine}
                       [:load-report mine type id]]))

  ;; --------------------

  (accountant/configure-navigation!
    ;;We use a custom brew accountant version which navigates based on fragments.
    ;;this prevents the annoying double back button problem where the homepage kept on
    ;;popping up in the history when we pressed the back button even though we hadn't been to the homepage at that point in the navigation flow.
    {:nav-handler (fn [path]
                    ;;We don't dispatch the entire url to analytics, just the first part of the page url. otherwise we'd be
                    ;; in scary over-tracking scenarios where we track queries.
                    ;; The try/catch is because some urls are malformed (possibly ones imtables builds, I'm unsure).
                    ;; They throw an error when secretary tries to regex the first part of the url. too many hashes?
                    (try
                      (let [shortened-path (secretary/locate-route-value path)]
                        (js/ga "send" "pageview" (secretary/locate-route-value path)))
                      (catch :default e (.error js/console "Unable to dispatch google analytics for this page: " path " make sure the url is formed correctly. Stacktrace: " e))
                      )
                    (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))}))