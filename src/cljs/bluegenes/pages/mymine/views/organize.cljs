(ns bluegenes.pages.mymine.views.organize
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [clojure.string :as string]
            [clojure.set :as s]
            [oops.core :refer [ocall oget]]))

(def path-tag-prefix "bgpath")

;;;;;;;;;;;;;;;;;;;;
;; Tree Structure ;;
;;;;;;;;;;;;;;;;;;;;

(defn extract-path-tag
  "Takes a sequence of tags and returns the first tag starting with
  `path-tag-prefix` and a colon, as a vector where the first element
  is the full tag and the second only the path string."
  [tags]
  (let [re (re-pattern (str path-tag-prefix ":(.+)"))]
    (some #(re-find re %) tags)))

(defn read-path
  "Takes a sequence of tags and returns the first tag starting with
  `path-tag-prefix` and a colon, as a vector split at each '.' char."
  [tags]
  (some-> (extract-path-tag tags)
          second
          (string/split #"\.")))

(defn tree-path
  "Takes a vector of strings and returns the corresponding tree path as a
  vector of keywords and strings. This path will be the location of the list
  with specified tag, in a tree implemented as nested maps."
  [path]
  (apply concat [(when path [:folders])
                 (interpose :folders path)
                 [:lists]]))

(defn lists->tree
  "Pass a sequence of list maps and receive a complete tree."
  [lists]
  (reduce (fn [tree {:keys [tags title] :as l}]
            (let [path (read-path tags)
                  ks   (tree-path path)
                  l'   (select-keys l [:tags :title :id])]
              (update-in tree ks assoc title l')))
          {}
          lists))

(defn tree->tags
  "Returns a map of list titles and their new path tags, as computed from the tree."
  ([tree] (tree->tags [] tree))
  ([path tree]
   (into {}
         (concat
          (map (fn [{:keys [title]}]
                 [title (when (not-empty path)
                          (str path-tag-prefix ":" (string/join "." path)))])
               (vals (:lists tree)))
          (map (fn [[k subtree]]
                 (tree->tags (conj path k) subtree))
               (:folders tree))))))

(defn drop-lastv
  "Works just like `drop-last` except it takes and returns a vector instead of
  a lazy sequence."
  [n v]
  (subvec v 0 (max 0 (- (count v) n))))

(defn empty-folders
  "Returns a collection of paths to empty folders present in the tree. This is
  so we can inform the user that these won't be saved. Note that parents of
  empty folders which don't have list children are also considered empty."
  [tree]
  (letfn [;; Recursively inspect the tree, returning paths to folder leaf nodes.
          (empty-nodes [path {:keys [folders] :as tree}]
            (if (empty? folders)
              [path]
              (mapcat (fn [[k subtree]]
                        (empty-nodes (into path [:folders k]) subtree))
                      (:folders tree))))]
    ;; Now we need to go through all the parents of the folder leaf nodes
    ;; returned by the recursive function above, until we find a folder with
    ;; lists. This is because we consider folders that only have empty folders
    ;; as children, to also be empty.
    (let [[empties lists]
          ;; In addition to paths to empty folders, we also keep track of paths
          ;; to parent folders which we have found to have list children.
          ;; We only track upwards from an empty folder, so it's possible that
          ;; some paths don't discover list children while others do.
          (reduce (fn [[empties lists] path]
                    (let [[empty-paths list-paths]
                          (split-with #(empty? (get-in tree (conj % :lists)))
                                      (take-while not-empty
                                                  (iterate #(drop-lastv 2 %) path)))]
                      [(into empties empty-paths) (into lists list-paths)]))
                  [#{} #{}]
                  (empty-nodes [] tree))]
      (s/difference empties lists))))

;;;;;;;;;;;
;; Logic ;;
;;;;;;;;;;;

(defn list-path?
  "Whether `p` is a path to a list."
  [p]
  (= (get p (- (count p) 2))
     :lists))

(defn direct-parent-of?
  "Path `p` is a direct parent of path `c`."
  [p c]
  (= p (drop-last 2 c)))

(defn child-of?
  "Path `c` is a child (nested somewhere within) path `p`."
  [c p]
  (let [all-parents (->> (iterate #(drop-lastv 2 %) c)
                         (take-while not-empty)
                         (set))]
    (contains? all-parents p)))

(defn top-node?
  "Path `p` is a node at the top of the tree, meaning its parent is the root node."
  [p]
  (= (count p) 2))

(defn root-node?
  "Whether `p` is the path to the root node."
  [p]
  (= p []))

(defn trash-node?
  "Whether `p` is the path to the trash node."
  [p]
  (= p [:trash]))

(defn has-children?
  "Whether `p` is a path to a folder node with children."
  [tree p]
  (let [{:keys [folders lists]} (get-in tree p)]
    (boolean (or (not-empty folders)
                 (not-empty lists)))))

(defn folder-exists?
  "Whether `folder-path` is a folder name already present in `path` in `tree`."
  [tree folder-path path]
  (contains? (get-in tree (conj path :folders))
             (last folder-path)))

(defn being-dragged?
  "Predicate for whether `path` is the target of `dragging`."
  [path dragging]
  (= path dragging))

(defn being-dropped?
  "Predicate for whether `path` is the target of `dropping`."
  [path dropping]
  (or (= path dropping)
      (and (list-path? dropping)
           (= path (drop-last 2 dropping)))))

(defn valid-drop?
  "Predicate for whether dropping paths `dragging` into `dropping` is a valid action."
  [tree dragging dropping]
  (let [dropping (if (list-path? dropping)
                   (drop-lastv 2 dropping)
                   dropping)]
    (and dragging dropping
         (not= dragging dropping)
         (not (direct-parent-of? dropping dragging))
         (not (child-of? dropping dragging))
         (not (folder-exists? tree dragging dropping)))))

;;;;;;;;;;;;;;;;;;;;;
;; Event Listeners ;;
;;;;;;;;;;;;;;;;;;;;;
;; Useful overview of drag and drop events:
;; https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API#Drag_Events

(defn drag-events
  "Event attributes for a draggable node."
  [state item]
  {:draggable true

   :on-drag-start (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc-in [:drag :dragging] item))

   :on-drag-end   (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc :drag nil))})

(defn drop-events
  "Event attributes for a droppable node."
  [state item]
  {:on-drag-over  (fn [evt]
                    (ocall evt :preventDefault)
                    (ocall evt :stopPropagation))

   :on-drag-enter (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc-in [:drag :dropping] item))

   :on-drop       (fn [evt]
                    (ocall evt :stopPropagation)
                    (let [{:keys [dragging dropping]} (:drag @state)]
                      (when (valid-drop? (:tree @state) dragging dropping)
                        (swap! state
                               (fn [{:keys [tree] :as prev-state}]
                                 ;; Make sure `dropping` in this context refers
                                 ;; to the parent folder if it's a list.
                                 (let [dropping (if (list-path? dropping)
                                                  (subvec dropping 0 (- (count dropping) 2))
                                                  dropping)
                                       state' (-> prev-state
                                                  ;; Clear drag state.
                                                  (assoc :drag nil)
                                                  ;; Clear error if present.
                                                  (cond-> (contains? prev-state :error)
                                                    (dissoc :error)))]
                                   (cond
                                     (and (trash-node? dropping) (has-children? tree dragging))
                                     (assoc state' :error "Only empty folders can be deleted.")
                                     :else
                                     (assoc state' :tree
                                            (-> tree
                                                ;; Create node in new location (if it wasn't trashed).
                                                (cond-> (not (trash-node? dropping))
                                                  (assoc-in (into dropping (take-last 2 dragging))
                                                            (get-in tree dragging)))
                                                ;; Delete node from old location.
                                                (update-in (drop-last dragging) dissoc (last dragging)))))))))))})

(defn cancel-drop-events
  "Event attributes for an element whose traversal nullifies the dropping target.
  We use this for the container element, which has a padding that lies outside
  any of the drag & drop child elements, so we can safely cancel any drop targets
  when the user moves into this area."
  [state]
  {:on-drag-enter (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc-in [:drag :dropping] nil))})

;;;;;;;;;;;;;;;;;;
;; DOM Elements ;;
;;;;;;;;;;;;;;;;;;

(defn node
  "This can be a folder or a list, which you can interact with through
  drag and drop. Note that a node can also be 'fake' or 'hidden', in which
  case they're non-interactive and used as a placeholder to signify a valid
  drop target."
  [state {:keys [title icon last? on-click path fake?
                 ;; Keys in line below are solely for renaming folders.
                 rename? edit? on-edit edit-value edit-change edit-save]} & children]
  (let [{:keys [dragging dropping]} (:drag @state)
        drag? (and (being-dragged? path dragging)
                   (not (valid-drop? (:tree @state) dragging dropping)))
        drop? (and (being-dragged? path dragging)
                   (valid-drop? (:tree @state) dragging dropping))]
    (into [:li {:className (str "node-container"
                                (when-not        last? " node-middle")
                                (when (or fake? drag?) " node-fake")
                                (when            drop? " node-hide"))}
           [:div.node-parent (when-not fake? (drop-events state path))
            [:div.node (when-not fake?
                         (merge (drag-events state path)
                                (when on-click {:on-click on-click})))
             [:svg {:className (str "icon symbol-icon icon-" icon)}
              [:use {:xlinkHref (str "#icon-" icon)}]]
             (if edit?
               [:input.form-control {:type "text"
                                     :placeholder title
                                     :value edit-value
                                     :on-change edit-change
                                     :on-click #(ocall % :stopPropagation)
                                     :on-key-up #(when (= (oget % :keyCode) 13)
                                                   (edit-save %))
                                     :auto-focus true}]
               [:span.text title])
             (when rename?
               (if edit?
                 [:<>
                  [:a {:role "button" :on-click on-edit}
                   [:svg.icon.edit-icon.icon-close [:use {:xlinkHref "#icon-close"}]]]
                  [:a {:role "button" :on-click edit-save}
                   [:svg.icon.edit-icon.icon-checkmark [:use {:xlinkHref "#icon-checkmark"}]]]]
                 [:a {:role "button" :on-click on-edit}
                  [:svg.icon.edit-icon.icon-edit [:use {:xlinkHref "#icon-edit"}]]]))
             [:span.drag-handle "☰"]]]]
          children)))

(defn root-node
  "This is a special root node for use as a drop target to move nested nodes
  into the top level."
  [state]
  [:li.node-container.node-fake
   [:div.node-parent (drop-events state [])
    [:div.node.root (drag-events state [])]]])

(defn trash-node
  "This is a special trash node for use as a drop target to delete folder nodes."
  [state]
  [:li.node-container.node-fake
   [:div.node-parent (drop-events state [:trash])
    [:div.node.trash (drag-events state [:trash])
     [:svg.icon.icon-bin [:use {:xlinkHref "#icon-bin"}]]]]])

(defn list-node
  "Wrapper around node for lists."
  [state path [_ {:keys [title fake?]}] & {:keys [last?]}]
  [node state {:title title
               :icon "list"
               :last? last?
               :path path
               :fake? fake?}])

(declare level)
(defn folder-node
  "Wrapper around node for folders."
  []
  (let [open? (r/atom true)
        edit? (r/atom false)
        edit-value (r/atom "")]
    (fn [state path [title children] & {:keys [last?]}]
      [node state
       {:title title
        :icon (str "folder" (when @open? "-open"))
        :last? last?
        :on-click #(swap! open? not)
        :path path
        :fake? (:fake? children)
        ;; Remaining properties are for supporting renaming of folders.
        :rename? true
        :edit? @edit?
        :on-edit #(do (ocall % :stopPropagation)
                      (reset! edit-value title)
                      (swap! edit? not)
                      (when (contains? @state :error)
                        (swap! state dissoc :error)))
        :edit-value @edit-value
        :edit-change #(reset! edit-value (oget % :target :value))
        :edit-save (fn [evt]
                     (ocall evt :stopPropagation)
                     (let [p (into [:tree] (drop-last path))
                           new-title (string/trim @edit-value)]
                       (cond
                         (or (empty? new-title) (= title new-title))
                         (swap! edit? not)

                         (contains? (get-in @state p) new-title)
                         (swap! state assoc :error "A folder with that name already exists at the same level.")
                         :else
                         (swap! state
                                #(-> %
                                     (update-in p s/rename-keys {title new-title})
                                     ;; Clear error if present.
                                     (cond-> (contains? % :error)
                                       (dissoc :error)))))))}
       (when (and @open? (not-empty children))
         [level state path children])])))

(defn map->node
  "Takes a map corresponding to either a list or folder, as read from the tree,
  and returns the correct node element."
  [state path [title data] & {:keys [last?]}]
  (let [folder? (not (contains? data :tags))
        path'   (if folder?
                  (conj path :folders title)
                  (conj path :lists title))]
    ^{:key (if folder? (string/join "." (conj path title)) (:id data))}
    [(if folder? folder-node list-node)
     state path' [title data]
     :last? last?]))

(defn level
  "This corresponds to one level of nesting within the tree. Every time there's
  a folder, we nest once more. Hence this function is mutually recursive with
  `folder-node`. As you can see from the comments in the function body, there
  are 3 nodes added manually in addition to the 'real' nodes:
  - Placeholder node to signify a valid drop target at this level
  - Top level root node (see `root-node`)
  - Top level placeholder node for when the root node is the drop target
  - Top level trash node (see `trash-node`)"
  [state path {:keys [lists folders]} & {:keys [top?]}]
  (-> [:ul {:className (if top? "top-level-list" "list")}]
      (into (let [items (concat folders lists
                                ;; Append a placeholder node when the user hovers over
                                ;; a valid drop target.
                                (when-let [{:keys [dragging dropping]} (:drag @state)]
                                  (when (and (being-dropped? path dropping)
                                             (valid-drop? (:tree @state) dragging dropping)
                                             (not (root-node? dropping)))
                                    {(last dragging)
                                     (assoc (get-in @state (into [:tree] dragging))
                                            :fake? true)})))]
              (map-indexed
               (fn [i item]
                 (map->node state path item :last? (= (inc i) (count items))))
               items)))
      ;; Root target node for moving nested nodes into the top level.
      (into (when top?
              (when-let [{:keys [dragging]} (:drag @state)]
                (when (and (some? dragging)
                           (not (top-node? dragging)))
                  [[root-node state]]))))
      ;; Placeholder node specifically for when the user hovers over the above root node.
      (into (when top?
              (when-let [{:keys [dragging dropping]} (:drag @state)]
                (when (and (being-dropped? path dropping)
                           (valid-drop? (:tree @state) dragging dropping)
                           (root-node? dropping))
                  [(map->node state path
                              [(last dragging)
                               (assoc (get-in @state (into [:tree] dragging))
                                      :fake? true)])]))))
      ;; Trash target node for deleting folder nodes.
      (into (when top?
              (when-let [{:keys [dragging]} (:drag @state)]
                (when (and (some? dragging)
                           (not (list-path? dragging)))
                  [[trash-node state]]))))))

(defn new-folder
  "Input and button for creating a new folder."
  [state]
  (let [submit-fn
        #(swap! state
                (fn [prev-state]
                  (let [input (some-> (get-in prev-state [:inputs :folder])
                                      string/trim)
                        ;; Full stops are technically permitted but we use them for nesting.
                        valid? (re-matches #"[A-Za-z0-9 \-:]+" input)
                        exists? (contains? (get-in prev-state [:tree :folders]) input)]
                    (if (and valid? (not exists?))
                      (-> prev-state
                          (assoc-in [:tree :folders input]
                                    {:folders {} :lists {}})
                          (assoc-in [:inputs :folder] "")
                          (dissoc :error))
                      (assoc prev-state :error
                             (cond
                               (not valid?) "Folder names may only contain letters, numbers, spaces, hyphens and colons."
                               exists? (str (pr-str input) " already exists at the top level.")))))))]

    [:div
     [:div.node-parent
      [:div.node-input
       [:svg.icon.icon-folder-plus [:use {:xlinkHref "#icon-folder-plus"}]]
       [:input.form-control
        {:type "text"
         :placeholder "New folder"
         :value (get-in @state [:inputs :folder])
         :on-change (fn [evt] (swap! state assoc-in [:inputs :folder]
                                     (oget evt :target :value)))
         :on-key-up (fn [evt] (when (= (oget evt :keyCode) 13)
                                (submit-fn)))}]
       [:button.btn.btn-slim
        {:type "button"
         :on-click submit-fn}
        "Add"]]]
     (when-let [err-msg (or (get @state :error)
                            @(subscribe [:bluegenes.pages.mymine.subs/modal-data
                                         [:organize :error]]))]
       [:p.error err-msg])]))

(defn confirm
  "Returns a stripped non-interactive organize element highlighting empty folders.
  You'll want to use `empty-folders` to generate the input to this function."
  [empties]
  (let [tree (reduce #(update-in %1 %2 merge {:empty? true}) {} empties)]
    (letfn [(level [lfolders & {:keys [top?]}]
              (into [:ul {:class (if top? "top-level-list" "list")}]
                    (map-indexed
                     (fn [i [title {:keys [folders empty?]}]]
                       (let [last? (= (inc i) (count lfolders))]
                         [:li.node-container {:class [(when-not last? "node-middle")
                                                      (when empty? "node-empty")]}
                          [:div.node-parent
                           [:div.node
                            [:svg.icon.symbol-icon.icon-folder-open
                             [:use {:xlinkHref "#icon-folder-open"}]]
                            [:span.text title]]]
                          (when (not-empty folders)
                            [level folders])]))
                     lfolders)))]
      [:div.organize-tree
       [level (:folders tree) :top? true]])))

(defn main
  "Main function for creating an organize element."
  [state]
  (let [lists @(subscribe [:lists/authorized-lists])]
    (when (not= (:hash @state) (hash lists))
      ;; Lists are not consistent with tree. Rebuild!
      (swap! state assoc
             :hash (hash lists)
             :tree (lists->tree lists)))
    (-> [:div.organize-tree (cancel-drop-events state)]
        (into [[level state [] (:tree @state) :top? true]])
        (into [[new-folder state]]))))
