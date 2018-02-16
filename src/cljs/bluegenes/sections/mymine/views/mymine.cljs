(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [oops.core :refer [oget ocall oset!]]
            [goog.i18n.NumberFormat.Format]
            [bluegenes.sections.mymine.events :as evts]
            [bluegenes.sections.mymine.subs :as subs]
            [inflections.core :as inf]
            [bluegenes.sections.mymine.views.modals :as modals]
            [bluegenes.sections.mymine.views.contextmenu :as m])

  (:import
    (goog.i18n NumberFormat)
    (goog.i18n.NumberFormat Format)))

(defn stop-prop [evt] (ocall evt :stopPropagation))

(def nff (NumberFormat. Format/DECIMAL))
(defn- nf [num] (.format nff (str num)))

(defn hide-context-menu [evt]
  (ocall (js/$ "#contextMenu") :hide))
;(dispatch [::evts/set-context-menu-target])

(defn attach-hide-context-menu []
  (ocall (js/$ "body") :on "click" hide-context-menu))

(defn tag-drag-events [entry]
  {:draggable true
   :on-drag-enter (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging-over entry]))
   :on-drag-over (fn [evt] (ocall evt :preventDefault) (ocall evt :stopPropagation))
   ;:on-drag-leave (fn [evt] (dispatch [::evts/dragging-over nil]) )
   :on-drag-start (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging entry]))
   :on-drag-end (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging? false]))
   :on-drop (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dropping-on entry]))})

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

(defn trigger-context-menu [data]
  {:on-context-menu (fn [evt]
                      (when-not (oget evt :nativeEvent :ctrlKey)
                        (do
                          ; Prevent the browser from showing its context menu
                          (ocall evt :preventDefault)
                          ; Stop evt propogation
                          (ocall evt :stopPropagation)
                          ; Force this item to be selected
                          ;(dispatch [::evts/toggle-selected trail {:force? true} row])
                          ; Set this item as the target of the context menu
                          (dispatch [::evts/set-context-menu-target data])
                          (dispatch [::evts/set-action-target data])
                          ; Show the context menu
                          (ocall (js/$ "#contextMenu") :css (clj->js {:display "block"
                                                                      :left (oget evt :pageX)
                                                                      :top (oget evt :pageY)})))))})

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
            [:svg.icon.icon-price-tag [:use {:xlinkHref "#icon-price-tag"}]]
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
       [:div.mymine-tag
        [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
         [:svg.icon.icon-caret-right
          {:class (when open "open")}]
         [:svg.icon.icon-folder [:use {:xlinkHref "#icon-globe"}]]]
        [:div.label-name {:on-click (fn [] (dispatch [::evts/set-focus [:public]]))} "Public Items"]
        [:div.extra
         [:span.count (count @lists)]
         [:svg.icon.icon-caret-right]]]])))

(defn unsorted-folder []
  (let [focus     (subscribe [::subs/focus])
        my-items  (subscribe [::subs/my-items])
        cursor    (subscribe [::subs/cursor])
        all-items (subscribe [::subs/cursor-items-at nil])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li {:on-click (fn [x]
                        (dispatch [::evts/set-context-menu-target])
                        (dispatch [::evts/set-cursor nil]))}
       [:div.mymine-tag
        {:class (when (= nil @cursor) "active")}
        [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
         [:svg.icon.icon-caret-right
          {:class (when open "open")}]

         [:svg.icon.icon-user-circle [:use {:xlinkHref "#icon-user-circle"}]]]
        [:div.label-name
         #_{:on-click (fn [] (dispatch [::evts/set-focus [:public]]))}
         "All Items "]
        [:div.extra
         [:span.count (count @all-items)]
         [:svg.icon.icon-caret-right]]]])))

(defn add-folder []
  (let [focus    (subscribe [::subs/focus])
        unfilled (subscribe [::subs/unfilled])]
    (fn [[key {:keys [label file-type open index trail]}]]
      [:li {:data-toggle "modal"
            :data-keyboard true
            :data-target "#myMineNewFolderModal"}
       [:div.mymine-tag
        [:div.icon-container {:style {:padding-left (str (* index 13) "px")}}
         [:svg.icon.icon-caret-right
          {:class (when open "open")}]
         [:svg.icon.icon-drawer [:use {:xlinkHref "#icon-plus"}]]]
        [:div.label-name
         {:on-click (fn [] (dispatch [::evts/set-action-target []]))}
         "New Tag"]
        [:div.extra]]])))




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


(defn toggle-open [entry-id status evt]
  (ocall evt :stopPropagation)
  (dispatch [::evts/toggle-tag-open entry-id (not status)]))

(defn is-active? [entry-id context-menu-target-atom cursor-atom dragging-over-atom]
  (or
    (= entry-id (:entry-id @context-menu-target-atom))
    (= entry-id (:entry-id @cursor-atom))
    (= entry-id (:entry-id @dragging-over-atom))))

(defn show-caret? [sub-tags-atom]
  (if @sub-tags-atom 1 0))

(defn set-cursor-on-click [entity]
  {:on-click (fn [evt]
               (ocall evt :stopPropagation)
               (dispatch [::evts/set-context-menu-target])
               (dispatch [::evts/set-cursor entity]))})



(defn tag [{entry-id :entry-id}]
  (let [context-menu-target (subscribe [::subs/context-menu-target])
        cursor              (subscribe [::subs/cursor])
        sub-tags            (subscribe [::subs/sub-tags entry-id])
        sub-not-tags        (subscribe [::subs/sub-not-tags entry-id])
        dragging-over       (subscribe [::subs/dragging-over])
        hierarchy           (subscribe [::subs/hierarchy])]
    (fn [{:keys [im-obj-type label open parent-id tag-trail] :as entry}]
      [:li
       (merge {}
              (trigger-context-menu entry)
              (set-cursor-on-click entry)
              (tag-drag-events entry))
       [:div.mymine-tag
        {:class (when (is-active? entry-id context-menu-target cursor dragging-over) "active")}
        [:div.icon-container
         [:svg.icon.icon-caret-right
          {:class (when open "open")
           :on-click (partial toggle-open entry-id open)
           :style {:opacity (show-caret? sub-tags)}}
          [:use {:xlinkHref "#icon-caret-right"}]]
         [:svg.icon.icon-price-tag [:use {:xlinkHref "#icon-price-tag"}]]]
        [:div.label-name label]
        [:div.extra
         [:span.count (count @sub-not-tags)]
         [:svg.icon.icon-caret-right]]]
       (when open
         (into [:ul {:style {:padding-left "25px"}}]
               (map (fn [t]
                      ^{:key (str "tag-" (:entry-id t))}
                      [tag t]) (sort-by :label @sub-tags))))])))

(defn tag-drag-events2 [entry]
  {:draggable true
   :on-drag-enter (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging-over entry]))
   :on-drag-over (fn [evt] (ocall evt :preventDefault) (ocall evt :stopPropagation))
   ;:on-drag-leave (fn [evt] (dispatch [::evts/dragging-over nil]) )
   :on-drag-start (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging entry]))
   :on-drag-end (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dragging? false]))
   :on-drop (fn [evt] (ocall evt :stopPropagation) (dispatch [::evts/dropping-on entry]))})

(defn sep-drag-events []
  {:on-drag-enter (fn [evt]
                    (ocall evt :preventDefault)
                    (dispatch [::evts/dragging-over :bar]))
   :on-drag-leave (fn [evt]
                    (ocall evt :preventDefault)
                    (dispatch [::evts/dragging-over nil]))
   :on-drag-over (fn [evt]
                   (ocall evt :preventDefault))
   :on-drop (fn [evt]
              (dispatch [::evts/dropping-on :bar]))})


(defn tag-browser []
  (let [tags          (subscribe [::subs/sub-tags nil])
        dragging-over (subscribe [::subs/dragging-over])
        dragging?     (subscribe [::subs/dragging?])

        authed?       (subscribe [:bluegenes.subs.auth/authenticated?])]
    (fn []
      (into [:ul
             [unsorted-folder]
             [:li.separator]
             (when @authed? [add-folder])
             [:li.separator (merge {}
                                   (sep-drag-events)
                                   {:class (when (and @dragging? (= :bar @dragging-over)) "separator-highlighted")})]]
            (conj
              (if @authed?
                (mapv
                  (fn [t] ^{:key (str "tag-" (:entry-id t))} [tag t])
                  (sort-by :label @tags))
                [[:li.info [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]] "Want to save your lists permanently? Click \"Log In\" (top right corner) to get started."]]))))))

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
  (let [checked (subscribe [::subs/checked-ids])]
    (fn []
      (let [cant-perform?        (not (some? (not-empty @checked)))
            cant-operate?        (not (> (count @checked) 1))
            operation-properties (if (> (count @checked) 1)
                                   {:data-toggle "modal"
                                    :data-keyboard true
                                    :data-target "#myTestModal"}
                                   {})]

         [:nav.nav-list-operations
           [:ul
            [:li {:class (when cant-perform? "disabled")}
             [:a {:on-click (fn [] (when (not cant-perform?) (dispatch [::evts/copy-n])))}
              "Duplicate " [:svg.icon.icon-duplicate [:use {:xlinkHref "#icon-duplicate"}]]]]
            [:li {:class (when cant-perform? "disabled")}
             [:a {:on-click (fn [] (when (not cant-perform?)
                                     (dispatch [::evts/delete-lists])))}
               "Delete " [:svg.icon.icon-bin [:use {:xlinkHref "#icon-bin"}]]]]

            [:li {:class (when (empty? operation-properties) "disabled")}
             [:a (merge
                   operation-properties
                   {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :combine])))})
              "Combine " [:svg.icon.icon-venn-combine.venn
                                 [:use {:xlinkHref "#icon-venn-combine"}]]]]

            [:li {:class (when (empty? operation-properties) "disabled")}
             [:a (merge
                   operation-properties
                   {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :intersect])))})
               "Intersect " [:svg.icon.icon-venn-intersection.venn [:use {:xlinkHref "#icon-venn-intersection"}]]]]
            [:li {:class (when (empty? operation-properties) "disabled")}
             [:a (merge
                   operation-properties
                   {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :difference])))})
              "Difference " [:svg.icon.icon-venn-disjunction.venn
                                    [:use {:xlinkHref "#icon-venn-disjunction"}]]]]
            [:li {:class (when (empty? operation-properties) "disabled")}
             [:a (merge
                   operation-properties
                   {:on-click (fn [] (when (not cant-operate?) (dispatch [::evts/set-modal :subtract])))})
               "Subtract " [:svg.icon.icon-venn-difference.venn [:use {:xlinkHref "#icon-venn-difference"}]]]]

            #_[:li {}
               [:a {:on-click (fn [] (dispatch [::evts/fetch-tree]))}
                [:span "Fetch " [:svg.icon.icon-venn-difference [:use {:xlinkHref "#icon-venn-difference"}]]]]]]]))))




(defn checked-card []
  (fn [{:keys [name id type] :as details}]
    ;(dispatch [::evts/toggle-checked id])
    [:div.mymine-card name [:span.pull-right [:span {:on-click (fn [] (dispatch [::evts/toggle-checked id]))} [:svg.icon.icon-close [:use {:xlinkHref "#icon-close"}]]]]]))

(defn checked-panel []
  (let [details (subscribe [::subs/checked-details])]
    (fn []
      [:div.details.open
       (if-not (empty? @details)
         (into [:div [:h3 "Selected Lists"]] (map (fn [i] [checked-card i]) @details))
         [:div [:h3 "Select one or more lists to perform an operation"]])])))

(defn breadcrumb []
  (fn [labels]
    (if labels

       (into [:ol.breadcrumb]
             (map-indexed (fn [idx {:keys [label] :as entry}]
                            (let [focused? (= idx (dec (count labels)))]
                              [:li {:class (when focused? "active")
                                    :on-click (fn [] (dispatch [::evts/set-cursor entry]))}
                               [:h2 [:a label]]]))
                          labels))
       [:ol.breadcrumb [:li.active [:h2 [:a "All Items"]]]])))


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
      "list" [:svg.icon.icon-document-list {:style {:margin-left 0}}
              [:use {:xlinkHref "#icon-document-list"}]]
      "tag" [:svg.icon.icon-folder [:use {:xlinkHref "#icon-folder"}]]
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

(defn tag-container []
  (fn [tag-col]
    [:span.label.label-default
     {:style {:font-size "0.9em" :opacity 0.9}}
     (apply str (interpose " / " tag-col))]))

(defn row-list-table [{:keys [im-obj-id trail im-obj-type entry-id] :as file}]
  (let [details             (subscribe [::subs/one-list im-obj-id])
        source              (subscribe [:current-mine-name])
        hierarchy-trail     (subscribe [::subs/hierarchy-trail entry-id])
        context-menu-target (subscribe [::subs/context-menu-target])]
    (fn [{:keys []}]
      (let [{:keys [description authorized name type size timestamp] :as dets} @details]
        [:tr
         (merge {:class (when (= @context-menu-target file) "highlighted")}
                (tag-drag-events file)
                (trigger-context-menu file)
                {:on-click (fn [] (dispatch [::evts/set-context-menu-target file]))})
         [:td [checkbox im-obj-id]]
         [:td (merge {} (draggable file))
          [:div [ico im-obj-type]
           [:a {:on-click (fn [e]
                            (.stopPropagation e)
                            (dispatch [::evts/view-list-results (assoc dets :source @source)]))}
            name (when-not authorized
                   [:svg.icon.icon-lock [:use {:xlinkHref "#icon-lock"}]])]]]
         [:td  [tag-container @hierarchy-trail]]
         [:td type]
         [:td size]
         [:td #_(tf/unparse built-in-formatter (c/from-long timestamp))]]))))

(defn row-list [{:keys [im-obj-id trail im-obj-type entry-id] :as file}]
  (let [details             (subscribe [::subs/one-list im-obj-id])
        source              (subscribe [:current-mine-name])
        hierarchy-trail     (subscribe [::subs/hierarchy-trail entry-id])
        context-menu-target (subscribe [::subs/context-menu-target])]
    (fn [{:keys []}]
      (let [{:keys [description authorized name type size timestamp] :as dets} @details]
        [:div.grid.grid-middle
         (merge {:class (when (= @context-menu-target file) "highlighted")}
                (tag-drag-events file)
                (trigger-context-menu file)
                {:on-click (fn []
                             (dispatch [::evts/set-context-menu-target file]))})
         [:div.col-1.shrink [checkbox im-obj-id]]
         [:div.col-7 (merge {} (draggable file))
          [:div [ico im-obj-type]
           [:a {:on-click (fn [e]
                            (.stopPropagation e)
                            (dispatch [::evts/view-list-results (assoc dets :source @source)]))} name]
           (when-not authorized
             [:svg.icon.icon-globe {:style {:fill "#939393"}} [:use {:xlinkHref "#icon-globe"}]])
           ]]
         [:div.col-2 [tag-container @hierarchy-trail]]
         [:div.col-1 {:class (str "tag-type type-" type)} type]
         [:div.col-1.list-size size]
         ]))))

(defn row [{:keys [im-obj-type] :as item}]
  (fn []
    (case im-obj-type
      "list" [row-list item]
      "folder" [row-folder item]
      [:div])))

(defn selected-items []
  (fn []
    [:div.bottom]))
(defn main []
  (let [as-list                    (subscribe [::subs/as-list])
        sort-by                    (subscribe [::subs/sort-by])
        context-menu-target        (subscribe [::subs/context-menu-target])
        files                      (subscribe [::subs/files])
        focus                      (subscribe [::subs/focus])
        my-items                   (subscribe [::subs/my-items])
        checked                    (subscribe [::subs/checked-ids])
        my-files                   (subscribe [::subs/visible-files])
        modal-kw                   (subscribe [::subs/modal])
        entries                    (subscribe [::subs/entries])
        root-tags                  (subscribe [::subs/root-tags])
        cursor-items               (subscribe [::subs/cursor-items])
        unsorted-items             (subscribe [::subs/untagged-items])
        cursor-trail               (subscribe [::subs/cursor-trail "abc"])
        selected-items             (subscribe [::subs/selected-items])
        selected-items-not-in-view (subscribe [::subs/selected-items-not-in-view])
        show-selected-pane?        (subscribe [::subs/show-selected-pane?])]


    (r/create-class
      {:component-did-mount attach-hide-context-menu
       :reagent-render
       (fn []

         [:div.mymine.noselect
          ; TODO - remove tags
          #_[:div.file-browser [tag-browser]]
          [:div.files
          ;[:div.headerwithguidance [:h1 "My Data"]]
           [list-operations]
           (when @show-selected-pane?
             [:div.top.shrink
              (into [:div [:h3 "Selected items with other tags"]] (map-indexed (fn [idx x]
                                                                 ^{:key (str "selected" (or (:entry-id x) (str (:im-obj-type x) (:im-obj-id x))))} [row (assoc x :index idx)]) @selected-items-not-in-view))])
           [:div.bottom
            ; TODO - remove tags
            #_[breadcrumb @cursor-trail]
            (let [just-files (not-empty (filter (comp (partial not= "tag") :im-obj-type) @cursor-items))]
              (if just-files
                (into [:div] (map-indexed (fn [idx x]
                                            ^{:key (str (or (:entry-id x) (str (:im-obj-type x) (:im-obj-id x))))} [row (assoc x :index idx)]) @cursor-items))
                #_[:table.table.mymine-table
                   [:thead
                    [:tr
                     [:th ""]
                     [table-header {:label "Name"
                                    :key :label
                                    :type :alphanum
                                    :sort-by @sort-by}]
                     [:th ""]
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
                                        ^{:key (str (or (:entry-id x) (str (:im-obj-type x) (:im-obj-id x))))} [row (assoc x :index idx)]) @cursor-items))]


                [:h4 "Empty Folder"]))


            #_(let [just-files (not-empty (filter (comp (partial not= "tag") :im-obj-type) @cursor-items))]
                (if just-files
                  [:table.table.mymine-table
                   [:thead
                    [:tr
                     [:th ""]
                     [table-header {:label "Name"
                                    :key :label
                                    :type :alphanum
                                    :sort-by @sort-by}]
                     [:th ""]
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
                                        ^{:key (str (or (:entry-id x) (str (:im-obj-type x) (:im-obj-id x))))} [row (assoc x :index idx)]) @selected-items))]


                  [:h4 "Empty Folder"]))]]


          #_(when true #_(not-empty @checked)
              [modals/list-operations-commutative
               {:title "Combine Lists"
                :body "The new list will contain items from all selected lists"}])
          ;[checked-panel]




          [modals/modal-list-operations @modal-kw]

          [modals/modal @context-menu-target]
          [modals/modal-copy @context-menu-target]
          [modals/modal-delete-item @context-menu-target]
          [modals/modal-new-folder @context-menu-target]
          [modals/modal-delete-folder @context-menu-target]
          [modals/modal-lo @context-menu-target]
          [modals/modal-lo-intersect @context-menu-target]
          [modals/modal-rename-list @context-menu-target]
          [m/context-menu-container @context-menu-target]])})))
