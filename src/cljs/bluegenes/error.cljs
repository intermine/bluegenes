(ns bluegenes.error
  (:require [re-frame.core :refer [subscribe dispatch-sync]]
            [reagent.core :as reagent]
            [oops.core :refer [oget]]
            [goog.json :as json]
            [goog.string :as gstring]
            [cljs-time.core :as time]
            [cljs-time.format :refer [unparse formatters]]
            [bluegenes.config :refer [server-vars]]))

(defn mailto-string [error]
  (let [support-email (or @(subscribe [:current-mine/support-email]) "support@intermine.org")
        current-url (oget js/window :location :href)
        service-url (get-in @(subscribe [:current-mine]) [:service :root])
        current-date (unparse (formatters :rfc822) (time/now))]
    (str "mailto:" support-email
         "?subject=" (gstring/urlEncode "[BLUEGENES] - Uncaught error")
         "&body=" (gstring/urlEncode
                   (str "Description:

page: " current-url "
service: " service-url "
date: " current-date "
-------------------------------
ERROR: " error)))))

(defn error-string [{:keys [error info]}]
  (str error
       \newline
       (or (oget info :componentStack)
           (json/serialize info))))

(defn error-icons []
  [:svg
   {:version "1.1"
    :height "0"
    :width "0"
    :style {:position "absolute" :width 0 :height 0}}
   [:defs
    [:symbol#icon-bug
     {:viewBox "0 0 32 32"}
     [:path
      {:d
       "M32 18v-2h-6.040c-0.183-2.271-0.993-4.345-2.24-6.008h5.061l2.189-8.758-1.94-0.485-1.811 7.242h-5.459c-0.028-0.022-0.056-0.043-0.084-0.064 0.21-0.609 0.324-1.263 0.324-1.944 0-3.305-2.686-5.984-6-5.984s-6 2.679-6 5.984c0 0.68 0.114 1.334 0.324 1.944-0.028 0.021-0.056 0.043-0.084 0.064h-5.459l-1.811-7.242-1.94 0.485 2.189 8.758h5.061c-1.246 1.663-2.056 3.736-2.24 6.008h-6.040v2h6.043c0.119 1.427 0.485 2.775 1.051 3.992h-3.875l-2.189 8.757 1.94 0.485 1.811-7.243h3.511c1.834 2.439 4.606 3.992 7.708 3.992s5.874-1.554 7.708-3.992h3.511l1.811 7.243 1.94-0.485-2.189-8.757h-3.875c0.567-1.217 0.932-2.565 1.051-3.992h6.043z"}]]]])

(defn error-panel [{:keys [error on-reset]}]
  (let [error-text (error-string error)]
    [:div.well.well-lg.error-container
     [:div.heading
      [:svg.icon.icon-bug [:use {:xlinkHref "#icon-bug"}]]
      [:div
       [:h1 "Something went wrong!"]
       [:h4 "BlueGenes experienced an uncaught error"]]]
     [:p "This could indicate a bug in BlueGenes or the InterMine instance returning malformed data. If you think BlueGenes should be able to handle this error, please send the pre-filled bug report below by clicking the button below. This will open an email: any description of what you were doing before this happened would be very helpful."]
     [:p "Use the " [:em "Reset"] " button below to start BlueGenes from a blank slate."]
     [:pre.text-danger error-text]
     [:div.button-group
      [:button.btn.btn-raised.btn-primary
       {:on-click on-reset}
       "Reset"]
      [:a.btn.btn-raised.btn-primary
       {:href (mailto-string error-text)}
       [:i.fa.fa-envelope] " Send a bug report"]]]))

(defn error-boundary
  [& children]
  (let [caught? (reagent/atom false)
        !error (reagent/atom nil)]
    (reagent/create-class
     {:display-name "BlueGenesRootErrorBoundary"
      :get-derived-state-from-error (fn [_]
                                      (reset! caught? true)
                                      #js {})
      :component-did-catch (fn [_ error info]
                             (reset! !error {:error error :info info}))
      :render (fn [this]
                (let [clear-error! (fn []
                                     ;; Change URL to root without triggering router.
                                     (.pushState js/window.history nil ""
                                                 (str (:bluegenes-deploy-path @server-vars) "/"))
                                     ;; Boot and clear error.
                                     (dispatch-sync [:boot])
                                     (reset! !error nil)
                                     (reset! caught? false))]
                  (if @caught?
                    (when-let [error @!error]
                      [:div.approot
                       [error-icons]
                       [:main
                        [:div.container.error-page
                         [error-panel
                          {:error error
                           :on-reset clear-error!}]]]])
                    (into [:<>] (reagent/children this)))))})))
