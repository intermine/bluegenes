(ns bluegenes.utils
  "dont' like utils namespaces
  but anyway here's one"
  (:require
    #?(:cljs [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx reg-sub]])
    #?(:cljs [day8.re-frame.undo :as undo :refer [undoable]])
    [clojure.string :refer [blank? join split upper-case]]))

(defn ^:export register!
  "
  Helper for registering re-frame events
  using metadata of their vars

  Note: because CLJS only allows statically compiled metadata
  for vars, we can't pass-in fns in metadata maps for those
  vars because they're not evaluated and just end-up as lists.
  That's why if the :undo-exp is a keyword, it
  signals that the event fn includes a 3-arity implementation
  for returning a re-frame string explanation dynamically
  "
  ([v]
   (register! @v (meta v)))
  ([f {reframe-key  :reframe-key
       reframe-kind :reframe-kind
       naym         :name
       undoable?    :undoable?
       undo-exp     :undo-exp
       :or {undoable? false}}]
      ;(println ">>" (fn? undo-exp))
   #?(:cljs
      (case reframe-kind
        :event
          (if undoable?
            (reg-event-db reframe-key
              (undoable
                (if (keyword? undo-exp) (fn [db ev] (f 1 db ev)) undo-exp)) f)
            (reg-event-db reframe-key f))
        :cofx
          (if undoable?
            (reg-event-fx reframe-key
              (undoable
                (if (keyword? undo-exp) (fn [db ev] (f 1 db ev)) undo-exp)) f)
            (reg-event-fx reframe-key f))
         :fx
          (reg-fx reframe-key f))
      :clj
        (println "would register" naym reframe-key reframe-kind))))


(defn ^:export register-all!
  "Registers all the given vars"
  ([vars]
    (doseq
     [v vars]
     (register! v))))

(defn ^:export reg-all-subs!
  "Registers all the given subscriptions
  e.g. [[:constraint :query-builder/current-constraint]]"
  ([queries]
   #?(:cljs
    (doseq
      [[path kw] queries]
      (reg-sub
        (or kw (keyword (name :query-builder) (name path)))
        (fn [db _]
          (get-in db [:query-builder path]))))
          :clj (println "reg these subs" queries))))

(comment
  (register!
   (comp test-reset-query (fn [db _] (println "something first") db))
   {:reframe-key  :query-builder/reset-query
    :reframe-kind :effect}))

(def not-blank? (complement blank?))

(defn uncamel [s]
  (if (not-blank? s)
    (join (-> (split (join " " (split s #"(?=[A-Z][^A-Z])")) "")
             (update 0 upper-case)))
    s))

;(register! #'reset-query)