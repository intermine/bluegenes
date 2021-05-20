(ns bluegenes.pages.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [clojure.string :as string :refer [split join lower-case]]
            [oops.core :refer [ocall oget]]
            [bluegenes.components.ui.constraint :refer [constraint]]
            [bluegenes.components.bootstrap :refer [tooltip]]
            [imcljs.path :as im-path]
            [imcljs.query :refer [->xml]]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [inflections.core :refer [ordinalize plural]]
            [bluegenes.components.icons :refer [icon]]))

(defn query=
  "Returns whether `queries` are all =, ignoring empty values like [] and nil."
  [& queries]
  (apply =
         (map #(into {} (remove (comp empty? val)) %)
              queries)))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(def auto (reagent/atom true))

(def aquery {:from "Gene"
             :constraintLogic "A or B"
             :select ["symbol"
                      "organism.name"
                      "alleles.symbol"
                      "alleles.phenotypeAnnotations.annotationType"
                      "alleles.phenotypeAnnotations.description"]
             :where [{:path "Gene.symbol"
                      :op "="
                      :code "A"
                      :value "zen"}
                     {:path "Gene.symbol"
                      :op "="
                      :code "B"
                      :value "mad"}]})

(defn sort-classes
  [classes]
  (sort-by (comp string/lower-case :displayName val) compare classes))

(defn filter-preferred
  [classes]
  (filter #(contains? (-> % val :tags set) "im:preferredBagType")
          classes))

(defn root-class-dropdown []
  (let [model @(subscribe [:model])
        root-class @(subscribe [:qb/root-class])
        classes (sort-classes model)
        preferred (filter-preferred classes)]
    (into [:select.form-control
           {:on-change (fn [e] (dispatch [:qb/set-root-class (oget e :target :value)]))
            :value root-class}
           (when (nil? root-class)
             [:<>
              [:option {:value nil} "Select to start query"]
              [:option {:disabled true :role "separator"} "─────────────────────────"]])]
          (map (fn [[class-kw details :as item]]
                 (if (map-entry? item)
                   [:option {:value class-kw} (:displayName details)]
                   [:option {:disabled true :role "separator"} "─────────────────────────"]))
               (concat preferred [[:separator]] classes)))))

(defn dotsplit [string] (split string "."))

(defn im-query->map [{:keys [select]}]
  (reduce (fn [total next] (assoc-in total next {})) {} (map dotsplit select)))

(defn attributes-first [[_ v]] (empty? v))

(defn has-children? [[_ v]] (some? (not-empty v)))

(defn within? [haystack needle] (some? (some #{needle} haystack)))

(defn not-selected [selected [k attributes-map]]
  (not (within? selected (name k))))

(defn display-name [model class]
  (let [class-kw (cond-> class
                   (string? class) keyword)]
    (get-in model [:classes class-kw :displayName])))

;; I think we can replace this function with `im-path/walk`.
(defn rel-prop-fn [prop]
  (fn [model path]
    (let [class (->> (drop-last 1 path)
                     (im-path/walk model)
                     (last))
          ;; `path` can point to either an attribute or a class.
          attrib+class (last path)
          ;; We don't use `im-path/properties` as we need to support subclasses.
          properties (apply merge (map class [:attributes :references :collections]))]
      (get-in properties [attrib+class prop]))))

(def rel-display-name (rel-prop-fn :displayName))
(def rel-referenced-type (rel-prop-fn :referencedType))

(def q {:from "Gene"
        :select ["Gene.symbol"
                 "Gene.secondaryIdentifier"
                 "Gene.organism.name"]})

(defn attribute []
  (let [enhance-query (subscribe [:qb/enhance-query])]
    (fn [model [k properties] & [trail sub]]
      (let [path (conj trail (name k))
            selected? (get-in @enhance-query path)]
        [:li.haschildren
         [:span
          {:on-click (fn []
                       (if selected?
                         (dispatch [:qb/enhance-query-remove-view path])
                         (dispatch [:qb/enhance-query-add-view path])))}
          (if (get-in @enhance-query path)
            [:svg.icon.icon-checkbox-checked [:use {:xlinkHref "#icon-checkbox-checked"}]]
            [:svg.icon.icon-checkbox-unchecked [:use {:xlinkHref "#icon-checkbox-unchecked"}]])
          [:span.qb-label (:displayName properties)]]]))))

(defn node []
  (let [menu (subscribe [:qb/menu])
        hier (subscribe [:current-model-hier])]
    (fn [model [k properties] & [trail]]
      (let [path (vec (conj trail (name k)))
            open? (get-in @menu path)
            str-path (join "." path)
            sub (get-in @menu (conj path :subclass))]
        [:li.haschildren.qb-group
         {:class (cond open? "expanded-group")}
         [:div.group-title
          {:on-click (fn []
                       (if open?
                         (dispatch [:qb/collapse-path path])
                         (dispatch [:qb/expand-path path])))}
          [icon (if open? "minus" "plus")]
          [:span.qb-class (:displayName properties)]
          [:span.label-button
           {:on-click (fn [e]
                        (ocall e :stopPropagation)
                        (dispatch [:qb/enhance-query-add-summary-views path sub]))}
           "Summary"]]
         (when open?
           (into [:ul]
                 (concat
                   ;; We remove :type-constraints from the model in these two instances
                   ;; as we want to find the subclasses of the superclass. Otherwise the
                   ;; list of subclasses would grow smaller when one is selected.
                  (when (im-path/class? (dissoc model :type-constraints) str-path)
                    (let [class (im-path/class (dissoc model :type-constraints) str-path)]
                      (when-let [subclasses (seq (sort (descendants @hier class)))]
                        [[:li
                          [:span
                           (into [:select.form-control
                                  {:on-change (fn [e]
                                                (dispatch [:qb/enhance-query-choose-subclass path (oget e :target :value) (:referencedType properties)]))
                                   :value (or sub (:referencedType properties))}
                                  [:option {:value (:referencedType properties)}
                                   (:displayName properties)]
                                  [:option {:disabled true :role "separator"}
                                   "─────────────────────────"]]
                                 (map (fn [subclass]
                                        [:option {:value subclass}
                                         (display-name model subclass)])
                                      subclasses))]]])))
                  (if sub
                    (map (fn [i] [attribute model i path sub]) (sort-classes (remove (comp (partial = :id) first) (im-path/attributes model sub))))
                    (map (fn [i] [attribute model i path sub]) (sort-classes (remove (comp (partial = :id) first) (im-path/attributes model (:referencedType properties))))))
                  (if sub
                    (map (fn [i] [node model i path false]) (sort-classes (im-path/relationships model sub)))
                    (map (fn [i] [node model i path false]) (sort-classes (im-path/relationships model (:referencedType properties))))))))]))))

(defn model-browser []
  (fn [model root-class]
    [:div.model-browser
     (let [path [root-class]
           attributes (->> (im-path/attributes model root-class)
                           (remove (comp #{:id} key)) ; Hide id attribute.
                           (sort-classes))
           relationships (->> (im-path/relationships model root-class)
                              ;; Make sure class is present in model.
                              ;; (If the class has no members, it won't be.)
                              (filter #(contains? (:classes model)
                                                  (-> % val :referencedType keyword)))
                              (sort-classes))]
       (into [:ul
              [:li.model-button-container
               [:div.model-button-group
                [:button.btn.btn-slim
                 {:on-click #(dispatch [:qb/expand-all])
                  :title "Expand the Model Browser tree to show all selected attributes"}
                 [icon "enlarge2"] "Expand"]
                [:button.btn.btn-slim
                 {:on-click #(dispatch [:qb/collapse-all])
                  :title "Collapse the Model Browser tree to the top level"}
                 [icon "shrink"] "Collapse"]
                [:button.btn.btn-slim
                 {:on-click #(dispatch [:qb/enhance-query-clear-query])
                  :title "Remove all selected attributes"}
                 [icon "bin"] "Clear"]]]]
             (concat
              (map (fn [class-entry] [attribute model class-entry path]) attributes)
              (map (fn [class-entry] [node model class-entry path]) relationships))))]))

(defn data-browser-node []
  (let [open? (reagent/atom true)]
    (fn [close! model hier tag]
      (let [;; To get the immediate children, we filter the descendants down to
            ;; those which have `tag` as immediate parent.
            children (filter #(contains? (parents hier %) tag)
                             (descendants hier tag))
            tag-count (get-in model [tag :count])
            is-nonempty (when (number? tag-count) (pos? tag-count))]
        [:li.haschildren.qb-group
         {:class (when @open? "expanded-group")}
         [:div.group-title
          (if (seq children)
            {:class "is-parent"
             :role "treeitem"
             :aria-expanded true}
            {:class "is-closed"
             :role "treeitem"
             :aria-expanded false})
          (when (seq children)
            [:a.expand-close {:on-click #(swap! open? not)}
             [icon (if @open? "minus" "plus")]])
          (if is-nonempty
            [:a.qb-class
             {:class (when (empty? children) "no-icon")
              :on-click #(do (dispatch [:qb/set-root-class tag]) (close!))}
             tag]
            [:span.qb-class.empty-class
             {:class (when (empty? children) "no-icon")}
             tag])
          (when is-nonempty
            [:span.class-count tag-count])]
         (when (and (seq children) @open?)
           (into [:ul]
                 (for [child (sort children)]
                   [data-browser-node close! model hier child])))]))))

(defn data-browser [close!]
  (let [model @(subscribe [:model])
        hier @(subscribe [:current-model-hier])]
    [:div.model-browser-column
     [:div.header-group
      [:h4 "Data Browser"]
      [:button.btn.btn-raised.btn-sm.browse-button
       {:on-click close!}
       [:svg.icon.icon-arrow-left [:use {:xlinkHref "#icon-arrow-left"}]]
       "Back to query"]]
     [:div.model-browser.class-browser
      (into [:ul]
            (for [root (->> (keys model)
                            (filter #(empty? (parents hier %)))
                            (sort))]
              [data-browser-node close! model hier root]))]]))

(defn browser-pane []
  (let [current-model (subscribe [:current-model])
        type-constraints (subscribe [:qb/menu-type-constraints])
        root-class (subscribe [:qb/root-class])
        browse-model? (reagent/atom true)]
    (fn []
      (if @browse-model?
        [:div.model-browser-column
         [:h4 "Model Browser"]
         [:div.model-browser-intro
          [:p "Select a Data Type or browse "]
          [:button.btn.btn-raised.btn-sm.browse-button
           {:on-click #(swap! browse-model? not)}
           [icon "tree"]
           "Data Model"]]
         [:div.model-browser-root
          [root-class-dropdown]
          (when @root-class
            [:button.label-button
             {:on-click #(dispatch [:qb/enhance-query-add-summary-views [(name @root-class)]])
              :title (str "Summarise " @root-class " by adding its common attributes")}
             "Summary"])]
         (if @root-class
           [model-browser
            (assoc @current-model :type-constraints @type-constraints)
            (name @root-class)]
           [:div
            [:hr]
            [:p.text-muted "Advanced users can use a flexible query interface to construct their own data mining queries. The Query Builder lets you view the data model, apply constraints and select output. You can also export queries to share them with others. "
             [:a {:href "http://intermine.org/intermine-user-docs/docs/the-query-builder" :target "_blank"} "Click here"]
             " to learn how to use the Query Builder - we also have a "
             [:a {:href "http://intermine.org/intermine-user-docs/docs/the-query-builder#video-tutorial" :target "_blank"} "video tutorial"]
             "."]
            [:p.text-muted "To get started, use the dropdown above to select a data type to create a new query starting at that class."]])]
        [data-browser #(swap! browse-model? not)]))))

(defn dissoc-keywords [m]
  (apply dissoc m (filter keyword? (keys m))))

(defn queryview-node []
  (let [lists (subscribe [:current-lists])
        class-keys (subscribe [:current-class-keys])]
    (fn [model [k properties] & [trail]]
      (let [path (vec (conj trail (name k)))
            class-node? (im-path/class? model (join "." path))
            non-root-class? (and class-node? (> (count path) 1))
            path-kw (map keyword path)
            [{referenced-name :displayName, referenced-class :name}
             {parent-name :displayName}] (reverse (im-path/walk model path-kw))
            relational-name (if non-root-class?
                              (rel-display-name model path-kw)
                              referenced-name)]
        [:li.tree.haschildren
         [:div.flexmex
          [:span.lab {:class (if class-node? "qb-class" "qb-attribute")}
           [:span.qb-label {:style {:margin-left 5}}
            [tooltip {:on-click #(dispatch [:qb/expand-path path])
                      :title (str "Show " relational-name " in the model browser")}
             [:a relational-name]]]
           (when (and non-root-class? (not= (lower-case referenced-class)
                                            (lower-case k)))
             [:span.qb-type (str "(" (rel-referenced-type model path-kw) ")")])
           (when-let [s (:subclass properties)]
             [:span.label.label-default (display-name model s)])
           [:svg.icon.icon-bin
            {:on-click (if (> (count path) 1)
                         (fn [] (dispatch [:qb/enhance-query-remove-view path]))
                         (fn [] (dispatch [:qb/enhance-query-clear-query path])))}
            [:use {:xlinkHref "#icon-bin"}]]

           (when non-root-class?
             (let [outer-join? @(subscribe [:qb/active-outer-join (join "." path)])]
               [:a.outer-join-button
                {:role "button"
                 :class (when outer-join? "active")
                 :title (if outer-join?
                          (str "Show all " (plural parent-name) " and show " (plural referenced-name) " if they are present")
                          (str "Show only " (plural parent-name) " if they have a " referenced-name))
                 :on-click #(dispatch [(if outer-join?
                                         :qb/remove-outer-join
                                         :qb/add-outer-join) path])}
                (if outer-join?
                  [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
                  [:svg.icon.icon-venn-intersection [:use {:xlinkHref "#icon-venn-intersection"}]])]))

           ;; Disallow adding constraints to classes without class keys.
           (when (or (not class-node?)
                     (contains? @class-keys (keyword referenced-class)))
             [:svg.icon.icon-filter
              {:on-click (fn []
                           (dispatch [:qb/enhance-query-add-constraint path]))}
              [:use {:xlinkHref "#icon-filter"}]])

           (when-let [c (:id-count properties)]
             [:span.label.label-soft
              {:class (when (= 0 c) "label-no-results")
               :style {:margin-left 5}} (str c " row" (when (not= c 1) "s"))])]]
         (when-let [constraints (:constraints properties)]
           (into [:ul.tree.banana]
                 (map-indexed (fn [idx con]
                                [:li
                                 [:div.contract
                                  {:class (when (empty? (:value con)) "empty")}
                                  [constraint
                                   :model model
                                   :path (join "." path)
                                   :lists @lists
                                   :code (:code con)
                                   :on-remove (fn [] (dispatch [:qb/enhance-query-remove-constraint path idx]))
                                   :possible-values (when (some? (:possible-values properties)) (map :item (:possible-values properties)))
                                   :typeahead? true
                                   :value (or (:value con) (:values con))
                                   :op (:op con)
                                   ;; Do we need all these? I think it would be simpler to just have:
                                   ;;     on-change - update state
                                   ;;     on-blur   - update state and rerun query
                                   ;; If you find you need to refactor these, try converting it to the above.
                                   :on-select-list (fn [c]
                                                     (dispatch [:qb/enhance-query-update-constraint path idx c])
                                                     (dispatch [:qb/enhance-query-build-im-query true]))
                                   :on-change-operator (fn [x]
                                                         (dispatch [:qb/enhance-query-update-constraint path idx x])
                                                         (dispatch [:qb/enhance-query-build-im-query true]))
                                   :on-change (fn [c]
                                                (dispatch [:qb/enhance-query-update-constraint path idx c]))
                                   :on-blur (fn [c]
                                              (dispatch [:qb/enhance-query-update-constraint path idx c])
                                              (dispatch [:qb/enhance-query-build-im-query true]))
                                   :label? false]]]) constraints)))
         (when (not-empty (dissoc-keywords properties))
           (let [classes (filter (fn [[k p]] (im-path/class? model (join "." (conj path k)))) (dissoc-keywords properties))
                 attributes (filter (fn [[k p]] ((complement im-path/class?) model (join "." (conj path k)))) (dissoc-keywords properties))]
             (into [:ul.tree.banana2]
                   (concat
                    (map (fn [n] [queryview-node model n path])
                         (sort-by (comp string/lower-case key) attributes))
                    (map (fn [n] [queryview-node model n path])
                         (sort-by (comp string/lower-case key) classes))))))]))))

(defn example-button []
  [:button.btn.btn-primary.btn-raised
   {:on-click (fn [] (dispatch [:qb/load-example aquery]))}
   "Example"])

(defn queryview-browser []
  (let [enhance-query (subscribe [:qb/enhance-query])
        default-query? (subscribe [:qb/example])]
    (fn [model]
      (if (not-empty @enhance-query)
        [:div.query-browser
         (into [:ul.tree]
               (map (fn [n]
                      [queryview-node model n]) @enhance-query))]
        [:div
         [:p "Please select at least one attribute from the Model Browser on the left."]
         (cond (seq @default-query?) [example-button])]))))

(def <sub (comp deref subscribe))

(defn logic-box []
  (let [editing? (reagent/atom false)
        logic (subscribe [:qb/constraint-logic])]
    (fn []
      [:div.logic-container
       [:label {:for "logic-input"} "Constraint Logic:"]
       [:div.logic-box
        {:ref (fn [x]
                (some-> x (ocall :getElementsByTagName "input") array-seq first (ocall :focus)))}
        (if @editing?
          [:input.form-control.input-sm
           {:type "text"
            :value @logic
            :id "logic-input"
            :on-key-down (fn [e] (when (= (oget e :keyCode) 13)
                                   (reset! editing? false)
                                   (dispatch [:qb/format-constraint-logic @logic])))
            :on-blur (fn []
                       (reset! editing? false)
                       (dispatch [:qb/format-constraint-logic @logic]))
            :on-change (fn [e] (dispatch [:qb/update-constraint-logic (oget e :target :value)]))}]
          [:pre
           [:a {:role "button"
                :id "logic-input"
                :title "Edit constraint logic"
                :on-click (fn [] (reset! editing? true))}
            [:svg.icon.icon-edit [:use {:xlinkHref "#icon-edit"}]]]
           [:span#logic-input @logic]])]])))

(defn controls []
  (let [saving-query? (reagent/atom false)
        query-title (reagent/atom "")
        results-preview (subscribe [:qb/preview])
        submit-fn #(when-let [title (not-empty @query-title)]
                     (swap! saving-query? not)
                     (reset! query-title "")
                     (dispatch [:qb/save-query title]))
        fetching-preview? (subscribe [:qb/fetching-preview?])]
    (fn []
      (if @saving-query?
        [:div.button-group
         [:div.input-group
          [:label {:for "query-title-input"} "Title:"]
          [:input#query-title-input.form-control
           {:type "text"
            :autoFocus true
            :value @query-title
            :on-change #(reset! query-title (oget % :target :value))
            :on-key-up #(when (= (oget % :keyCode) 13)
                          (submit-fn))}]]
         [:button.btn.btn-raised
          {:on-click submit-fn}
          "Save Query"]
         [:button.btn
          {:on-click (fn [] (swap! saving-query? not))}
          "Cancel"]]
        [:div.button-group
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn [] (dispatch [:qb/export-query]))}
          (str "Show All " (when-let [c (:iTotalRecords @results-preview)] (str c " ")) "Rows")]
         [:button.btn.btn-raised
          {:on-click (fn [] (swap! saving-query? not))}
          "Save Query"]
         [:button.btn.btn-raised
          {:on-click (fn [] (dispatch [:qb/enhance-query-clear-query]))}
          "Clear Query"]
         (when @fetching-preview?
           [:div.query-preview-loader
            [mini-loader "tiny"]])]))))

(defn preview []
  (let [results-preview @(subscribe [:qb/preview])
        preview-error @(subscribe [:qb/preview-error])]
    [:div.preview-container
     (cond
       preview-error [:div
                      [:p "Error occured when running query."]
                      [:pre.well.text-danger preview-error]]
       results-preview [preview-table
                        :loading? false ; The loader is ugly.
                        :hide-count? true
                        :query-results results-preview]
       :else [:p "No query available for generating preview."])]))

(defn move-vec-elem
  "Moves an element in a vector `v` from index `i` to `i'`."
  [v i i']
  (let [e (nth v i)
        v' (into (subvec v 0 i)
                 (subvec v (inc i)))]
    (into [] cat
          [(subvec v' 0 i')
           [e]
           (subvec v' i')])))

(defn manage-columns []
  (let [order (subscribe [:qb/order])
        current-model (subscribe [:current-model])
        current-constraints (subscribe [:qb/im-query-constraints])
        selected* (reagent/atom nil)]
    (fn []
      (into [:div.sort-order-container
             {:class (when (some? @selected*) "dragtest")}]
            (map-indexed
             (fn [idx path]
               [:div.sort-item
                {:class (when (= idx @selected*) "dragging")
                 :draggable true
                 :on-drag-start (fn [e]
                                  (ocall e :stopPropagation)
                                  (reset! selected* idx))
                 :on-drag-enter (fn [e]
                                  (ocall e :preventDefault)
                                  (ocall e :stopPropagation)
                                  (let [new-order (move-vec-elem @order @selected* idx)]
                                    (reset! selected* idx)
                                    (dispatch [:qb/set-order new-order])))
                 :on-drag-end (fn [] (reset! selected* nil))}
                (into [:div.path-parts]
                      (let [model (assoc @current-model
                                         :type-constraints @current-constraints)]
                        (map (fn [part]
                               [:span.part part])
                             (->> (split path #"\.")
                                  (map keyword)
                                  (im-path/walk model)
                                  (map :displayName)
                                  (interpose ">")))))
                (let [subpaths (->> (split path #"\.") (iterate drop-last) (take-while not-empty) (set))
                      outer-joined-section? (some #(contains? subpaths %)
                                                  (map #(split % #"\.") @(subscribe [:qb/joins])))]
                  (if outer-joined-section?
                    [:span.outerjoined-section {:title "Optional attributes cannot be sorted"}
                     [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]]
                    [:div.button-group
                     (when-let [sort-priority @(subscribe [:qb/sort-priority path])]
                       [:span.sort-priority (str "Sort " (-> sort-priority inc ordinalize))])
                     [:a.sort-button {:role "button"
                                      :title "Sort ascending"
                                      :class (when (= "ASC" @(subscribe [:qb/active-sort path]))
                                               "active")
                                      :on-click #(dispatch [:qb/set-sort path "ASC"])}
                      [:svg.icon.icon-sort-alpha-asc [:use {:xlinkHref "#icon-sort-alpha-asc"}]]]
                     [:a.sort-button {:role "button"
                                      :title "Sort descending"
                                      :class (when (= "DESC" @(subscribe [:qb/active-sort path]))
                                               "active")
                                      :on-click #(dispatch [:qb/set-sort path "DESC"])}
                      [:svg.icon.icon-sort-alpha-desc [:use {:xlinkHref "#icon-sort-alpha-desc"}]]]]))]))
            @order))))

(defn xml-view []
  (let [query (subscribe [:qb/im-query])
        current-mine (subscribe [:current-mine])
        clipboard (atom nil)
        msg (reagent/atom nil)]
    (fn []
      (let [xml (str (->xml (get-in @current-mine [:service :model]) @query))]
        [:div
         [:textarea.hidden-clipboard
          {:ref #(reset! clipboard %) :value xml :read-only true}]
         [:pre xml]
         [:div.flex-row
          [:button.btn.btn-raised
           {:type "button"
            :on-click
            #(when-let [clip @clipboard]
               (.focus clip)
               (.select clip)
               (try
                 (ocall js/document :execCommand "copy")
                 (reset! msg {:type "success" :text "Copied to clipboard"})
                 (catch js/Error _
                   (reset! msg {:type "failure" :text "Failed to copy to clipboard"}))))}
           "Copy XML"]
          (when-let [{:keys [type text]} @msg]
            [:p {:class type} text])]]))))

(defn joins-list []
  (let [joins @(subscribe [:qb/joins])]
    (when (not-empty joins)
      (into [:ul.joins-list]
            (for [path joins]
              [:li
               [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
               [:span [:code path] "will show as a subtable if present"]])))))

(defn query-editor []
  (let [current-model @(subscribe [:current-model])
        current-constraints @(subscribe [:qb/im-query-constraints])
        constraint-count @(subscribe [:qb/constraint-count])]
    [:div
     [queryview-browser (assoc current-model :type-constraints current-constraints)]
     [joins-list]
     (when (>= constraint-count 2)
       [logic-box])]))

(defn query-viewer []
  (let [enhance-query (subscribe [:qb/enhance-query])
        tab-index (reagent/atom 0)]
    (fn []
      [:div.panel.panel-default
       [:div.panel-body
        [:ul.nav.nav-tabs
         [:li {:class (when (= @tab-index 0) "active")} [:a {:on-click #(reset! tab-index 0)} "Query Editor"]]
         [:li {:class (when (= @tab-index 1) "active")} [:a {:on-click #(reset! tab-index 1)} "Manage Columns"]]]
        (case @tab-index
          0 [query-editor]
          1 [manage-columns])
        (when (not-empty @enhance-query) [controls])]])))

(defn column-order-preview []
  (let [enhance-query (subscribe [:qb/enhance-query])
        current-mine (subscribe [:current-mine])
        tab-index (reagent/atom 0)]
    (fn []
      [:div.panel.panel-default
       [:div.panel-body
        [:ul.nav.nav-tabs
         [:li {:class (when (= @tab-index 0) "active")} [:a {:on-click #(reset! tab-index 0)} "Preview"]]
         [:li {:class (when (= @tab-index 1) "active")} [:a {:on-click #(reset! tab-index 1)} "XML"]]]
        (case @tab-index
          0 [preview]
          1 [xml-view])]])))

(defn short-readable-path
  "Takes a path and returns a concise abbreviated version.
  (short-readable-path 'Gene.homologues.homologue.symbol')
  => 'G.h.homologue.symbol'"
  [path]
  (let [pathv (string/split path #"\.")]
    (if (> (count pathv) 2)
      (let [[pre-path class-attr] (split-at (- (count pathv) 2) pathv)]
        (string/join "." [(string/join "." (map first pre-path))
                          (string/join "." class-attr)]))
      path)))

(defn truncate-path [path]
  (string/join "." (take-last 2 (string/split path #"\."))))

(defn recent-queries []
  (let [queries (take 5 @(subscribe [:results/historical-custom-queries]))
        active-query @(subscribe [:qb/im-query])]
    (if (empty? queries)
      [:p "Queries that you have run during this session will appear here."]
      [:table.table.query-table
       [:thead
        [:tr
         [:th "Start"]
         [:th "Results Format"]
         [:th "Last Run"]]]
       (into [:tbody]
             (for [[_ {:keys [value last-executed]}] queries
                   :let [{:keys [from select]} value
                         active? (query= active-query (dissoc value :title))]]
               [:tr {:role "button"
                     :title (if active? "This query is active" "Load this query")
                     :class (when active? "active-query")
                     :on-click #(dispatch [:qb/load-query value])}
                [:td [:code.start {:class (str "start-" from)} from]]
                [:td (into [:<>] (for [path select]
                                   [:code {:title path} (truncate-path path)]))]
                [:td [:span.date (.toLocaleTimeString (js/Date. last-executed))]]]))])))

(defn saved-query [title _query]
  (let [renaming? (reagent/atom false)
        rename-input (reagent/atom title)]
    (fn [title {:keys [from select] :as query} & {:keys [active? any-renaming*]}]
      (let [toggle-rename! #(do (.stopPropagation %)
                                (swap! any-renaming* not)
                                (swap! renaming? not)
                                (reset! rename-input title))
            rename! #(do (.stopPropagation %)
                         (when-let [new-title (not-empty @rename-input)]
                           (when (not= title new-title)
                             (dispatch [:qb/rename-query title new-title])))
                         (toggle-rename! %))]
        [:tr {:role "button"
              :title (if active? "This query is active" "Load this query")
              :class (when active? "active-query")
              :on-click #(dispatch [:qb/load-query query])}
         (if @renaming?
           [:td
            [:input.form-control
             {:type "text"
              :placeholder title
              :value @rename-input
              :autoFocus true
              :aria-label "New title for query"
              :title "New title for query"
              :on-change #(reset! rename-input (oget % :target :value))
              :on-key-up #(when (= (oget % :keyCode) 13) (rename! %))}]]
           [:td (str title)])
         [:td [:code.start {:class (str "start-" from)} from]]
         [:td.hidden-lg {:class (when-not @any-renaming* "hidden")}
          [:code {:title (string/join " " select)} "..."]]
         [:td {:class (when @any-renaming* "visible-lg-block")}
          (into [:<>] (for [path select]
                        [:code {:title path} (truncate-path path)]))]
         [:td
          (when-not @any-renaming*
            [:<>
             [:a {:role "button"
                  :title ""
                  :on-click #(do (.stopPropagation %)
                                 (dispatch [:qb/load-query query]))}
              "Load"]
             " "
             [:a {:role "button"
                  :title ""
                  :on-click toggle-rename!}
              "Rename"]
             " "
             [:a {:role "button"
                  :title ""
                  :on-click #(do (.stopPropagation %)
                                 (dispatch [:qb/delete-query title]))}
              "Delete"]])
          (when @renaming?
            [:<>
             [:a {:role "button"
                  :title ""
                  :on-click rename!}
              "Save"]
             " "
             [:a {:role "button"
                  :title ""
                  :on-click toggle-rename!}
              "Cancel"]])]]))))

(defn saved-queries []
  (let [queries (subscribe [:qb/saved-queries])
        active-query (subscribe [:qb/im-query])
        authed? (subscribe [:bluegenes.subs.auth/authenticated?])
        any-renaming? (reagent/atom false)]
    (fn []
      (if (empty? @queries)
        [:p "Queries that you have saved will appear here."]
        [:table.table.query-table
         [:thead
          (when (and (not-empty @queries) (not @authed?))
            [:tr.danger
             [:th {:colSpan 4}
              "Warning: Since you're not logged in, these queries have been saved to your session and may disappear when exiting BlueGenes."]])
          [:tr
           ;; Remember to update colspan if you change the amount of columns.
           [:th "Title"]
           [:th "Start"]
           [:th "Results Format"]
           [:th "Actions"]]]
         (into [:tbody]
               (for [[title query] @queries]
                 ^{:key title}
                 [saved-query title query
                  :active? (query= @active-query query)
                  :any-renaming* any-renaming?]))]))))

(defn import-from-xml []
  (let [query-input (reagent/atom "")]
    (fn []
      [:div
       [:p "Paste your InterMine PathQuery XML here."
        [:a {:href "http://intermine.org/im-docs/docs/api/pathquery/"
             :target "_blank"
             :title "More information on the PathQuery API"}
         [:svg.icon.icon-external [:use {:xlinkHref "#icon-external"}]]]]
       [:textarea.form-control
        {:rows 10
         :autoFocus true
         :value @query-input
         :on-change #(reset! query-input (oget % :target :value))}]
       [:div.flex-row
        [:button.btn.btn-raised
         {:type "button"
          :on-click #(when-let [query (not-empty @query-input)]
                       (reset! query-input "")
                       (dispatch [:qb/import-xml-query query]))}
         "Load query"]
        (when-let [{:keys [type text]} @(subscribe [:qb/import-result])]
          [:p {:class type} text])]])))

(defn create-template [])

(defn other-query-options []
  (let [tab-index (reagent/atom 0)]
    (fn []
      [:div.panel.panel-default
       [:div.panel-body
        (into [:ul.nav.nav-tabs]
              (let [tabs ["Recent Queries"
                          "Saved Queries"
                          "Import from XML"]]
                          ; "Create Template"]]
                (for [[i title] (map-indexed vector tabs)]
                  [:li {:class (when (= @tab-index i) "active")}
                   [:a {:on-click #(do (when (= @tab-index
                                                (.indexOf tabs "Import from XML"))
                                         (dispatch [:qb/clear-import-result]))
                                       (reset! tab-index i))}
                    title]])))
        (case @tab-index
          0 [recent-queries]
          1 [saved-queries]
          2 [import-from-xml])]])))
          ; 3 [create-template])]])))

(defn main []
  [:div.column-container
   [browser-pane]
   [:div.query-view-column
    [:div.row
     [:div.col-xs-12.col-xl-5-workaround
      [query-viewer]]
     [:div.col-xs-12.col-xl-7-workaround
      [column-order-preview]]]
    [:div.row
     [:div.col-xs-12
      [other-query-options]]]]])
