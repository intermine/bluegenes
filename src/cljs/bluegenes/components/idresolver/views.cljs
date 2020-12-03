(ns bluegenes.components.idresolver.views
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [clojure.string :refer [join split]]
            [bluegenes.components.icons :as icons]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [bluegenes.components.loader :refer [loader mini-loader]]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.idresolver.events :as evts]
            [bluegenes.components.idresolver.subs :as subs]
            [oops.core :refer [oget oget+ ocall]]
            [imcljs.path :as path]
            [bluegenes.route :as route]))

;;; TODOS:


(def separators (set ",; "))

(def timeout 1500)

(def allowed-number-of-results [5 10 20 50 100 250 500])

(defn splitter "Splits a string on any one of a set of strings."
  [string]
  (->> (clojure.string/split string (re-pattern (str "[" (reduce str separators) "\\r?\\n]")))
       (remove nil?)
       (remove #(= "" %))))

(defn has-separator?
  "Returns true if a string contains any one of a set of strings."
  [str]
  (some? (some separators str)))

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

(defn submit-input
  ([input val]
   (reset! val "")
   (submit-input input))
  ([input]
   (dispatch [:idresolver/resolve (splitter input)])))

(defn input-box []
  (reagent/create-class
   (let [val (reagent/atom nil)
         timer (reagent/atom nil)]
     {:reagent-render (fn []
                        [:input#identifierinput.freeform
                         {:type "text"
                          :placeholder "Type or paste identifiers here..."
                          :value @val
                          :on-key-press
                          (fn [e]
                            (let [keycode (.-charCode e)
                                  input (.. e -target -value)]
                              (cond (= keycode 13)
                                    (submit-input input val))))
                           ;;not all keys are picked up by on key press or on-change so we need to do both.
                          :on-change
                          (fn [e]
                            (let [input (.. e -target -value)]
                               ;;we have a counter that automatically submits the typed entry if the user waits long enough (currently 1.5s).
                               ;stop old auto-submit counter.
                              (js/clearInterval @timer)
                               ;start new timer again
                              (reset! timer (js/setTimeout #(submit-input input val) timeout))
                               ;submit the stuff
                              (if (has-separator? input)
                                (do
                                  (js/clearInterval @timer)
                                  (submit-input input val))
                                (reset! val input))))}])
       ;;autofocus on the entry field when the page loads
      :component-did-mount (fn [this] (.focus (dom/dom-node this)))})))

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

(defn parse-files [files]
  (dotimes [i (.-length files)]
    (let [rdr (js/FileReader.)
          the-file (aget files i)]
      (set! (.-onload rdr)
            (fn [e]
              (let [file-content (.-result (.-target e))
                    file-name (if (= ";;; " (.substr file-content 0 4))
                                (let [idx (.indexOf file-content "\n\n")]
                                  (.slice file-content 4 idx))
                                (.-name the-file))]
                (submit-input file-content))))
      (.readAsText rdr the-file))))

(defn handle-drag-over [state-atom evt]
  (reset! state-atom true)
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))

(defn handle-drop-over [state-atom evt]
  (reset! state-atom false)
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (parse-files files)))

(defn obj->clj
  "Convert the top level of a javascript object to clojurescript.
  Unconditionally attempts on any js property, unlike js->clj"
  [obj]
  (reduce (fn [m k] (assoc m (keyword k) (oget+ obj k))) {} (js-keys obj)))

(defn bytes->size [bytes]
  (let [sizes ["Bytes" "KB" "MB" "GB" "TB"]]
    (if (= bytes 0)
      "0 bytes"
      (let [idx (js/parseInt (js/Math.floor (/ (js/Math.log bytes) (js/Math.log 1000))))]
        (str (js/Math.round (/ bytes (js/Math.pow 1000 idx)) 2) " " (nth sizes idx))))))

(defn file []
  (fn [js-File]
    (let [file (obj->clj js-File)]
      [:tr.file
       [:td.file-name (:name file)]
       [:td.file-size (bytes->size (:size file))]
       [:td.remove-file [:a
                         {:on-click (fn [] (dispatch [::evts/unstage-file js-File]))}
                         [:svg.icon.icon-close
                          [:use {:xlinkHref "#icon-close"}]]]]])))

(defn file-manager []
  (let [files (subscribe [::subs/staged-files])
        textbox-identifiers (subscribe [::subs/textbox-identifiers])
        stage-options (subscribe [::subs/stage-options])
        stage-status (subscribe [::subs/stage-status])
        upload-elem (reagent/atom nil)]
    (fn []
      [:div.file-manager
       ;;file inputs are notoriously unstyleable. We hide it and style our own.
       [:input.hidden
        {:type "file"
         :ref (fn [e] (reset! upload-elem e))
         :multiple true
         :on-click (fn [e]
                     (.stopPropagation e))
         ;;otherwise we just end up focusing on the input on the left/top.
         :on-change (fn [e]
                      (dispatch
                       [::evts/stage-files (oget e :target :files)]))}]
       [:div.form-group.file-list
        (when @files
          (into
           [:table.files
            [:thead
             [:tr
              [:th "File name"]
              [:th "Size"]
              [:th]]]] ; don't need a heading for remove button

           (map (fn [js-File] [file js-File]) @files)))
        [:button.btn.btn-default.btn-raised
         {:on-click (fn [] (-> @upload-elem js/$ (ocall :click)))
          :disabled @textbox-identifiers}
         (if @files "Add more files" "Browse")]]])))

(defn debugger []
  (let [everything (subscribe [:idresolver/everything])]
    (fn []
      [:div (json-html/edn->hiccup @everything)])))

(defn selected []
  (let [selected (subscribe [:idresolver/selected])]
    (fn []
      [:div "selected: " (str @selected)])))

(defn delete-selected-handler [e]
  (let [keycode (.-charCode e)]
    (cond
      (= keycode 127) (dispatch [:idresolver/delete-selected])
      :else nil)))

(defn key-down-handler [e]
  (case (.. e -keyIdentifier)
    "Control" (dispatch [:idresolver/toggle-select-multi true])
    "Shift" (dispatch [:idresolver/toggle-select-range true])
    nil))

(defn key-up-handler [e]
  (case (.. e -keyIdentifier)
    "Control" (dispatch [:idresolver/toggle-select-multi false])
    "Shift" (dispatch [:idresolver/toggle-select-range false])
    nil))

(defn attach-body-events []

  (dommy/unlisten! (sel1 :body) :keypress delete-selected-handler)
  (dommy/listen! (sel1 :body) :keypress delete-selected-handler)

  (dommy/unlisten! (sel1 :body) :keydown key-down-handler)
  (dommy/listen! (sel1 :body) :keydown key-down-handler)

  (dommy/unlisten! (sel1 :body) :keyup key-up-handler)
  (dommy/listen! (sel1 :body) :keyup key-up-handler))

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

(defn select-organism-option []
  (fn [organism disable-organism?]
    [:div.form-group
     [:label "Organism"]
     [im-controls/select-organism
      {:value organism
       :disabled disable-organism?
       :class (when disable-organism? "disabled")
       :on-change (fn [organism]
                    (dispatch [::evts/update-option :organism organism]))}]]))

(defn select-type-option []
  (let [model (subscribe [:current-model])]
    (fn [type]
      [:div.form-group
       [:label "List type"]
       [im-controls/select-type
        {:value type
         :qualified? true
         :on-change (fn [type] (dispatch [::evts/update-type @model type]))}]])))

(defn case-sensitive-option []
  (fn [bool]
    [:div.clearfix
     [:div.form-group
      [:label {:for "caseSensitiveCheckbox"} "Identifiers are case sensitive"]
      [:input#caseSensitiveCheckbox
       {:type "checkbox"
        :checked bool
        :on-change (fn [e]
                     (dispatch [::evts/update-option :case-sensitive (oget e :target :checked)]))}]]]))

(defn guidance []
  "list upload guidance text. no functional / interactive bits."
  [:div.guidance-and-title
   [:h2 "Create a new list"]
   [:div.list-upload-guidance
    [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
    [:div
     [:p "Select the type of list to create and then enter your identifiers or upload them from a file."]
     [:ul
      [:li "Separate identifiers by a comma, space, tab or new line"]
      [:li "Qualify any identifiers that contain whitespace with double quotes like so: \"even skipped\""]]]]])

(defn upload-step []
  (let [files (subscribe [::subs/staged-files])
        textbox-identifiers (subscribe [::subs/textbox-identifiers])
        options (subscribe [::subs/stage-options])
        tab (subscribe [::subs/upload-tab])]
    (fn []
      (let [{:keys [organism type case-sensitive disable-organism?]} @options]
        [:div.id-resolver
         [guidance]
         [:div.input-info
          [:div.select-organism-and-list-type
           [select-type-option type]
           [select-organism-option organism disable-organism?]
           [case-sensitive-option case-sensitive]]
          [:div.identifier-input
           [:ul.nav.nav-tabs
            [:li
             {:class
              (cond-> ""
                (= @tab nil) (str " active")
                @files (str "disabled"))
              :on-click (fn []
                          (when-not @files
                            (dispatch
                             [::evts/update-option :upload-tab nil])))
              :disabled true}
             [:a
              [:svg.icon.icon-free-text
               [:use {:xlinkHref "#icon-free-text"}]] " Free Text"]]
            [:li {:class (cond-> ""
                           (= @tab :file) (str " active")
                           @textbox-identifiers (str "disabled"))
                  :on-click (fn []
                              (when-not @textbox-identifiers
                                (dispatch
                                 [::evts/update-option :upload-tab :file])))
                  :disabled true}
             [:a [:svg.icon.icon-upload [:use {:xlinkHref "#icon-upload"}]] " File Upload"]]]

           [:div.form-group.nav-tab-body

            (case @tab
              :file [file-manager]
              [:textarea.form-control
               {:on-change (fn [e] (dispatch
                                    [::evts/update-textbox-identifiers
                                     (oget e :target :value)]))
                :value @textbox-identifiers
                :class (when @files "disabled")
                :spellCheck false
                :disabled @files
                :rows 5}])]
           [:div.btn-toolbar.wizard-toolbar
            (let [example? (subscribe [::subs/example?])]
              ;; if we don't have example text available, don't show the
              ;; example button, but do log to the console that there's a problm
              (if @example?
                [:button.btn.btn-default.btn-raised
                 {:on-click (fn [] (dispatch [::evts/load-example]))}
                 "Example"]
                (.debug js/console
                        "No example button available due to missing or misconfigured example in the InterMine properties")))
            [:button.btn.btn-default.btn-raised
             {:on-click (fn [] (dispatch [::evts/reset]))}
             "Reset"]
            [:button.btn.btn-primary.btn-raised
             {:on-click (fn [] (dispatch [::evts/parse-staged-files @files @textbox-identifiers @options]))
              :disabled (when (and (nil? @files) (nil? @textbox-identifiers)) true)}
             "Continue"]]]]]))))

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

(defn review-step []
  (let [resolution-response (subscribe [::subs/resolution-response])
        list-name (subscribe [::subs/list-name])
        tab (subscribe [::subs/review-tab])
        stats (subscribe [::subs/stats])]
    (when (pos? (:duplicates @stats))
      (dispatch [::evts/update-option :review-tab :issues]))
    (fn []
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
           "Save List"]]

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

(defn review-step-container []
  (let [resolution-response (subscribe [::subs/resolution-response])
        in-progress? (subscribe [::subs/in-progress?])]
    (fn []
      (if (= nil @resolution-response)
        (if @in-progress?
          [:div.wizard-loader [loader]]
          (dispatch [::route/navigate ::route/upload-step {:step "input"}]))
        [review-step]))))

(defn breadcrumbs []
  (let [response (subscribe [::subs/resolution-response])]
    (fn [view]
      [:h4.breadcrumbs
       [:ol.breadcrumb
        [:li
         {:class (when (or (= view nil) (= view :input)) "active")}
         [:a {:href (route/href ::route/upload-step {:step "input"})}
          [:svg.icon.icon-upload
           [:use {:xlinkHref "#icon-upload"}]] "Upload"]]
        [:li.disabled {:class (when (= view :save) "active")}
         (if @response
           [:a {:href (route/href ::route/upload-step {:step "save"})}
            [:svg.icon.icon-floppy-disk
             [:use {:xlinkHref "#icon-floppy-disk"}]]
            "Save"]
           [:span
            [:svg.icon.icon-floppy-disk
             [:use {:xlinkHref "#icon-floppy-disk"}]]
            "Save"])]]])))

(defn wizard []
  (let [view (subscribe [::subs/view])
        panel-params (subscribe [:panel-params])]
    (fn []
      [:div.wizard
       [breadcrumbs (:step @panel-params)]
       [:div.wizard-body
        (case (:step @panel-params)
          :save [review-step-container]
          [upload-step])]])))

(defn main []
  (let [options (subscribe [::subs/stage-options])]
    (reagent/create-class
     {:component-did-mount
      (fn [e]
        (attach-body-events)
        (dispatch [::evts/reset]))
      :reagent-render
      (fn []
        [:div.container.idresolverupload
         [wizard]])})))
