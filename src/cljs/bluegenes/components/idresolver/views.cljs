(ns bluegenes.components.idresolver.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.components.idresolver.events]
            [bluegenes.components.icons :as icons]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.idresolver.subs]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.lighttable :as lighttable]))

;;; TODOS:

;We need to handler more than X results :D right now 1000 results would ALL show on screen. Eep.

(def separators (set ",; "))

(def timeout 1500)

(defn organism-selection
  "UI component allowing user to choose which organisms to search. Defaults to all."
  []
  (let [selected-organism (subscribe [:idresolver/selected-organism])]
    [:div [:label "Organism"]
      [im-controls/organism-dropdown
      {:selected-value (if (some? @selected-organism) @selected-organism "Any")
       :on-change (fn [organism]
                    (dispatch [:idresolver/set-selected-organism organism]))}]]))

(defn object-type-selection
  "UI component allowing user to choose which object type to search. Defaults to the first one configured for a mine."
  []
  (let [selected-object-type (subscribe [:idresolver/selected-object-type])
        values (subscribe [:idresolver/object-types])]
    [:div [:label "Type"]
      [im-controls/object-type-dropdown
      {:values @values
       :selected-value @selected-object-type
       :on-change (fn [object-type]
                    (dispatch [:idresolver/set-selected-object-type object-type]))}]]))

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
  (let [results  (subscribe [:idresolver/results])
        matches  (subscribe [:idresolver/results-matches])
        selected (subscribe [:idresolver/selected])]
    (fn []
      [:div.btn-toolbar.controls
       [:button.btn.btn-warning
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (dispatch [:idresolver/clear]))}
        "Clear all"]
       [:button.btn.btn-warning
        {:class    (if (empty? @selected) "disabled")
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
    (dispatch [:idresolver/resolve (splitter input)]))
)

(defn input-box []
  (reagent/create-class
  (let [val (reagent/atom nil)
        timer (reagent/atom nil)]
    {:reagent-render (fn []
      [:input#identifierinput.freeform
       {:type         "text"
        :placeholder  "Type or paste identifiers here..."
        :value        @val
        :on-key-press
          (fn [e]
            (let [keycode (.-charCode e)
                  input   (.. e -target -value)]
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
     :component-did-mount (fn [this] (.focus (reagent/dom-node this)))})))

(defn organism-identifier
  "Sometimes the ambiguity we're resolving with duplicate ids is the sme symbol from two similar organisms, so we'll need to ass organism name where known."
  [summary]
  (if (:organism.name summary)
    (str " (" (:organism.name summary) ")")
    ""
    )
)

(defn build-duplicate-identifier
  "Different objects types have different summary fields. Try to select one intelligently or fall back to primary identifier if the others ain't there."
  [result]
  (let [summary (:summary result)
        symbol (:symbol summary)
        accession (:primaryAccession summary)
        primaryId (:primaryId summary)]
    (str
     (first (remove nil? [symbol accession primaryId]))
     (organism-identifier summary)
)))

(defn input-item-duplicate []
  "Input control. allows user to resolve when an ID has matched more than one object."
  (fn [[oid data]]
    [:span.id-item [:span.dropdown
     [:span.dropdown-toggle
      {:type        "button"
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
    :MATCH [:i.fa.fa-check.MATCH]
    :UNRESOLVED [:svg.icon.icon-sad.UNRESOLVED [:use {:xlinkHref "#icon-sad"}]]
    :DUPLICATE [:i.fa.fa-clone.DUPLICATE]
    :TYPE_CONVERTED [:i.fa.fa-random.TYPE_CONVERTED]
    :OTHER [:svg.icon.icon-arrow-right.OTHER [:use {:xlinkHref "#icon-arrow-right"}]]
    [mini-loader "tiny"]))

(def reasons
  {:TYPE_CONVERTED "we're searching for genes and you input a protein (or vice versa)."
   :OTHER " the synonym you input is out of date."})

(defn input-item-converted [original results]
  (let [new-primary-id (get-in results [:matches 0 :summary :primaryIdentifier])
        conversion-reason ((:status results) reasons)]
    [:span.id-item {:title (str "You input '" original "', but we converted it to '" new-primary-id "', because " conversion-reason)}
     original " -> " new-primary-id]
))

(defn input-item [{:keys [input] :as i}]
  "visually displays items that have been input and have been resolved as known or unknown IDs (or currently are resolving)"
  (let [result   (subscribe [:idresolver/results-item input])
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
             {:class    class
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
               [:span.id-item (:input i)])
              ]]))})))

(defn input-items []
  (let [bank (subscribe [:idresolver/bank])]
    (fn []
      (into [:div.input-items]
        (map (fn [i]
           ^{:key (:input i)} [input-item i]) (reverse @bank))))))

(defn parse-files [files]
  (dotimes [i (.-length files)]
    (let [rdr      (js/FileReader.)
          the-file (aget files i)]
      (set! (.-onload rdr)
            (fn [e]
              (let [file-content (.-result (.-target e))
                    file-name    (if (= ";;; " (.substr file-content 0 4))
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

(defn drag-and-drop-prompt []
  [:div.upload-file
   [:svg.icon.icon-file [:use {:xlinkHref "#icon-file"}]]
    [:p "All your identifiers in a text file? Try dragging and dropping it here, or "
      [:label.browseforfile {:on-click (fn [e] (.stopPropagation e))};;otherwise it focuses on the typeable input
       [:input
        {:type "file"
         :multiple true
         :on-click (fn [e] (.stopPropagation e)) ;;otherwise we just end up focusing on the input on the left/top.
         :on-change (fn [e] (parse-files (.-files (.-target e)))
                      )}]
       ;;this input isn't visible, but don't worry, clicking on the label is still accessible. Even the MDN says it's ok. True story.
       "browse for a file"]]])

(defn input-div []
  (let [drag-state (reagent/atom false)]
    (fn []
      [:div.resolvey
       [:div#dropzone1
       {
        :on-drop       (partial handle-drop-over drag-state)
        :on-click      (fn [evt]
                         (.preventDefault evt)
                         (.stopPropagation evt)
                         (dispatch [:idresolver/clear-selected])
                         (.focus (sel1 :#identifierinput)))
        :on-drag-over  (partial handle-drag-over drag-state)
        :on-drag-leave (fn [] (reset! drag-state false))
        :on-drag-end   (fn [] (reset! drag-state false))
        :on-drag-exit  (fn [] (reset! drag-state false))}
       [:div.eenput
        {:class (if @drag-state "dragging")}
        [:div.idresolver
          [:div.type-and-organism
           [organism-selection]
           [object-type-selection]]
          [input-items]
          [input-box]
         [controls]]
        [drag-and-drop-prompt]
        ]]
       ])))

(defn stats []
  (let [bank       (subscribe [:idresolver/bank])
        no-matches (subscribe [:idresolver/results-no-matches])
        matches    (subscribe [:idresolver/results-matches])
        type-converted (subscribe [:idresolver/results-type-converted])
        duplicates (subscribe [:idresolver/results-duplicates])
        other      (subscribe [:idresolver/results-other])]
    (fn []
        ;;goodness gracious this could use a refactor
        [:div.legend
         [:h3 "Legend & Stats:"]
         [:div.results
            [:div.MATCH {:tab-index -5}
              [:div.type-head [get-icon :MATCH]
                [:span.title "Matches"]
                [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]
              [:div.details [:span.count (count @matches)]
                [:p "The input you entered was successfully matched to a known ID"]
               ]
             ]
            [:div.TYPE_CONVERTED {:tab-index -4}
              [:div.type-head [get-icon :TYPE_CONVERTED]
                [:span.title "Converted"]
                [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]
              [:div.details [:span.count (count @type-converted)]
                 [:p "Input protein IDs resolved to gene (or vice versa)"]]
             ]
           [:div.OTHER {:tab-index -2}
             [:div.type-head [get-icon :OTHER]
             [:span.title "Synonyms"]
             [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]
             [:div.details [:span.count (count @other)]
             [:p "The ID you input matches an old synonym of an ID. We've used the most up-to-date one instead."]]]
          [:div.DUPLICATE {:tab-index -3}
              [:div.type-head  [get-icon :DUPLICATE]
                [:span.title "Partial\u00A0Match"]
                [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]
              [:div.details [:span.count (count @duplicates)]
                [:p "The ID you input matched more than one item. Click on the down arrow beside IDs with this icon to fix this."]]]
            [:div.UNRESOLVED {:tab-index -1}
              [:div.type-head [get-icon :UNRESOLVED]
                [:span.title "Not\u00A0Found"]
                [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]
              [:div.details [:span.count (count @no-matches)]
              [:p "The ID provided isn't one that's known for your chosen organism"]]]
        ]]

      )))

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
            "View all >>"])
       ]]
       [preview-table
         :loading? @fetching-preview?
         :query-results @results-preview]]
))

(defn main []
  (reagent/create-class
    {:component-did-mount attach-body-events
     :reagent-render
       (fn []
         (let [bank       (subscribe [:idresolver/bank])
               no-matches (subscribe [:idresolver/results-no-matches])
               result-count (- (count @bank) (count @no-matches))]
         [:div.container.idresolverupload
          [:div.headerwithguidance
           [:h1 "List Upload"]
           [:a.guidance
            {:on-click
             (fn []
              (dispatch [:idresolver/example splitter]))} "[Show me an example]"]]
           [input-div]
           [stats]
           (cond (> result-count 0) [preview result-count])
        ;[selected]
        ;[debugger]
        ]))}))
