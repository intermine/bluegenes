(ns bluegenes.pages.lists.utils
  (:require [clojure.string :as str]))

;; TODO add docstrings and unit tests

(def path-tag-prefix "bgpath")

(defn path-prefix? [s]
  (str/starts-with? s (str path-tag-prefix ":")))

(defn internal-tag? [tag]
  (str/includes? tag ":"))

(defn split-path [path]
  (str/split path #"[\.:]"))

(defn join-path [pathv]
  (str path-tag-prefix ":" (str/join "." pathv)))

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

(defn nesting [path]
  (count (re-seq #"\." path)))

(defn descendant-of? [parent child]
  (str/starts-with? child (str parent ".")))

(defn child-of? [parent child]
  (and (descendant-of? parent child)
       (= (nesting parent) (dec (nesting child)))))

(comment
  (child-of? "bgpath:foo.bar" "bgpath:foo.bar")
  (child-of? "bgpath:foo.bar" "bgpath:foo.bar.baz")
  (child-of? "bgpath:foo.bar" "bgpath:foo.bar.baz.boz")
  (descendant-of? "bgpath:foo.bar" "bgpath:foo.bar.baz.boz")
  (nesting "bgpath:foo.bar.baz"))

(defn newest-descendant-list [path->lists path]
  (->> path->lists
       (filter (comp (some-fn (partial = path)
                              (partial descendant-of? path))
                     key))
       (mapcat val)
       (sort-by :timestamp >)
       (first)))

(defn ensure-subpaths
  "If there are nested paths, where a higher level of nesting don't have lists,
  that level won't be visible as a tag (e.g. `foo.bar` has lists but not `foo`).
  This goes through all paths and add any subpaths that are missing."
  [paths]
  (distinct
   (mapcat (fn [path]
             (let [p (split-path path)]
               (map (comp join-path #(subvec p 1 %))
                    (range 2 (inc (count p))))))
           paths)))

(comment
  (re-seq #"(bgpath:.+\.)+" "bgpath:foo.bar.baz")
  (let [s (split-path "bgpath:foo.bar.baz")]
    (map (comp join-path #(subvec s 1 %)) (range 2 (inc (count s)))))
  (ensure-subpaths '("bgpath:foo.bar" "bgpath:bar.bar")))

(defn denormalize-lists [lists]
  (let [id->list (zipmap (map :id lists) lists)
        folder-paths (->> (mapcat :tags lists)
                          (distinct)
                          (filter path-prefix?)
                          (ensure-subpaths))
        path->lists (->> (filter (complement top-level-list?) lists)
                         (group-by list->path))]
    (reduce (fn [id->list+folder path]
              (let [list-children (path->lists path)
                    child-list-ids (map :id list-children)
                    child-paths (filter (partial child-of? path) folder-paths)
                    children (concat child-list-ids child-paths)
                    {:keys [authorized dateCreated timestamp]}
                    (newest-descendant-list path->lists path)]
                (assoc id->list+folder path
                       {:id path
                        :path path ; Main way to identify a folder is the presence of this key.
                        :title (path-title path)
                        :size (count children)
                        :authorized authorized
                        :dateCreated dateCreated
                        :timestamp timestamp
                        :children children})))
            id->list
            folder-paths)))

(defn folder? [folderm]
  (contains? folderm :path))

(defn reduce-folders
  "Walk through `items` adding children after folders that are expanded and
  stripping folders without children. A folder can only exist if it has
  children, so if there are no children, they have likely become filtered away,
  in which case there's no point in showing this folder. Also adds an
  `:is-last` key to each item to mark when a folder ends."
  [expanded-paths expand-children items & [top-level?]]
  (reduce (fn [items [{:keys [path children] :as item} last-child?]]
            (let [item?last (cond-> item
                              (and last-child? (not top-level?))
                              (assoc :is-last true))]
              (if (folder? item)
                (let [expanded (expand-children children)]
                  (cond
                    (and (contains? expanded-paths path)
                         (seq expanded)) (into items (cons item expanded))
                    (seq expanded)       (conj items item?last)
                    :else                items))
                (conj items item?last))))
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
     #_(take 20) ;; TODO implement pagination
     (sortf
      (concat (filterf (filter top-level-list? top-level-maps))
              (filter top-level-folder? top-level-maps)))
     true)))

(comment
  (require '[re-frame.core :refer [subscribe]])
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{}})
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{"bgpath:my folder.nested folder"}})
  (normalize-lists identity identity {:by-id @(subscribe [:lists/by-id]) :expanded-paths #{"bgpath:my folder"}}))
