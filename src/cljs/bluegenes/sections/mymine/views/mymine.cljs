(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [oops.core :refer [oget ocall oset!]]
            [goog.i18n.NumberFormat.Format]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [inflections.core :as inf]
            [bluegenes.sections.mymine.views.browser :as browser]
            [bluegenes.sections.mymine.views.modals :as modals]
            )
  (:import
    (goog.i18n NumberFormat)
    (goog.i18n.NumberFormat Format)))

(defn stop-prop [evt] (ocall evt :stopPropagation))

(def nff (NumberFormat. Format/DECIMAL))
(defn- nf [num] (.format nff (str num)))

(defn hide-context-menu [evt] (ocall (js/$ "#contextMenu") :hide))

(defn attach-hide-context-menu [] (ocall (js/$ "body") :on "click" hide-context-menu))

(defn drag-events [trail index whoami]
  {:draggable true
   :on-drag-start (fn [evt]
                    (dispatch [::evts/drag-start trail whoami]))})

(defn drop-events [trail index]
  {:on-drag-end (fn [evt]
                  (dispatch [::evts/drag-end trail]))
   :on-drag-over (fn [evt]
                   (ocall evt :preventDefault)
                   (dispatch [::evts/drag-over trail]))
   :on-drag-leave (fn [evt]
                    (dispatch [::evts/drag-over nil]))
   :on-drop (fn [evt]
              (dispatch [::evts/drop trail]))})

(defn click-events [trail index row]
  {:on-mouse-down (fn [evt]
                    (stop-prop evt)
                    (let [ctrl-key? (oget evt :nativeEvent :ctrlKey)]
                      (dispatch [::evts/toggle-selected trail {:force? true} row])
                      ; TODO re-enable multi selected

                      #_(if ctrl-key?
                          (dispatch [::evts/toggle-selected trail index])
                          (dispatch [::evts/toggle-selected trail index {:reset? true}]))))
   :on-context-menu (fn [evt]

                      (when-not (oget evt :nativeEvent :ctrlKey)


                        (do
                          ; Prevent the browser from showing its context menu
                          (ocall evt :preventDefault)
                          ; Force this item to be selected
                          (dispatch [::evts/toggle-selected trail {:force? true} row])
                          ; Set this item as the target of the context menu
                          (dispatch [::evts/set-context-menu-target trail row])
                          ; Show the context menu
                          (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                      :left (oget evt :pageX)
                                                                      :top (oget evt :pageY)})))))
   })




;(defn dispatch-edit [location key value]
;  (dispatch [::evts/new-folder location key value]))

(defmulti context-menu :file-type)

(defmethod context-menu :folder []
  (fn [{:keys [trail type]}]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineNewFolderModal"}
      [:a "New Folder"]]
     [:li.divider]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameModal"}
      [:a "Rename"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteFolderModal"}
      [:a "Remove"]]]))

(defmethod context-menu :list []
  (fn [target]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineCopyModal"}
      [:a "Copy"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteModal"}
      [:a "Delete"]]]))

(defmethod context-menu :default []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "Default"]]]))

(defn context-menu-container []
  (fn [{:keys [file-type label trail] :as target}]
    [:div#contextMenu.dropdown.clearfix ^{:key (str "context-menu" trail)} [context-menu target]]))

(defn folder-cell []
  (fn [{:keys [file-type trail index label open editing?] :as item}]
    [:div.mymine-row
     (merge {:on-double-click (fn [] (dispatch [::evts/set-focus trail true]))}
            (drop-events trail index))
     [:span.shrink
      [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]
      #_(if open
          [:svg.icon.icon-folder-open [:use {:xlinkHref "#icon-folder-open"}]]
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]])]
     [:span.grow
      [:a label]]]))

(defn list-cell []
  (fn [{:keys [file-type trail index label open type] :as item}]
    [:div.mymine-row
     [:svg.icon.icon-document-list.im-type
      {:class (str type)} [:use {:xlinkHref "#icon-document-list"}]]
     [:span.grow [:a label]]]))

(defn query-cell []
  (fn [{:keys [file-type trail index label open type] :as item}]
    [:div.mymine-row
     [:svg.icon.icon-document-list.im-type
      {:class (str type)} [:use {:xlinkHref "#icon-spyglass"}]]
     [:span.grow [:a label]]]))

(def clj-type type)

(defn table-row []
  (let [over     (subscribe [::subs/dragging-over])
        selected (subscribe [::subs/selected])]
    (fn [{:keys [editing? friendly-date-created level file-type type trail index read-only? size type] :as row}]
      (let [selected? (some? (some #{trail} @selected))]
        [:tr (-> {:class (clojure.string/join " " [
                                                   ;(when selected? (str "im-type box " type))
                                                   (when selected? (str "selected"))
                                                   (cond
                                                     (= trail @over) "draggingover"
                                                     :else nil)])}
                 (merge (click-events trail index row))
                 (cond-> (not read-only?) (merge (drag-events trail index row))))
         [:td {:style {:padding-left (str (* level 20) "px")}}
          (case file-type
            :folder [folder-cell row]
            :list [list-cell row]
            :query [query-cell row])]
         [:td type]
         [:td size]
         [:td friendly-date-created]]))))


(defn table-header []
  (fn [{:keys [label type key sort-by]}]
    (let [{:keys [asc?]} sort-by]
      [:th
       {:on-click (fn [] (dispatch [::evts/toggle-sort key type (not asc?)]))}
       label
       (when (= key (:key sort-by))
         (if asc?
           [:i.fa.fa-fw.fa-chevron-down]
           [:i.fa.fa-fw.fa-chevron-up]))])))

(defn private-folder []
  (let [over  (subscribe [::subs/dragging-over])
        focus (subscribe [::subs/focus])]
    (fn [[key {:keys [label file-type children open index trail] :as row}]]
      (let [has-child-folders? (> (count (filter #(= :folder (:file-type (second %))) children)) 0)]
        [:li
         (merge {:on-click (fn [] (dispatch [::evts/set-focus trail]))
                 :on-context-menu (fn [evt]

                                    (when-not (oget evt :nativeEvent :ctrlKey)


                                      (do
                                        ; Prevent the browser from showing its context menu
                                        (ocall evt :preventDefault)
                                        ; Force this item to be selected
                                        (dispatch [::evts/toggle-selected trail {:force? true}])
                                        ; Set this item as the target of the context menu
                                        (dispatch [::evts/set-context-menu-target trail row])
                                        ; Show the context menu
                                        (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                                    :left (oget evt :pageX)
                                                                                    :top (oget evt :pageY)})))))
                 :class (cond
                          (= trail @over) "draggingover"
                          (= trail @focus) "active"
                          :else nil)}
                (drop-events trail 0))
         [:div.icon-container {:style {:padding-left (str (* (dec index) 26) "px")}}
          [:svg.icon.icon-caret-right
           {:class (when open "open")
            :on-click (fn [x]
                        (ocall x :stopPropagation)
                        (dispatch [::evts/toggle-folder-open trail]))}
           (when has-child-folders? [:use {:xlinkHref "#icon-caret-right"}])]
          (case key
            :root [:svg.icon.icon-folder [:use {:xlinkHref "#icon-intermine"}]]
            [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]
            #_(if open
                [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder-open"}]]
                [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]))]
         [:div.label-name label]
         [:div.extra
          [:span.label (count children)]
          [:svg.icon.icon-caret-right]]]))))

(defn public-folder []
  (let [lists (subscribe [::subs/public-lists])
        focus (subscribe [::subs/focus])]
    (fn [[key {:keys [label file-type open index trail children]}]]
      [:li {:on-click (fn [x] (dispatch [::evts/set-focus [:public]]))
            :class (when (= :public @focus) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")}]
        [:svg.icon.icon-folder [:use {:xlinkHref "#icon-globe"}]]]
       [:div.label-name {:on-click (fn [] (dispatch [::evts/set-focus :public]))} "Public"]
       [:div.extra
        [:span.label (count @lists)]
        [:svg.icon.icon-caret-right]]])))

(defn unsorted-folder []
  (let [focus    (subscribe [::subs/focus])
        unfilled (subscribe [::subs/unfilled])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li {:on-click (fn [x] (dispatch [::evts/set-focus [:unsorted]]))
            :class (when (= [:unsorted] @focus) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")}]
        [:svg.icon.icon-drawer [:use {:xlinkHref "#icon-drawer"}]]]
       [:div.label-name
        {:on-click (fn [] (dispatch [::evts/set-focus :public]))}
        "Unsorted "]
       (when (> (count @unfilled) 0) [:div.extra
                                      [:span.label (count @unfilled)]
                                      [:svg.icon.icon-caret-right
                                       {:class (when open "open")}]])])))

(defn root-folder []
  (let [over  (subscribe [::subs/dragging-over])
        focus (subscribe [::subs/focus])]
    (fn [[key {:keys [label file-type children open index trail] :as row}]]
      (let [has-child-folders? (> (count (filter #(= :folder (:file-type (second %))) children)) 0)]
        [:li
         (merge {:on-click (fn [] (dispatch [::evts/set-focus trail]))
                 :on-context-menu (fn [evt]

                                    (when-not (oget evt :nativeEvent :ctrlKey)

                                      (do
                                        ; Prevent the browser from showing its context menu
                                        (ocall evt :preventDefault)
                                        ; Force this item to be selected
                                        (dispatch [::evts/toggle-selected trail {:force? true}])
                                        ; Set this item as the target of the context menu
                                        (dispatch [::evts/set-context-menu-target trail row])
                                        ; Show the context menu
                                        (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                                    :left (oget evt :pageX)
                                                                                    :top (oget evt :pageY)})))))
                 :class (cond
                          (= [:root] @over) "draggingover"
                          (= [:root] @focus) "active"
                          :else nil)}
                (drop-events trail 0))
         [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
          [:svg.icon.icon-caret-right
           {:class (when open "open")
            :on-click (fn [x] (ocall x :stopPropagation) (dispatch [::evts/toggle-folder-open trail]))}
           (when has-child-folders? [:use {:xlinkHref "#icon-caret-right"}])]
          [:svg.icon.icon-folder [:use {:xlinkHref "#icon-intermine"}]]]
         [:div.label-name label]]))))

(defn folder []
  (fn [[key properties :as f]]
    (case key
      :root [root-folder f]
      [private-folder f])))

(defn file-browser []
  (let [files   (subscribe [::subs/with-public])
        folders (subscribe [::subs/folders])]
    (fn []
      (into [:ul
             [public-folder]
             [unsorted-folder]
             [:li.separator]]
            (map
              (fn [[k v]] ^{:key (str (:trail v))} [folder [k v]])
              @folders)))))

(defn details-list []
  (fn [{:keys [description tags authorized name type source size title status id timestamp dateCreated]}]
    [:div
     [:div [:h2 name]]
     [:h3 (str (nf size) " " (inf/plural type))]]))

(defn details []
  (let [selected         (subscribe [::subs/selected])
        selected-details (subscribe [::subs/selected-details])
        dets (subscribe [::subs/details])]
    (fn []
      [:div.details.open
       [details-list @dets]])))

(defn breadcrumb []
  (let [bc    (subscribe [::subs/breadcrumb])
        focus (subscribe [::subs/focus])]
    (fn []
      [:h4
       (case @focus
         [:public] [:ol.breadcrumb [:li.active [:a "Public"]]]
         [:unsorted] [:ol.breadcrumb [:li.active [:a "Unsorted"]]]
         (into [:ol.breadcrumb]
               (map (fn [{:keys [trail label]}]
                      (let [focused? (= trail @focus)]
                        [:li {:class (when focused? "active")
                              :on-click (fn [] (dispatch [::evts/set-focus trail]))}
                         [:a label]]))
                    (filter #(= :folder (:file-type %)) @bc))))])))

(defn main []
  (let [as-list             (subscribe [::subs/as-list])
        sort-by             (subscribe [::subs/sort-by])
        context-menu-target (subscribe [::subs/context-menu-target])
        files               (subscribe [::subs/files])
        focus               (subscribe [::subs/focus])]
    (r/create-class
      {:component-did-mount attach-hide-context-menu
       :reagent-render
       (fn []
         [:div.mymine.noselect
          [:div.file-browser [file-browser]]
          [:div.files
           [browser/main]
           [breadcrumb]
           (if (not-empty @files)
             [:table.table.mymine-table
              [:thead
               [:tr
                [table-header {:label "Name"
                               :key :label
                               :type :alphanum
                               :sort-by @sort-by}]
                [table-header {:label "Type"
                               :key :type
                               :type :alphanum
                               :sort-by @sort-by}]
                [table-header {:label "Size"
                               :key :size
                               :type :alphanum
                               :sort-by @sort-by}]
                [table-header {:label "Last Modified"
                               :key :dateCreated
                               :type :date
                               :sort-by @sort-by}]]]
              (into [:tbody]
                    (map-indexed (fn [idx x]
                                   ^{:key (str (:trail x))} [table-row (assoc x :index idx)]) @files))]
             [:h1 "Empty Folder"])]
          [details]
          [modals/modal @context-menu-target]
          [modals/modal-copy @context-menu-target]
          [modals/modal-delete-item @context-menu-target]
          [modals/modal-new-folder @context-menu-target]
          [modals/modal-delete-folder @context-menu-target]

          [context-menu-container @context-menu-target]
          ])})))
