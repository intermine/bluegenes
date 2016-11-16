(ns redgenes.components.idresolver.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [redgenes.components.idresolver.events]
            [redgenes.components.icons :as icons]
            [redgenes.components.idresolver.subs]))

;;; TODOS:

;We need to add "other" handling. It can be synonyms, but there may be (are?) other things an "other" result could mean.
;We need to add descriptions of what the states mean
;We need to handler more than X results :D right now 1000 results would ALL show on screen. Eep.

(defn ex []
  (let [active-mine (subscribe [:current-mine])
        mines (subscribe [:mines])
        example-text (:idresolver-example ((:id @active-mine) @mines))]
example-text))

(def separators (set ".,; "))

(defn splitter
  "Splits a string on any one of a set of strings."
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
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (if (some? @results) (dispatch [:idresolver/analyse])))}
        "View Results"]])))


;

(defn submit-input [input] (dispatch [:idresolver/resolve (splitter input)]))

(defn input-box []
  (reagent/create-class
  (let [val (reagent/atom nil)
        timer (reagent/atom nil)]
    {:reagent-render (fn []
      [:input#identifierinput.freeform
       {:type         "text"
        :placeholder  "Type or paste identifiers here..."
        :value        @val
        :on-key-press (fn [e]
                        (let [keycode (.-charCode e)
                              input   (.. e -target -value)]
                          (cond (= keycode 13)
                                (do
                                  (reset! val "")
                                  (submit-input input)))))
        :on-change    (fn [e]
                        (let [input (.. e -target -value)]
                          (if (has-separator? input)
                            (do
                              (reset! val "")
                              (submit-input input))
                            (do (reset! val input)))))}])
     :component-did-mount (fn [this] (.focus (reagent/dom-node this)))})))


(defn input-item-duplicate []
  "Input control. allows user to resolve when an ID has matched more than one object."
  (fn [[oid data]]
    [:span.dropdown
     [:span.dropdown-toggle
      {:type        "button"
       :data-toggle "dropdown"}
      (:input data)
      [:span.caret]]
     (into [:ul.dropdown-menu]
           (map (fn [result]
                  [:li
                   {:on-click (fn [e]
                                (.preventDefault e)
                                (dispatch [:idresolver/resolve-duplicate
                                           (:input data)
                                           result]))}
                   [:a (-> result :summary :symbol)]]) (:matches data)))]))

(defn input-item [{:keys [input] :as i}]
  "visually displays items that have been input and have been resolved as known or unknown IDs (or currently are resolving)"
  (let [result   (subscribe [:idresolver/results-item input])
        selected (subscribe [:idresolver/selected])]
    (reagent/create-class
      {:component-did-mount
       (fn [])
       :reagent-render
       (fn [i]
         (let [class (if (empty? @result)
                       "inactive"
                       (name (:status (second (first @result)))))
               class (if (some #{input} @selected) (str class " selected") class)]
           [:div.id-resolver-item-container
            {:class (if (some #{input} @selected) "selected")}
            [:div.id-resolver-item
             {:class    class
              :on-click (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (dispatch [:idresolver/toggle-selected input]))}
             (case (:status (second (first @result)))
               :MATCH [:i.fa.fa-check]
               :UNRESOLVED [:i.fa.fa-times]
               :DUPLICATE [:i.fa.fa-clone]
               :TYPE_CONVERTED [:i.fa.fa-random]
               :OTHER [:i.fa.fa-exclamation]
               [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw])
             [:span.pad-left-5

              (if (= :DUPLICATE (:status (second (first @result))))
                [input-item-duplicate (first @result)]
                (:input i))]]]))})))

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
        [:div.legend
         [:h3 "Legend & Stats:"]

         [:div [:h4.title
                        "Total Identifiers: " [:span.count (count @bank)]]]
         [:div.results
            [:div.MATCH
              [:span.type-head [:i.fa.fa-check.MATCH]
              [:span.title "Matches"]]
              [:span.count (count @matches)]]
            [:div.TYPE_CONVERTED
              [:span.type-head [:i.fa.fa-random.TYPE_CONVERTED]
              [:span.title "Converted"]]
              [:span.count (count @type-converted)]]
            [:div.DUPLICATE
              [:span.type-head  [:i.fa.fa-clone.DUPLICATE]
              [:span.title "Duplicates"]]
              [:span.count (count @duplicates)]]
            [:div.OTHER
              [:span.type-head [:i.fa.fa-exclamation.OTHER]
              [:span.title "Other"]]
              [:span.count (count @other)]]
            [:div.UNRESOLVED
              [:span.type-head [:i.fa.fa-times.UNRESOLVED]
              [:span.title "Not Found"]]
              [:span.count (count @no-matches)]]
        ]]

      #_[:div
         [:ul
          [:li (str "entered" (count @bank))]
          [:li (str "matches" (count @matches))]
          [:li (str "no matches" (count @no-matches))]
          [:li (str "duplicates" (count @duplicates))]]]

      )))

(defn debugger []
  (let [everything (subscribe [:idresolver/everything])]
    (fn []
      [:div (json-html/edn->hiccup @everything)])))

(defn spinner []
  (let [resolving? (subscribe [:idresolver/resolving?])]
    (fn []
      (if @resolving?
        [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
        [:i.fa.fa-check]))))

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

(defn help-panel []
  [:div.panel.panel-default
   [:div.panel-body [:h4 [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]] " Tips:"]
   [:ul
    [:li "Want to remove more than one item at a time? Try pressing"
   [:strong " Shift "] " or " [:strong "Ctrl"] " to select multiple identifiers at once."]
    [:li "When you're typing in identifiers, press "
     [:strong "space"] " or " [:strong "enter"] " to submit the form."]]]])

(defn main []
  (reagent/create-class
    {:component-did-mount
     attach-body-events
     :reagent-render
     (fn []
       [:div.container.idresolverupload
        [:div.headerwithguidance
         [:h1 "List Upload"]
         [:a.guidance {:on-click (fn [] (dispatch [:idresolver/resolve (splitter (ex))]))} "[Show me an example]"]
         [:div.tip]]
        [:div
         [input-div]
        ]
        [stats]
        [help-panel]
        ;[selected]
        ;[debugger]
        ])}))
