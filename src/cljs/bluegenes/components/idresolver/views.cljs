(ns bluegenes.components.idresolver.views
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [clojure.string :refer [join split]]
            [bluegenes.components.icons :as icons]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.idresolver.events :as evts]
            [bluegenes.components.idresolver.subs :as subs]
            [oops.core :refer [oget oget+ ocall]]
            [imcljs.path :as path]
            [bluegenes.route :as route]))

(def allowed-number-of-results [5 10 20 50 100 250 500])

(defn controls []
  (let [results (subscribe [:idresolver/results])
        matches (subscribe [:idresolver/results-matches])
        selected (subscribe [:idresolver/selected])]
    (fn []
      [:div.btn-toolbar.controls
       [:button.btn.btn-warning
        {:class (if (nil? @results) "disabled")
         :on-click (fn [] (dispatch [:idresolver/clear]))}
        "Clear all"]
       [:button.btn.btn-warning
        {:class (if (empty? @selected) "disabled")
         :on-click (fn [] (dispatch [:idresolver/delete-selected]))}
        (str "Remove selected (" (count @selected) ")")]
       [:button.btn.btn-primary.btn-raised
        {:disabled (if (empty? @matches) "disabled")
         :on-click (fn [] (if (some? @matches) (dispatch [:idresolver/analyse true])))}
        "View Results"]])))

(defn organism-identifier
  "Sometimes the ambiguity we're resolving with duplicate ids is the same symbol from two similar organisms, so we'll need to add organism name where known."
  [summary]
  (if (:organism.name summary)
    (str " (" (:organism.name summary) ")")
    ""))

(defn build-duplicate-identifier
  "Different objects types have different summary fields. Try to select one intelligently or fall back to primary identifier if the others ain't there."
  [result]
  (let [summary (:summary result)
        symbol (:symbol summary)
        accession (:primaryAccession summary)
        primaryId (:primaryId summary)]
    (str
     (first (remove nil? [symbol accession primaryId]))
     (organism-identifier summary))))

(defn input-item-duplicate []
  "Input control. allows user to resolve when an ID has matched more than one object."
  (fn [[oid data]]
    [:span.id-item [:span.dropdown
                    [:span.dropdown-toggle
                     {:type "button"
                      :data-toggle "dropdown"}
                     (:input data)
                     [:span.caret]]
                    (into [:ul.dropdown-menu]
                          (map (fn [result]
                                 [:li {:on-click
                                       (fn [e]
                                         (.preventDefault e)
                                         (dispatch [:idresolver/resolve-duplicate
                                                    (:input data) result]))}
                                  [:a (build-duplicate-identifier result)]]) (:matches data)))]]))

(defn get-icon [icon-type]
  (case icon-type
    :MATCH [:svg.icon.icon-checkmark.MATCH [:use {:xlinkHref "#icon-checkmark"}]]
    :UNRESOLVED [:svg.icon.icon-sad.UNRESOLVED [:use {:xlinkHref "#icon-sad"}]]
    :DUPLICATE [:svg.icon.icon-duplicate.DUPLICATE [:use {:xlinkHref "#icon-duplicate"}]]
    :TYPE_CONVERTED [:svg.icon.icon-converted.TYPE_CONVERTED [:use {:xlinkHref "#icon-converted"}]]
    :OTHER [:svg.icon.icon-arrow-right.OTHER [:use {:xlinkHref "#icon-arrow-right"}]]
    [mini-loader "tiny"]))

(def reasons
  {:TYPE_CONVERTED "we're searching for genes and you input a protein (or vice versa)."
   :OTHER " the synonym you input is out of date."})

(defn input-item-converted [original results]
  (let [new-primary-id (get-in results [:matches 0 :summary :primaryIdentifier])
        conversion-reason ((:status results) reasons)]
    [:span.id-item {:title (str "You input '" original "', but we converted it to '" new-primary-id "', because " conversion-reason)}
     original " -> " new-primary-id]))

(defn input-item [{:keys [input] :as i}]
  "visually displays items that have been input and have been resolved as known or unknown IDs (or currently are resolving)"
  (let [result (subscribe [:idresolver/results-item input])
        selected (subscribe [:idresolver/selected])]
    (reagent/create-class
     {:component-did-mount
      (fn [])
      :reagent-render
      (fn [i]
        (let [result-vals (second (first @result))
              class (if (empty? @result)
                      "inactive"
                      (name (:status result-vals)))
              class (if (some #{input} @selected) (str class " selected") class)]
          [:div.id-resolver-item-container
           {:class (if (some #{input} @selected) "selected")}
           [:div.id-resolver-item
            {:class class
             :on-click (fn [e]
                         (.preventDefault e)
                         (.stopPropagation e)
                         (dispatch [:idresolver/toggle-selected input]))}
            [get-icon (:status result-vals)]
            (case (:status result-vals)
              :DUPLICATE [input-item-duplicate (first @result)]
              :TYPE_CONVERTED [input-item-converted (:input i) result-vals]
              :OTHER [input-item-converted (:input i) result-vals]
              :MATCH [:span.id-item (:input i)]
              [:span.id-item (:input i)])]]))})))

(defn debugger []
  (let [everything (subscribe [:idresolver/everything])]
    (fn []
      [:div (json-html/edn->hiccup @everything)])))

(defn selected []
  (let [selected (subscribe [:idresolver/selected])]
    (fn []
      [:div "selected: " (str @selected)])))

(defn preview [result-count]
  (let [results-preview (subscribe [:idresolver/results-preview])
        fetching-preview? (subscribe [:idresolver/fetching-preview?])]
    [:div
     [:h4.title "Results preview:"
      [:small.pull-right "Showing " [:span.count (min 5 result-count)] " of " [:span.count result-count] " Total Good Identifiers. "
       (cond (> result-count 0)
             [:a {:on-click
                  (fn [] (dispatch [:idresolver/analyse true]))}
              "View all >>"])]]
     [preview-table
      :loading? @fetching-preview?
      :query-results @results-preview]]))

(defn paginator []
  (fn [pager results]
    (let [pages (Math/ceil (/ (count results) (:show @pager)))
          rows-in-view (take (:show @pager)
                             (drop (* (:show @pager)
                                      (:page @pager)) results))]
      [:div.paginator
       [:div.previous-next-buttons
        [:button.btn.btn-default.previous-button
         {:on-click (fn [] (swap! pager update
                                  :page (comp (partial max 0) dec)))
          :disabled (< (:page @pager) 1)}
         [:svg.icon.icon-circle-left
          [:use {:xlinkHref "#icon-circle-left"}]]
         "Previous"]
        [:button.btn.btn-default.next-button
         {:on-click (fn [] (swap! pager update
                                  :page (comp (partial min (dec pages)) inc)))
          :disabled (= (:page @pager) (dec pages))}
         "Next"

         [:svg.icon.icon-circle-right
          [:use {:xlinkHref "#icon-circle-right"}]]]]
       [:div.results-count
        [:label "Show"]
        (into
         [:select.form-control
          {:on-change (fn [e]
                        (swap! pager assoc
                               :show (js/parseInt (oget e :target :value))
                               :page 0))
           :value (:show @pager)}]
         (map
          (fn [p]
            [:option {:value p} p])
          (let [to-show (inc
                         (count (take-while
                                 (fn [v]
                                   (<= v (count results)))
                                 allowed-number-of-results)))]
            (take to-show allowed-number-of-results))))
        [:label "results on page "]]
       [:div.page-selector
        (into
         [:select.form-control
          {:on-change
           (fn [e] (swap! pager assoc :page
                          (js/parseInt (oget e :target :value))))
           :disabled (< pages 2)
           :class (when (< pages 2) "disabled")
           :value (:page @pager)}]
         (map (fn [p]
                [:option {:value p} (str "Page " (inc p))]) (range pages)))
        [:label (str "of " pages)]]])))

(defn matches-table []
  (let [pager (reagent/atom {:show 10 :page 0})
        summary-fields (subscribe [:current-summary-fields])
        model (subscribe [:current-model])]
    (fn [type results show-keep?]
      (let [pages (Math/ceil (/ (count results) (:show @pager)))
            rows-in-view (take (:show @pager)
                               (drop (* (:show @pager) (:page @pager)) results))
            type-summary-fields (get @summary-fields (keyword type))]
        [:div
         [:div.alert.alert-result-table.alert-info
          [:p [:svg.icon.icon-info
               [:use {:xlinkHref "#icon-info"}]]
           "An exact match was found for the following identifiers"]]
         [paginator pager results]
         [:table.table.table-condensed.table-striped
          [:thead [:tr [:th {:row-span 2} "Your Identifier"]
                   [:th {:col-span 6} "Matches"]]
           (into [:tr]
                 (map
                  (fn [f]
                    (let [path (path/friendly @model f)]
                      [:th (join " > "
                                 (rest (split path " > ")))]))
                  type-summary-fields))]
          (into [:tbody]
                (->> rows-in-view
                     (map-indexed
                      (fn [row-idx {:keys [summary input id]}]
                        (into
                         [:tr [:td (join ", " input)]]
                         (map
                          (fn [field]
                            (let [without-prefix
                                  (keyword (join "." (rest (split field "."))))]
                              [:td (get summary without-prefix)]))
                          type-summary-fields))))))]]))))

(defn converted-table []
  (let [pager (reagent/atom {:show 10 :page 0})
        summary-fields (subscribe [:current-summary-fields])
        model (subscribe [:current-model])]
    (fn [type results category-kw]
      (let [pages (Math/floor (/ (count results) (:show @pager)))
            rows-in-view (take (:show @pager) (drop (* (:show @pager) (:page @pager)) results))
            type-summary-fields (get @summary-fields (keyword type))]
        [:div
         (case category-kw
           :converted
           [:div.alert.alert-result-table.alert-info
            [:p [:svg.icon.icon-info
                 [:use {:xlinkHref "#icon-info"}]]
             (str "These identifiers matched non-" type
                  " records from which a relationship to a " type
                  " was found")]]
           :other [:div.alert.alert-result-table.alert-info
                   [:p [:svg.icon.icon-info
                        [:use {:xlinkHref "#icon-info"}]]
                    "These identifiers matched old identifiers"]]
           nil)
         [paginator pager results]
         [:table.table.table-condensed.table-striped
          [:thead [:tr [:th {:row-span 2} "Your Identifier"]
                   [:th {:col-span 5} "Matches"]]
           (into
            [:tr]
            (map
             (fn [f]
               (let [path (path/friendly @model f)]
                 [:th (join " > " (rest (split path " > ")))]))
             type-summary-fields))]
          (into
           [:tbody]
           (->> rows-in-view
                (map-indexed
                 (fn [duplicate-idx
                      {:keys [input reason matches]
                       :as duplicate}]
                   (->>
                    matches
                    (map-indexed
                     (fn [match-idx {:keys [summary keep?] :as match}]
                       (into
                        (if (= match-idx 0)
                          [:tr {:class (when keep? "success")}
                           [:td {:row-span (count matches)} input]]
                          [:tr {:class (when keep? "success")}])
                        (map
                         (fn [field]
                           (let [without-prefix
                                 (keyword (join "." (rest (split field "."))))]
                             [:td (get summary without-prefix)]))
                         type-summary-fields)))))))
                (apply concat)))]]))))

(defn not-found-table []
  (let [pager (reagent/atom {:show 10 :page 0})
        summary-fields (subscribe [:current-summary-fields])
        model (subscribe [:current-model])]
    (fn [type results show-keep?]
      (let [pages (Math/floor (/ (count results) (:show @pager)))
            rows-in-view (take (:show @pager)
                               (drop (* (:show @pager) (:page @pager)) results))
            type-summary-fields (get @summary-fields (keyword type))]
        [:div
         [:div.alert.alert-result-table.alert-info
          [:p [:svg.icon.icon-info
               [:use {:xlinkHref "#icon-info"}]]
           (str "These identifiers returned no matches")]]
         [paginator pager results]
         [:table.table.table-condensed.table-striped
          [:thead [:tr [:th "Your Identifier"]]]
          (into [:tbody]
                (->> rows-in-view
                     (map-indexed
                      (fn [row-idx value]
                        [:tr [:td value]]))))]]))))

(defn review-table []
  (let [pager (reagent/atom {:show 5 :page 0})
        summary-fields (subscribe [:current-summary-fields])
        model (subscribe [:current-model])]
    (fn [type results show-keep?]
      (let [pages (Math/floor (/ (count results) (:show @pager)))
            rows-in-view (take (:show @pager) (drop (* (:show @pager) (:page @pager)) results))
            type-summary-fields (get @summary-fields (keyword type))]
        [:div.form
         [:div.alert.alert-result-table.alert-info
          [:p [:svg.icon.icon-info
               [:use {:xlinkHref "#icon-info"}]]
           (str "These identifiers matched more than one " type)]]
         [paginator pager results]
         [:table.table.table-condensed.table-striped
          [:thead [:tr [:th {:row-span 2} "Your Identifier"]
                   [:th {:col-span 6} "Matches"]]
           (into
            [:tr [:th "Keep?"]]
            (map
             (fn [f]
               (let [path (path/friendly @model f)]
                 [:th (join " > " (rest (split path " > ")))])) type-summary-fields))]
          (into
           [:tbody]
           (->>
            rows-in-view
            (map-indexed
             (fn [duplicate-idx {:keys [input reason matches] :as duplicate}]
               (->>
                matches
                (map-indexed
                 (fn [match-idx {:keys [summary keep?] :as match}]
                   (into
                    (if (= match-idx 0)
                      [:tr {:class (when keep? "success")}
                       [:td {:row-span (count matches)} input]]
                      [:tr {:class (when keep? "success")}])
                    (conj
                     (map
                      (fn [field]
                        (let [without-prefix
                              (keyword (join "." (rest (split field "."))))]
                          [:td (get summary without-prefix)])) type-summary-fields)
                     [:td
                      [:div
                       [:label
                        [:input
                         {:type "checkbox"
                          :checked keep?
                          :on-change
                          (fn [e]
                            (dispatch [::evts/toggle-keep-duplicate
                                       duplicate-idx match-idx]))}]]]])))))))
            (apply concat)))]]))))

(defn success-message []
  (fn [count total]
    [:div.alert.alert-success.stat
     [:span
      [:div
       [:svg.icon.icon-checkmark
        [:use {:xlinkHref "#icon-checkmark"}]]
       (str " " count " matching objects were found")]]]))

(defn issues-message []
  (fn [count total]
    [:div.alert.alert-warning.stat
     [:span
      [:div [:svg.icon.icon-duplicate [:use {:xlinkHref "#icon-duplicate"}]]
       (str " " count " identifiers returned multiple objects")]
      [:div "Please"]]]))

(defn not-found-message []
  (fn [count total]
    [:div.alert.alert-danger.stat
     [:span
      [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
      (str " " count " identifiers were not found")]]))

(defn main []
  (let [resolution-response (subscribe [::subs/resolution-response])
        list-name (subscribe [::subs/list-name])
        tab (subscribe [::subs/review-tab])
        stats (subscribe [::subs/stats])]
    ;; Select first tab with results.
    (when-let [stats @stats]
      (dispatch [::evts/update-option :review-tab
                 (some #(when (pos? (get stats %)) %)
                       [:matches :converted :other :issues :notFound])]))
    (fn [& {:keys [upgrade?]}]
      (let [{:keys [matches issues notFound converted duplicates all other]} @stats]
        [:div
         [:div.flex-progressbar
          [:div.title
           [:h4
            (str (+ matches other) " of your " all " identifiers matched a "
                 (str (:type @resolution-response)))]]
          ;; inline styles are actually appropriate here for the
          ;; percentages
          [:div.bars
           (when (> (- matches converted) 0)
             [:div.bar.bar-success
              {:style {:flex (* 100 (/ (+ matches converted) all))}
               :on-click #(dispatch [::evts/update-option :review-tab :matches])}
              (str (- matches converted)
                   (str " Match" (when (> (- matches converted) 1) "es")))])
           (when (> converted 0)
             [:div.bar.bar-success
              {:style {:flex (* 100 (/ (+ matches converted) all))}
               :on-click #(dispatch [::evts/update-option :review-tab :converted])}
              (str converted " Converted")])
           (when (> other 0)
             [:div.bar.bar-success
              {:style {:flex (* 100 (/ other all))}
               :on-click #(dispatch [::evts/update-option :review-tab :other])}
              (str other " Synonym" (when (> other 1) "s"))])
           (when (> duplicates 0)
             [:div.bar.bar-warning
              {:style {:flex (* 100 (/ duplicates all))}
               :on-click #(dispatch [::evts/update-option :review-tab :issues])}
              (str duplicates " Ambiguous")])
           (when (> notFound 0)
             [:div.bar.bar-danger
              {:style {:flex (* 100 (/ notFound all))}
               :on-click #(dispatch [::evts/update-option :review-tab :notFound])}
              (str notFound " Not Found")])]]

         (when (not= duplicates 0)
           [:div.alert.alert-warning.guidance
            [:h4 [:svg.icon.icon-info
                  [:use {:xlinkHref "#icon-info"}]]
             (str " " duplicates " of your identifiers resolved to more than one "
                  (:type @resolution-response))]
            [:p "Please select which objects you want to keep from the "
             [:span.label.label-warning
              [:svg.icon.icon-duplicate
               [:use {:xlinkHref "#icon-duplicate"}]]
              (str " Ambiguous (" duplicates ")")]
             " tab"]])

         (when-not upgrade?
           [:div.save-list
            [:label "List Name"
             [:input
              {:type "text"
               :value @list-name
               :on-change (fn [e]
                            (dispatch
                             [::evts/update-list-name
                              (oget e :target :value)]))}]]
            [:button.cta
             {:on-click (fn [] (dispatch [::evts/save-list]))}
             [:svg.icon.icon-floppy-disk
              [:use {:xlinkHref "#icon-floppy-disk"}]]
             "Save List"]])

         [:ul.nav.nav-tabs.id-resolver-tabs
          (when (> matches 0)
            [:li
             {:class (when (= @tab :matches) "active")
              :on-click
              (fn []
                (dispatch
                 [::evts/update-option :review-tab :matches]))}
             [:a.matches.all-ok
              [:svg.icon.icon-checkmark
               [:use {:xlinkHref "#icon-checkmark"}]]
              (str " Matches ("
                   (- matches converted) ")")]])
          (when (> converted 0)
            [:li
             {:class (when (= @tab :converted) "active")
              :on-click
              (fn [] (dispatch
                      [::evts/update-option :review-tab :converted]))}
             [:a.converted.all-ok
              [:svg.icon.icon-converted
               [:use {:xlinkHref "#icon-converted"}]]
              (str "Converted (" converted ")")]])
          (when (> other 0)
            [:li
             {:class (when (= @tab :other) "active")
              :on-click
              (fn []
                (dispatch
                 [::evts/update-option :review-tab :other]))}
             [:a.synonyms.all-ok
              [:svg.icon.icon-info
               [:use {:xlinkHref "#icon-info"}]]
              (str "Synonyms (" other ")")]])
          (when (> duplicates 0)
            [:li
             {:class (when (= @tab :issues) "active")
              :on-click (fn []
                          (dispatch
                           [::evts/update-option :review-tab :issues]))}
             [:a.ambiguous.needs-attention
              [:svg.icon.icon-duplicate
               [:use {:xlinkHref "#icon-duplicate"}]]
              (str " Ambiguous (" duplicates ")")]])
          (when (> notFound 0)
            [:li
             {:class (when (= @tab :notFound) "active")
              :on-click
              (fn [] (dispatch
                      [::evts/update-option :review-tab :notFound]))}
             [:a.error.not-found
              [:svg.icon.icon-wondering
               [:use {:xlinkHref "#icon-wondering"}]]
              (str "Not Found (" notFound ")")]])]
         [:div.table-container
          (case @tab
            :issues [review-table (:type @resolution-response) (-> @resolution-response :matches :DUPLICATE)]
            :notFound [not-found-table (:type @resolution-response) (-> @resolution-response :unresolved)]
            :converted [converted-table (:type @resolution-response) (-> @resolution-response :matches :TYPE_CONVERTED) :converted]
            :other [converted-table (:type @resolution-response) (-> @resolution-response :matches :OTHER) :other]
            [matches-table (:type @resolution-response) (-> @resolution-response :matches :MATCH)])]]))))
