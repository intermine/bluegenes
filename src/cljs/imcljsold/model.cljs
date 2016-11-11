(ns imcljsold.model)

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(defn descendant-classes
  "Returns classes that directly extend the provided class
  TODO: Run recursively to find *all* descendents, not just direct"
  [model class]
  (reduce (fn [total [class-name class-map]]
            (if (one-of? (:extends class-map) (name class))
              (let [direct-descendants (descendant-classes model class-name)]
                (merge (assoc total class-name class-map) direct-descendants))
              total)) {} model))

(defn descendant-classes-as-tree
  "Returns classes that directly extend the provided class
  TODO: Run recursively to find *all* descendents, not just direct"
  [model class]
  (reduce (fn [total [class-name class-map]]
            (if (one-of? (:extends class-map) (name class))
              (let [direct-descendants (descendant-classes-as-tree model class-name)]
                (assoc total class-name (assoc class-map :descendants direct-descendants)))
              total)) {} model))

(defn direct-descendant-classes
  "Returns classes that directly extend the provided class
  TODO: Run recursively to find *all* descendents, not just direct"
  [model class]
  (reduce (fn [total [class-name class-map]]
            (if (one-of? (:extends class-map) (name class))
              (assoc total class-name class-map)
              total)) {} model))