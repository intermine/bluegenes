(ns bluegenes.pages.lists.utils
  (:require [clojure.string :as str]))

;; TODO should we implement folders on the backend?

(def path-tag-prefix "bgpath")

(defn path-prefix? [s]
  (str/starts-with? s (str path-tag-prefix ":")))

(defn split-path [path]
  (str/split path #"[\.:]"))

(defn path-title [path]
  (last (split-path path)))

(comment
  (path-title "bgpath:my folder.nested folder")
  (path-title "bgpath:my folder"))

(defn when-fn [f]
  (fn [x]
    (when (f x) x)))

(defn top-level-list? [{:keys [tags] :as _listm}]
  (if (sequential? tags)
    (every? (complement path-prefix?) tags)
    false))

(defn top-level-folder? [{:keys [path] :as _folderm}]
  (if (some? path)
    (= 2 (-> path split-path count))
    false))

(comment
  (top-level-folder? {:path "bgpath:my folder"})
  (top-level-folder? {:id "foo"})
  (top-level-list? {:path "bgpath:my folder" :tags nil}))

(defn list->path [{:keys [tags] :as _listm}]
  (some (when-fn path-prefix?) tags))

(comment
  (some #(when (path-prefix? %) %) ["foo" "bgpath:foo"])
  (require '[re-frame.core :refer [subscribe]])
  (def lists (:default @(subscribe [:lists]))))

(defn child-of? [parent child]
  (str/starts-with? child (str parent ".")))

(defn denormalize-lists [lists]
  (let [id->list (zipmap (map :id lists) lists)
        folder-paths (->> (mapcat :tags lists)
                          (set)
                          (filter path-prefix?))
        path->lists (->> (filter (complement top-level-list?) lists)
                         (group-by list->path))]
    (reduce (fn [id->list+folder path]
              (let [list-children (path->lists path)
                    child-list-ids (map :id list-children)
                    child-paths (filter (partial child-of? path) folder-paths)
                    children (concat child-list-ids child-paths)
                    newest-child-list (first (sort-by :timestamp > list-children))]
                (assoc id->list+folder path
                       {:id path
                        :path path ; Main way to identify a folder is the presence of this key.
                        :title (path-title path)
                        :size (count children)
                        :authorized (:authorized newest-child-list)
                        ;; TODO make date and timestamp recursive to nested folders
                        :dateCreated (:dateCreated newest-child-list)
                        :timestamp (:timestamp newest-child-list)
                        :children children})))
            id->list
            folder-paths)))

(defn folder? [{:keys [path] :as _folderm}]
  (some? (not-empty path)))

(defn reduce-folders [expanded-paths expand-children items & [top-level?]]
  (reduce (fn [items [{:keys [path children] :as folderm} last-child?]]
            (if (contains? expanded-paths path)
              (if-let [expanded (seq (expand-children children))]
                ;; A folder can only exist if it has children. If there are no
                ;; children, they have likely become filtered away, in which
                ;; case there's no point in showing this folder.
                (into items (cons folderm expanded))
                items)
              (conj items (cond-> folderm
                            (and last-child? (not top-level?))
                            (assoc :is-last true)))))
          []
          (map-indexed (fn [i x]
                         [x (= i (dec (count items)))])
                       items)))

(defn expand-folders [filterf sortf {:keys [by-id expanded-paths] :as denormalized} children]
  (let [children-maps (mapv by-id children)]
    (reduce-folders
     expanded-paths
     (partial expand-folders filterf sortf denormalized)
     (sortf
      (concat (filterf (filter (complement folder?) children-maps))
              (filter folder? children-maps))))))

(defn normalize-lists [filterf sortf {:keys [by-id expanded-paths] :as denormalized}]
  (let [top-level-maps (vals by-id)]
    (reduce-folders
     expanded-paths
     (partial expand-folders filterf sortf denormalized)
     (sortf
      (concat (filterf (filter top-level-list? top-level-maps))
              (filter top-level-folder? top-level-maps)))
     true)))

(comment
  (require '[re-frame.core :refer [subscribe]])
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{}})
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{"bgpath:my folder.nested folder"}})
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{"bgpath:my folder"}}))
