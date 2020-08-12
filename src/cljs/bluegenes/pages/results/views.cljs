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
            [bluegenes.components.tools.views :as tools]
            [bluegenes.components.viz.views :as viz]))

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
                         :on-click #(dispatch [::route/navigate ::route/results {:title idx}])}
                   [tooltip
                    {:title title}
                    adjusted-title]])) @history))])))

(defn no-results
  "Show an appropriate message when the list does not exist."
  []
  [:div "Hmmm. There are no results. How did this happen? Whoopsie! "
   [:a {:href (route/href ::route/home)}
    "There's no place like home."]])

(defn query-history
  "Gives an overview of recent queries or lists, allowing you to jump between them."
  []
  (let [historical-queries (subscribe [:results/historical-queries])
        current-query (subscribe [:results/history-index])]
    (fn []
      [:div
       [:h3 [:i.fa.fa-clock-o] " Recent Queries"]
       (into [:ul.history-list]
             (map (fn [[title {:keys [source value last-executed display-title]}]]
                    [:li.history-item
                     {:class (when (= title @current-query) "active")
                      :on-click #(dispatch [::route/navigate ::route/results {:title title}])}
                     [:div.title (or display-title title)]
                     [:div.time (time-format/unparse custom-time-formatter (time-coerce/from-long last-executed))]])
                  @historical-queries))])))

(defn description-input
  "Textarea input for updating the description of a list."
  [_title initial-text]
  (let [text (reagent/atom (or initial-text ""))
        error (subscribe [:results/errors :description])
        stop-editing #(dispatch [:list-description/edit false])]
    (fn [title _initial-text]
      [:div.description-edit
       [:label "Description"]
       [:textarea.form-control
        {:rows 5
         :placeholder "Describe the contents of this list."
         :value @text
         :ref #(.focus (js/$ %))
         :on-change #(reset! text (oget % "target" "value"))}]
       [:div.controls
        [:button.btn
         {:type "button"
          :on-click stop-editing}
         "Cancel"]
        [:button.btn.btn-primary.btn-raised
         {:type "button"
          :on-click #(dispatch [:list-description/update title @text])}
         "Save"]
        (when-let [e @error] [:p.failure e])]])))

(defn description-box
  "Shows the list description if one is available, and let's you toggle editing."
  []
  (let [editing? (subscribe [:list-description/editing?])
        start-editing #(dispatch [:list-description/edit true])]
    (fn [title description]
      (if @editing?
        [description-input title description]
        (if (not-empty description)
          [:div.description
           [:b "Description: "]
           description
           [:button.btn.btn-slim {:type "button"
                                  :on-click start-editing}
            [:svg.icon.icon-edit [:use {:xlinkHref "#icon-edit"}]]
            "Edit description"]]
          [:div.description
           [:button.btn.btn-slim {:type "button"
                                  :on-click start-editing}
            [:svg.icon.icon-edit [:use {:xlinkHref "#icon-edit"}]]
            "Add description"]])))))

(defn main
  "Result page for a list or query."
  []
  (let [are-there-results? (subscribe [:results/are-there-results?])
        current-list (subscribe [:results/current-list])]
    (fn []
      (let [{:keys [description authorized name type size timestamp] :as list} @current-list]
        (if @are-there-results?
        ;;show results
          [:div.container-fluid.results
           {:style {:width "100%"}}
           [:div.row
            [:div.col-sm-3.col-lg-2
             [query-history]
             [:div.hidden-lg
              [enrichment/enrich]]]
            [:div.col-sm-9.col-lg-7
             [:div.results-table
              [tables/main [:results :table]]
              (when (and list authorized)
                ;; Only show when results are for a list, not a query.
                ;; And only when the user is authorized to edit it.
                [description-box name description])]
             [viz/main]
             [:div
              [tools/main]]]
            [:div.col-sm-3.visible-lg-block
             [enrichment/enrich]]]
           #_[:div.results-and-enrichment
              [:div.col-md-8.col-sm-12.panel]
             ;;[:results :fortable] is the key where the imtables data (appdb) are stored.

              [:div.col-md-4.col-sm-12]]]
        ;;oh noes, somehow we made it here with noresults. Fail elegantly, not just console errors.


          [no-results])))))
