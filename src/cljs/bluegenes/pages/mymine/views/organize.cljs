(ns bluegenes.pages.mymine.views.organize
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [clojure.string :as string]
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
  (string/starts-with? (string/join "." c)
                       (string/join "." p)))

(defn top-node?
  "Path `p` is a node at the top of the tree, meaning its parent is the root node."
  [p]
  (= (count p) 2))

(defn root-node?
  "Whether `p` is the path to the root node."
  [p]
  (= p []))

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
  [dragging dropping]
  (let [dropping (if (list-path? dropping)
                   (drop-last 2 dropping)
                   dropping)]
    (and dragging dropping
         (not= dragging dropping)
         (not (direct-parent-of? dropping dragging))
         (not (child-of? dropping dragging)))))

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
                      (when (valid-drop? dragging dropping)
                        (swap! state
                               (fn [{:keys [tree] :as prev-state}]
                                 (let [dropping (if (list-path? dropping)
                                                  (subvec dropping 0 (- (count dropping) 2))
                                                  dropping)]
                                   (-> prev-state
                                       (assoc :drag nil)
                                       (assoc :tree
                                              (-> tree
                                                  (assoc-in (into dropping (take-last 2 dragging))
                                                            (get-in tree dragging))
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
  [state {:keys [title icon last? on-click path fake?]} & children]
  (let [{:keys [dragging dropping]} (:drag @state)
        drag? (and (being-dragged? path dragging)
                   (not (valid-drop? dragging dropping)))
        drop? (and (being-dragged? path dragging)
                   (valid-drop? dragging dropping))]
    (into [:li {:className (str "node-container"
                                (when-not        last? " node-middle")
                                (when (or fake? drag?) " node-fake")
                                (when            drop? " node-hide"))}
           [:div.node-parent (when-not fake? (drop-events state path))
            [:div.node (when-not fake?
                         (merge (drag-events state path)
                                (when on-click {:on-click on-click})))
             [:svg {:className (str "icon icon-" icon)}
              [:use {:xlinkHref (str "#icon-" icon)}]]
             [:span.text title]
             [:span.drag-handle "☰"]]]]
          children)))

(defn root-node
  "This is a special root node for use as a drop target to move nested nodes
  into the top level."
  [state]
  [:li.node-container.node-fake
    [:div.node-parent (drop-events state [])
     [:div.node.root (drag-events state [])]]])

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
  (let [open? (r/atom true)]
    (fn [state path [title children] & {:keys [last?]}]
      [node state {:title title
                   :icon (str "folder" (when @open? "-open"))
                   :last? last?
                   :on-click #(swap! open? not)
                   :path path
                   :fake? (:fake? children)}
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
  - Top level placeholder node for when the root node is the drop target"
  [state path {:keys [lists folders]} & {:keys [top?]}]
  (-> [:ul {:className (if top? "top-level-list" "list")}]
      (into (let [items (concat folders lists
                                ;; Append a placeholder node when the user hovers over
                                ;; a valid drop target.
                                (when-let [{:keys [dragging dropping]} (:drag @state)]
                                  (when (and (being-dropped? path dropping)
                                             (valid-drop? dragging dropping)
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
                          (valid-drop? dragging dropping)
                          (root-node? dropping))
                 [(map->node state path
                             [(last dragging)
                              (assoc (get-in @state (into [:tree] dragging))
                                     :fake? true)])]))))))


;; TODO may only contain "letters, numbers, spaces, hyphens and colons.",
;; "full stops" are technically permitted but we use them for nesting
;; TODO also check that folder name doesn't already exist
(defn new-folder
  "Input and button for creating a new folder."
  [state]
  [:div.node-parent
   [:div.node.node-input
    [:svg.icon.icon-folder-plus [:use {:xlinkHref "#icon-folder-plus"}]]
    [:input.form-control
     {:type "text"
      :placeholder "New folder"
      :value (get-in @state [:inputs :folder])
      :on-change (fn [evt] (swap! state assoc-in [:inputs :folder]
                                  (oget evt :target :value)))}]
    [:button.btn.btn-slim
     {:type "button"
      :on-click #(swap! state
                        (fn [prev-state]
                          (let [input (get-in prev-state [:inputs :folder])]
                            (assoc-in prev-state [:tree :folders input]
                                      {:folders {} :lists {}}))))}
     "Add"]]])

;; TODO make sure that this is only run when modal is open (? is this really a good idea)
(defn main
  "Main function for creating an organize element."
  [state]
  (let [lists (subscribe [:lists/filtered-lists])]
    (swap! state assoc :tree (lists->tree @lists))
    (fn [state]
      (-> [:div.organize-tree (cancel-drop-events state)]
          (into [[level state [] (:tree @state) :top? true]])
          (into [[new-folder state]])))))
