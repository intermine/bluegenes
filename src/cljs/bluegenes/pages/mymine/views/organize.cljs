(ns bluegenes.pages.mymine.views.organize
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [clojure.string :as string]
            [oops.core :refer [ocall oget]]))

;;;;;;;;;;;;;;;;;;;;
;; Tree Structure ;;
;;;;;;;;;;;;;;;;;;;;

(def path-tag-prefix "bgpath")

(defn read-path [tags]
  (let [re (re-pattern (str path-tag-prefix ":(.+)"))]
    (->> tags
         (some #(re-find re %))
         second)))

;; TODO use select-keys to only grab keys you use
(defn lists->tree [lists]
  (comment
    "TODO" "Turn these examples into unit tests?"
    "Although that would require it to be defined as it's own function."

    (apply concat [nil (interpose :folders nil) [:lists]])
    (apply concat [[:folders] (interpose :folders ["tfg"]) [:lists]])
    (apply concat [[:folders] (interpose :folders ["tfg" "extra"]) [:lists]])

    {:lists ["Z7 Special"]
     :folders {"TFG" {:lists ["TFG Panel Blue"
                              "TFG Panel Multi"]
                      :folders {"more" {:lists ["TFG Panel Extra"]}}}}})

  (reduce (fn [tree {:keys [tags title] :as l}]
            (let [path (some-> (read-path tags)
                               (string/split #"\."))
                  ks (apply concat [(when path [:folders])
                                    (interpose :folders path)
                                    [:lists]])]
              (update-in tree ks assoc title l)))
          {}
          lists))

;;;;;;;;;;;
;; Logic ;;
;;;;;;;;;;;

(defn list-path? [p]
  (= (get p (- (count p) 2))
     :lists))

(defn direct-parent-of? [p c]
  (= p (drop-last 2 c)))

(defn child-of? [c p]
  (string/starts-with? (string/join "." c)
                       (string/join "." p)))

(defn top-node? [p]
  (= (count p) 2))

(defn root-node? [p]
  (= p []))

(defn being-dragged? [path dragging]
  (= path dragging))

(defn being-dropped? [path dropping]
  (or (= path dropping)
      (and (list-path? dropping)
           (= path (drop-last 2 dropping)))))

(defn valid-drop? [dragging dropping]
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
  [state item]
  {:draggable true

   :on-drag-start (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc-in [:drag :dragging] item))

   :on-drag-end   (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc :drag nil))})

(defn drop-events
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
  [state]
  {:on-drag-enter (fn [evt]
                    (ocall evt :stopPropagation)
                    (swap! state assoc-in [:drag :dropping] nil))})

;;;;;;;;;;;;;;;;;;
;; DOM Elements ;;
;;;;;;;;;;;;;;;;;;

(defn node [state {:keys [title icon last? on-click path fake?]} & children]
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

(defn root-node [state]
  [:li.node-container.node-fake
    [:div.node-parent (drop-events state [])
     [:div.node.root (drag-events state [])]]])

(defn list-node [state path [_ {:keys [title fake?]}] & {:keys [last?]}]
  [node state {:title title
               :icon "list"
               :last? last?
               :path path
               :fake? fake?}])

(declare level)
(defn folder-node []
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

(defn map->node [state path [title data] & {:keys [last?]}]
  (let [folder? (not (contains? data :tags))
        path'   (if folder?
                  (conj path :folders title)
                  (conj path :lists title))]
    ^{:key (if folder? (string/join "." (conj path title)) (:id data))}
    [(if folder? folder-node list-node)
     state path' [title data]
     :last? last?]))

(defn level [state path {:keys [lists folders]} & {:keys [top?]}]
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
(defn new-folder [state]
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
(defn main [state]
  (let [lists (subscribe [:lists/filtered-lists])]
    (swap! state assoc :tree (lists->tree @lists))
    (fn [state]
      (-> [:div.organize-tree (cancel-drop-events state)]
          (into [[level state [] (:tree @state) :top? true]])
          (into [[new-folder state]])))))
