(ns imcljsold.names
  (:require [re-frame.core :as re-frame :refer [subscribe]]))

(defn find-name
  "Given a collection look up the *display name* for the object" ;; I expect this to grow complexer.
  [thing-to-find-name-for]
  (let [reference-name (:referencedType thing-to-find-name-for)
        the-name (if (some? reference-name) reference-name thing-to-find-name-for)
        model (subscribe [:model])]
    (:displayName ((keyword the-name) @model))
))

(defn find-type
  "Given a collection look up the *computer name* for the object"
  ;;TODO: When this gets more grown up it should build a path and use
  ;;filters/path-type to get type more correctly but maybe really path-type should live in here too?.
  [thing-to-find-name-for]
  (let [the-name (:referencedType thing-to-find-name-for)
        model (subscribe [:model])]
    (:name ((keyword the-name) @model))
))
