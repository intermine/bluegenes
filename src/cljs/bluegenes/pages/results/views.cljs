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
            [bluegenes.components.viz.views :as viz]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.pages.lists.utils :refer [internal-tag?]]
            [bluegenes.pages.lists.views :refer [pretty-timestamp]]))

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

#_(defn description-input
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

#_(defn description-box
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

#_(defn list-description []
    (let [{:keys [description authorized name] :as list} @(subscribe [:results/current-list])]
      (when (and list authorized)
        ;; Only show when results are for a list, not a query.
        ;; And only when the user is authorized to edit it.
        [description-box name description])))

(defn back-button []
  (let [intent @(subscribe [:results/intent])]
    [:button.btn.btn-link.back-button
     {:on-click #(dispatch [::route/go-back])}
     [icon "chevron-left"]
     (str "Back to "
          (case intent
            :query "Query Builder"
            :search "Search"
            :list "Lists"
            :template "Templates"
            "previous page"))]))

(defn query-details []
  (let [{:keys [title]} @(subscribe [:results/query])
        {:keys [size authorized timestamp type tags
                description] :as list} @(subscribe [:results/current-list])]
    [:div.query-details
     {:title "Use the Edit button on the Lists page to change details"}
     [:div.query-or-list
      [:span.query-title title]
      (when list ; You won't have this data if it's an unsaved query.
        [:<>
         [:div.list-size-auth
          [:span (str "[" size "]")]
          (if authorized
            [icon "user-circle" nil ["authorized"]]
            [icon "globe"])]
         [:span (pretty-timestamp timestamp)]
         [:code.start {:class (str "start-" type)} type]
         (into [:div.list-tags]
               ;; Hide internal tags.
               (for [tag (remove internal-tag? tags)]
                 [:code.tag tag]))])]
     (when-not (empty? description)
       [:p.list-description description])]))

(defn main
  "Result page for a list or query."
  []
  (let [are-there-results? (subscribe [:results/are-there-results?])
        intent (subscribe [:results/intent])]
    (fn []
      [:div.container-fluid.results
       (when @are-there-results?
         [:div.row
          [:div.col-sm-3.col-lg-2
           [query-history]
           [:div.hidden-lg
            [enrichment/enrich]]]
          [:div.col-sm-9.col-lg-7
           [:h2.results-heading "Query Results"]
           [back-button]
            ;; Query details is only interesting if it's a saved list.
           (when (= @intent :list)
             [query-details])
           [:div.results-table
            [tables/main [:results :table]]]
           [viz/main]
           [:div
            [tools/main]]]
          [:div.col-sm-3.visible-lg-block
           [enrichment/enrich]]])])))
