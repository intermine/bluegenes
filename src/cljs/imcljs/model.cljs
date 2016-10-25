(ns imcljs.model)

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(defn descendants-of
  "Returns classes that directly extend the provided class
  TODO: Run recursively to find *all* descendents, not just direct"
  [model class]
  (reduce (fn [total [class-name class-map]]
            (if (one-of? (:extends class-map) (name class))
              (assoc total class-name class-map)
              total)) {} model))