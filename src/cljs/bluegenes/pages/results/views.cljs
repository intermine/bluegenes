(ns bluegenes.pages.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.table :as table]
            [bluegenes.pages.results.events]
            [bluegenes.pages.results.subs]
            [bluegenes.pages.results.enrichment.views :as enrichment]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget oget+ ocall oset!]]
            [json-html.core :as json-html]
            [im-tables.views.core :as tables]
            [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]
            [bluegenes.route :as route]
            [bluegenes.components.tools.views :as tools]))

(def custom-time-formatter (time-format/formatter "dd MMM, yy HH:mm"))

(defn adjust-str-to-length [length string]
  (if (< length (count string)) (str (clojure.string/join (take (- length 3) string)) "...") string))

(defn breadcrumb []
  (let [history (subscribe [:results/history])
        history-index (subscribe [:results/history-index])]
    (fn []
      [:div.breadcrumb-container
       [:svg.icon.icon-history [:use {:xlinkHref "#icon-history"}]]
       (into [:div.breadcrumb]
             (map-indexed
              (fn [idx {{title :title} :value}]
                (let [adjusted-title (if (not= idx @history-index) (adjust-str-to-length 20 title) title)]
                  [:div {:class (if (= @history-index idx) "active")
                         :on-click #(dispatch [::route/navigate ::route/list {:title idx}])}
                   [tooltip
                    {:title title}
                    adjusted-title]])) @history))])))

(defn no-results []
  [:div "Hmmm. There are no results. How did this happen? Whoopsie! "
   [:a {:href (route/href ::route/home)}
    "There's no place like home."]])

(defn query-history []
  (let [historical-queries (subscribe [:results/historical-queries])
        current-query (subscribe [:results/history-index])]
    (fn []
      [:div
       [:h3 [:i.fa.fa-clock-o] " Recent Queries"]
       (into [:ul.history-list]
             (map (fn [[title {:keys [source value last-executed]}]]
                    [:li.history-item
                     {:class (when (= title @current-query) "active")
                      :on-click #(dispatch [::route/navigate ::route/list {:title title}])}
                     [:div.title title]
                     [:div.time (time-format/unparse custom-time-formatter (time-coerce/from-long last-executed))]])
                  @historical-queries))])))

(defn main []
  (let [are-there-results? (subscribe [:results/are-there-results?])
        current-list (subscribe [:results/current-list])]
    (fn []
      (let [{:keys [description authorized name type size timestamp]} @current-list]
        (if @are-there-results?
        ;;show results
          [:div.container-fluid.results
           [:div.row
            [:div.col-sm-2
             [query-history]]
            [:div.col-sm-7
             [:div
              [tables/main [:results :table]]
              (when (> (count description) 0)
                [:div.description-div
                 {:style {:background-color "#D2CEBF"  :padding "10px" :overflow "auto"}}
                 [:b "Description: "]
                 description])]
             [:div
              [tools/main]]]
            [:div.col-sm-3
             [enrichment/enrich]]]
           #_[:div.results-and-enrichment
              [:div.col-md-8.col-sm-12.panel]
             ;;[:results :fortable] is the key where the imtables data (appdb) are stored.

              [:div.col-md-4.col-sm-12]]]
        ;;oh noes, somehow we made it here with noresults. Fail elegantly, not just console errors.


          [no-results])))))
