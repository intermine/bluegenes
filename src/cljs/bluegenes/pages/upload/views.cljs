(ns bluegenes.pages.upload.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [bluegenes.components.idresolver.subs :as subs]
            [bluegenes.components.idresolver.events :as evts]
            [bluegenes.components.idresolver.views :as idresolver]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.route :as route]
            [dommy.core :as dommy :refer-macros [sel1]]
            [oops.core :refer [oget oget+ ocall]]
            [clojure.string :as str]))

(def timeout 1500)
(def separators (set ",; "))

(defn has-separator?
  "Returns true if a string contains any one of a set of strings."
  [str]
  (some? (some separators str)))

(defn splitter
  "Splits a string on any one of a set of strings."
  [string]
  (->> (str/split string (re-pattern (str "[" (reduce str separators) "\\r?\\n]")))
       (remove nil?)
       (remove #(= "" %))))

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

(defn guidance
  "list upload guidance text. no functional / interactive bits."
  []
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
                :placeholder "Type identifiers here, or click [SHOW EXAMPLE] below."
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
                 "Show example"]
                (.debug js/console
                        "No example button available due to missing or misconfigured example in the InterMine properties")))
            [:button.btn.btn-default.btn-raised
             {:on-click (fn [] (dispatch [::evts/reset]))}
             "Reset"]
            [:button.btn.btn-primary.btn-raised
             {:on-click (fn [] (dispatch [::evts/parse-staged-files @files @textbox-identifiers @options]))
              :disabled (or (and (nil? @files) (nil? @textbox-identifiers))
                            (nil? type))}
             "Continue"]]]]]))))

(defn review-step []
  (let [resolution-response (subscribe [::subs/resolution-response])
        resolution-error (subscribe [::subs/resolution-error])
        in-progress? (subscribe [::subs/in-progress?])]
    (fn []
      (cond
        @resolution-error [:div.guidance-and-title
                           [:h2 "Error occurred when resolving identifiers"]
                           [:code (if-let [err (not-empty (get-in @resolution-error [:body :error]))]
                                    err
                                    "Please check your connection and try again later.")]
                           [:hr]
                           [:button.btn.btn-primary.btn-raised
                            {:on-click (fn [] (dispatch [::evts/reset nil true]))}
                            "Reset"]]

        (some? @resolution-response) [idresolver/main]

        @in-progress? [:div.wizard-loader [loader "IDENTIFIERS"]]))))

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
  (let [panel-params (subscribe [:panel-params])]
    (fn []
      [:div.wizard
       [breadcrumbs (:step @panel-params)]
       [:div.wizard-body
        (case (:step @panel-params)
          :save [review-step]
          [upload-step])]])))

(defn main []
  (attach-body-events)
  (fn []
    [:div.container.idresolverupload
     [wizard]]))
