(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [oops.core :refer [oget ocall oset!]]
            [goog.i18n.NumberFormat.Format]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [inflections.core :as inf]
            [bluegenes.sections.mymine.views.browser :as browser]
            [bluegenes.sections.mymine.views.modals :as modals]
            [bluegenes.sections.mymine.views.contextmenu :as m])

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

                      (println "TRAIL IS" trail)

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
                                                                      :top (oget evt :pageY)})))))})





;(defn dispatch-edit [location key value]
;  (dispatch [::evts/new-folder location key value]))



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
  (fn [{:keys [file-type trail index label open] :as item} {:keys [title type] :as details}]
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

(defn table-row [{:keys [id] :as me}]
  (let [over          (subscribe [::subs/dragging-over])
        selected      (subscribe [::subs/selected])
        checked       (subscribe [::subs/checked-ids])
        asset-details (subscribe [::subs/one-list id])]
    (fn [{:keys [editing? friendly-date-created level file-type type trail index read-only? size type id] :as row}]
      (let [selected? (some? (some #{trail} @selected))
            checked?  (some? (some #{id} @checked))]
        [:tr (-> {:class (clojure.string/join " " [
                                                   ;(when selected? (str "im-type box " type))
                                                   (when selected? (str "selected"))
                                                   (cond
                                                     (= trail @over) "draggingover"
                                                     :else nil)])}
                 (merge (click-events trail index row))
                 (cond-> (not read-only?) (merge (drag-events trail index row))))
         [:td.shrinky (when (not= file-type :folder)
                        [:input
                         {:type "checkbox"
                          :checked checked?
                          :on-change (fn [e]
                                       (dispatch [::evts/toggle-checked id]))}])]
         [:td {:style {:padding-left (str (* level 20) "px")}}
          (case file-type
            :folder [folder-cell row]
            :list [list-cell row @asset-details]
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

(defn count-nested-children [m]
  (count (mapcat (fn [y] (tree-seq (comp map? second) (comp :children second) y)) m)))

(defn clickable [{:keys [id file-type trail]}]
  {:on-click (fn [e] (js/console.log "clicked" id))})

(defn draggable [{:keys [id file-type trail whoami] :as file-details}]
  {:draggable true
   :on-drag-start (fn [evt]
                    (dispatch [::evts/drag-start file-details]))})

(defn menuable [file-details]
  {:on-context-menu (fn [evt]
                      ; Prevent the default right menu
                      (ocall evt :preventDefault)
                      ; Tell re-frame what we're right clicking on
                      (dispatch [::evts/set-menu-target file-details])
                      ; TODO
                      (dispatch [::evts/set-action-target (:trail file-details)])
                      ; Show the context menu
                      (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                  :left (oget evt :pageX)
                                                                  :top (oget evt :pageY)})))})


(defn droppable [trail]
  {:on-drag-end (fn [evt] (dispatch [::evts/drag-end trail]))
   :on-drag-over (fn [evt] (ocall evt :preventDefault) (dispatch [::evts/drag-over trail]))
   :on-drag-leave (fn [evt] (dispatch [::evts/drag-over nil]))
   :on-drop (fn [evt] (dispatch [::evts/drop trail]))})

(defn private-folder []
  (let [over  (subscribe [::subs/dragging-over])
        focus (subscribe [::subs/focus])]
    (fn [[key {:keys [label file-type children open index trail] :as row}]]
      (let [has-child-folders? (> (count (filter #(= :folder (:file-type (second %))) children)) 0)]
        [:li
         (merge
           {:on-click (fn [] (dispatch [::evts/set-focus trail]))
            :class (cond
                     (= trail @over) "draggingover"
                     (= trail @focus) "active"
                     :else nil)}
           (menuable row)
           (droppable trail))
         #_(merge {:on-click (fn [] (dispatch [::evts/set-focus trail]))
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
          [:span.count (count-nested-children children)]
          [:svg.icon.icon-caret-right]]]))))

(defn public-folder []
  (let [lists (subscribe [::subs/public-lists])
        focus (subscribe [::subs/focus])]
    (fn [[key {:keys [label file-type open index trail children]}]]
      [:li {:on-click (fn [x] (dispatch [::evts/set-focus [:public]]))
            :class (when (= [:public] @focus) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")}]
        [:svg.icon.icon-folder [:use {:xlinkHref "#icon-globe"}]]]
       [:div.label-name {:on-click (fn [] (dispatch [::evts/set-focus [:public]]))} "Public Items"]
       [:div.extra
        [:span.count (count @lists)]
        [:svg.icon.icon-caret-right]]])))

(defn unsorted-folder []
  (let [focus    (subscribe [::subs/focus])
        my-items (subscribe [::subs/my-items])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li {:on-click (fn [x] (dispatch [::evts/set-focus [:unsorted]]))
            :class (when (= [:unsorted] @focus) "active")}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")}]
        [:svg.icon.icon-drawer [:use {:xlinkHref "#icon-user"}]]]
       [:div.label-name
        #_{:on-click (fn [] (dispatch [::evts/set-focus [:public]]))}
        "My Items "]
       [:div.extra
        [:span.count (count @my-items)]
        [:svg.icon.icon-caret-right]]])))

(defn add-folder []
  (let [focus    (subscribe [::subs/focus])
        unfilled (subscribe [::subs/unfilled])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li {:data-toggle "modal"
            :data-keyboard true
            :data-target "#myMineNewFolderModal"}
       [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
        [:svg.icon.icon-caret-right
         {:class (when open "open")}]
        [:svg.icon.icon-drawer [:use {:xlinkHref "#icon-plus"}]]]
       [:div.label-name
        {:on-click (fn [] (dispatch [::evts/set-action-target []]))}
        "New Folder"]
       [:div.extra]])))


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
             [unsorted-folder]
             [public-folder]
             [:li.separator]
             [add-folder]
             [:li.separator]]
            (conj
              (mapv
                (fn [[k v]] ^{:key (str (:trail v))} [folder [k v]])
                @folders)

              ;[:li.separator]
              )))))


(defn details-list []
  (fn [{:keys [description tags authorized name type source size title status id timestamp dateCreated]}]
    [:div
     [:div [:h2 name]]
     [:h3 (str (nf size) " " (inf/plural type))]]))

(defn details []
  (let [selected         (subscribe [::subs/selected])
        selected-details (subscribe [::subs/selected-details])
        dets             (subscribe [::subs/details])]
    (fn []
      [:div.details.open
       [details-list @dets]])))



(defn list-operations []
  (fn []
    [:div
     [:div.btn-group

      [:button.btn.btn-raised.btn-primary
       {:disabled false
        :on-click (fn [] (dispatch [::evts/copy-n]))}
       [:div
        [:svg.icon.copy [:use {:xlinkHref "#copy"}]]
        [:div "Copy"]]]


      ]
     [:div.btn-group

      [:button.btn.btn-raised.btn-primary
       {:disabled false
        :data-toggle "modal"
        :data-keyboard true
        :data-target "#myMineLoModal"}
       ;:on-click (fn [] (dispatch [::evts/lo-combine]) )

       [:div
        [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
        [:div "Combine"]]]
      [:button.btn.btn-raised.btn-primary
       {:disabled false
        :data-toggle "modal"
        :data-keyboard true
        :data-target "#myMineLoIntersectModal"
        :on-click (fn [])}
       [:div
        [:svg.icon.icon-venn-intersection [:use {:xlinkHref "#icon-venn-intersection"}]]
        [:div "Intersect"]]]

      [:button.btn.btn-raised.btn-primary
       {:disabled true
        :on-click (fn [])}
       [:div
        [:svg.icon.icon-venn-difference [:use {:xlinkHref "#icon-venn-difference"}]]
        [:div "Subtract"]]]

      ]]))



(defn checked-card []
  (fn [{:keys [name id type] :as details}]
    ;(dispatch [::evts/toggle-checked id])
    [:div.mymine-card name [:span.pull-right [:span {:on-click (fn [] (dispatch [::evts/toggle-checked id]))} [:svg.icon.icon-close [:use {:xlinkHref "#icon-close"}]]]]]))

(defn checked-panel []
  (let [details (subscribe [::subs/checked-details])]
    (fn []
      [:div.details.open
       [list-operations]
       (if-not (empty? @details)
         (into [:div [:h3 "Selected Lists"]] (map (fn [i] [checked-card i]) @details))
         [:div [:h3 "Select one or more lists to perform an operation"]])])))

(defn breadcrumb []
  (let [bc    (subscribe [::subs/breadcrumb])
        focus (subscribe [::subs/focus])]
    (fn []
      [:h2
       (case @focus
         [:public] [:ol.breadcrumb [:li.active [:a "Public Items"]]]
         [:unsorted] [:ol.breadcrumb [:li.active [:a "My Items"]]]
         (into [:ol.breadcrumb]
               (map (fn [{:keys [trail label]}]
                      (let [focused? (= trail @focus)]
                        [:li {:class (when focused? "active")
                              :on-click (fn [] (dispatch [::evts/set-focus trail]))}
                         [:a label]]))
                    (filter #(= :folder (:file-type %)) @bc))))])))





(defn checkbox [id]
  (let [checked-ids (subscribe [::subs/checked-ids])]
    (fn [id]
      (let [checked? (some? (some #{id} @checked-ids))]
        [:input
         {:type "checkbox"
          :checked checked?
          :on-click (fn [e]
                      (ocall e :stopPropagation)
                      (dispatch [::evts/toggle-checked id]))}]))))

(defn ico []
  (fn [file-type]
    (case file-type
      :list [:svg.icon.icon-folder [:use {:xlinkHref "#icon-document-list"}]]
      :folder [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]
      [:svg.icon.icon-folder [:use {:xlinkHref "#icon-spyglass"}]])))

(defn row-folder []
  (fn [{:keys [file-type label id] :as file}]
    [:tr
     (merge {} (clickable file) (menuable file))
     [:td [checkbox id]]
     [:td [:div [ico file-type] label]]
     [:td] ; Type
     [:td] ; Size
     [:td]])) ; Last Modified


(def built-in-formatter (tf/formatter "MMM d, y"))

(defn row-list [{:keys [id trail file-type] :as file}]
  (let [details (subscribe [::subs/one-list id])
        source  (subscribe [:current-mine-name])]

    (fn []
      (let [{:keys [description authorized name type size timestamp] :as dets} @details]
        [:tr
         (merge {} (clickable file) (menuable file))
         [:td [checkbox id]]
         [:td (merge {} (draggable file))
          [:div [ico file-type]
           [:a {:on-click (fn [e]
                            (.stopPropagation e)
                            (dispatch [:lists/view-results (assoc dets :source @source)]))} name]]]
         [:td type]
         [:td size]
         [:td (tf/unparse built-in-formatter (c/from-long timestamp))]]))))

(defn row [{:keys [id file-type trail] :as item}]
  (fn []
    (case file-type
      :list [row-list item]
      :folder [row-folder item]
      [:div])))

(defn main []
  (let [as-list             (subscribe [::subs/as-list])
        sort-by             (subscribe [::subs/sort-by])
        context-menu-target (subscribe [::subs/context-menu-target])
        files               (subscribe [::subs/files])
        focus               (subscribe [::subs/focus])
        my-items            (subscribe [::subs/my-items])
        checked             (subscribe [::subs/checked-ids])
        my-files            (subscribe [::subs/visible-files])]

    (r/create-class
      {:component-did-mount attach-hide-context-menu
       :reagent-render
       (fn []

         (let [filtered-files (not-empty (filter (comp #{:list} :file-type) @my-files))]
           [:div.mymine.noselect
            [:div.file-browser [file-browser]]
            [:div.files

             [breadcrumb]
             (if filtered-files
               [:table.table.mymine-table
                [:thead
                 [:tr
                  [:th ""]
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
                #_(into [:tbody]
                        (map-indexed (fn [idx x]
                                       ^{:key (str (:trail x))} [table-row (assoc x :index idx)]) @files))

                (into [:tbody]
                      (map-indexed (fn [idx x]
                                     ^{:key (str (:id x))} [row (assoc x :index idx)]) filtered-files))]


               [:h4 "Empty Folder"])]
            ;[browser/main]

            (when true #_(not-empty @checked) [checked-panel])
            [modals/modal @context-menu-target]
            [modals/modal-copy @context-menu-target]
            [modals/modal-delete-item @context-menu-target]
            [modals/modal-new-folder @context-menu-target]
            [modals/modal-delete-folder @context-menu-target]
            [modals/modal-lo @context-menu-target]
            [modals/modal-lo-intersect @context-menu-target]
            [modals/modal-rename-list @context-menu-target]
            [m/context-menu-container @context-menu-target]]))})))

