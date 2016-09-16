(ns redgenes.samantha
  "Samantha is here today to
  reformat some functions"
  (:require
    [clojure.zip :as zip]
    [clojure.string :as string]
    [fipp.clojure]))

(defn get-ns [filename]
  (read-string (str "(" (slurp filename) ")")))

(defn reframe-fns
  ([form]
    (filter
      (comp #{'reg-event-db 'reg-event-fx 'reg-fx} first)
      form)))

(defn reframe-form-to-fn
  "Returns a (defn..) form
  for the given (reg-event-... form

  (todo - transform (fn*) forms from #(%) macros"
  ([[reframe-fn reframe-key fn-def]]
   (let [reframe-kind (case reframe-fn
                        reg-fx :fx
                        reg-event-fx :cofx
                        reg-event-db :event)
          fn-name (symbol (str (name reframe-key)
                    (case reframe-kind
                      :fx "!"
                      :cofx "-cofx"
                      :event "")))]
    `(defn
       ~fn-name
       "Returns the x for the given y"
       {
        :reframe-key ~reframe-key
        :reframe-kind ~reframe-kind}
       ~@(rest fn-def)))))

(defn transform-fns
  ([forms]
    (map
      (fn [form]
        (cond
          ((comp #{'reg-event-db 'reg-event-fx 'reg-fx} first) form) (reframe-form-to-fn form)
          true form))
      forms)))