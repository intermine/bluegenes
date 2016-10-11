(ns redgenes.components.idresolver.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [redgenes.components.idresolver.events]
            [redgenes.components.idresolver.subs]))

(def ex "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt")

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
      [:div.btn-toolbar
       [:button.btn.btn-warning.btn-raised
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (dispatch [:idresolver/clear]))}
        "Clear"]
       [:button.btn.btn-warning.btn-raised
        {:class    (if (empty? @selected) "disabled")
         :on-click (fn [] (dispatch [:idresolver/delete-selected]))}
        "Remove"]
       [:button.btn.btn-success.btn-raised
        {:class    (if (empty? @matches) "disabled")
         :on-click (fn [] (dispatch [:idresolver/save-results]))}
        "Quick Save"]
       [:button.btn.btn-primary.btn-raised
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (if (some? @results) (dispatch [:idresolver/analyse])))}
        "View Results"]])))


;

(defn submit-input [input] (dispatch [:idresolver/resolve (splitter input)]))

(defn input-box []
  (let [val (reagent/atom nil)]
    (fn []
      [:input#identifierinput.freeform
       {:type         "text"
        :placeholder  "Type identifiers here..."
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
                              (submit-input input)) (reset! val input))))}])))


(defn input-item-duplicate []
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
           [:div.id-resolver-item
            {:class class
             :on-click (fn [e]
                         (.preventDefault e)
                         (.stopPropagation e)
                         (dispatch [:idresolver/toggle-selected input]) )}
            (case (:status (second (first @result)))
              :MATCH [:i.fa.fa-check.fa-1x.fa-fw]
              :UNRESOLVED [:i.fa.fa-times]
              :DUPLICATE [:i.fa.fa-clone]
              :TYPE_CONVERTED [:i.fa.fa-random]
              :OTHER [:i.fa.fa-exclamation]
              [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw])
            [:span.pad-left-5

             (if (= :DUPLICATE (:status (second (first @result))))
               [input-item-duplicate (first @result)]
               (:input i))]]))})))

(defn input-items []
  (let [bank (subscribe [:idresolver/bank])]
    (fn []
      (into [:div.input-items]
            (map (fn [i]
                   ^{:key (:input i)} [input-item i]) (reverse @bank))))))

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
        (.readAsText rdr the-file)))))

(defn input-div []
  (let [drag-state (reagent/atom false)]
    (fn []
      [:div#dropzone1.panel.panel-default
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
       [:div.panel-body.transitions
        {:class (if @drag-state "dragging")}
        [:div.idresolver.form-control
         [input-items]
         [input-box]
         ]]])))

(defn stats []
  (let [bank       (subscribe [:idresolver/bank])
        no-matches (subscribe [:idresolver/results-no-matches])
        matches    (subscribe [:idresolver/results-matches])
        duplicates (subscribe [:idresolver/results-duplicates])
        other      (subscribe [:idresolver/results-other])]
    (fn []
      [:div.panel.panel-default
       [:div.panel-body
        [:div.row.legend
         [:div.col-md-4 [:h4.title
                         (str "Total Identifiers: " (count @bank))]]
         [:div.col-md-2 [:h4.MATCH
                         [:i.fa.fa-check.fa-1x.fa-fw.MATCH]
                         (str "Matches: " (count @matches))]]
         [:div.col-md-2 [:h4.DUPLICATE
                         [:i.fa.fa-clone.DUPLICATE]
                         (str "Duplicates: " (count @duplicates))]]
         [:div.col-md-2 [:h4.UNRESOLVED
                         [:i.fa.fa-times.UNRESOLVED]
                         (str "Not Found: " (count @no-matches))]]
         [:div.col-md-2 [:h4.OTHER
                         [:i.fa.fa-exclamation.OTHER]
                         (str "Other: " (count @other))]]]
        [:div [controls]]]]

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

(defn dropzone []
  (fn []
    [:div#dropzone1.dropzone [:h1 "Drop Here"]]))

(defn main []
  (reagent/create-class
    {:component-did-mount
     attach-body-events
     :reagent-render
     (fn []
       [:div.container
        [:div.headerwithguidance
         [:h1 "List Upload"]
         [:a.guidance {:on-click (fn [] (dispatch [:idresolver/resolve (splitter ex)]))} "[Show me an example]"]
         [:div.tip [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
          "Tip: Press enter or space bar to submit the form"]]
        ;[dropzone]
        [input-div]
        [stats]
        ;[selected]
        ;[debugger]
        ])}))
