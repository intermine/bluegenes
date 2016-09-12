(ns redgenes.utils
  (:require
    #?(:cljs [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]])
    [com.rpl.specter :as s]
    [clojure.zip :as zip]
    [clojure.string :as string]))

(defn
  test-reset-query
  "Returns the given app state
  with the query reset"
  {:reframe-key :query-builder/reset-query
   :reframe-kind :effect}
  [db [_ count]]
  (println "reset query!!")
  (-> db
    (assoc-in [:query-builder :query] nil)
    (assoc-in [:query-builder :count] nil)))

(defn do-reg!
  ([v]
   (do-reg! @v (meta v)))
  ([f {reframe-key :reframe-key reframe-kind :reframe-kind naym :name}]
   #?(:cljs
      (case (or reframe-kind (if (and naym (string/ends-with? naym "!")) :cofx :effect))
        :effect (reg-event-db reframe-key f)
        :fx     (reg-fx reframe-key f)
        :cofx   (reg-event-fx reframe-key f))
      :clj
        (println "would register" naym reframe-key reframe-kind (if (and naym (string/ends-with? naym "!")) :cofx :effect)))))

(comment (do-reg!
   (comp test-reset-query (fn [db _] (println "something first") db))
   {:reframe-key  :query-builder/reset-query
    :reframe-kind :effect
    }))

;(do-reg! #'reset-query)

(defn get-ns [filename]
  (read-string (str "(" (slurp filename) ")")))

(defn reframe-fns
  ([form]
    (filter
      (comp #{'reg-event-db 'reg-event-fx 'reg-fx} first)
      form)))

(defn to-fn
  "Returns a (defn..) form for the given (reg-event-... form"
  ([[reframe-fn reframe-key fn-def]]
    `(defn
    ~(symbol (name reframe-key))
       "Returns the x for the given y"
       {
        :reframe-key ~reframe-key
        :reframe-kind
                     ~(case (name reframe-fn)
                        "reg-fx" :fx
                        "reg-event-fx" :cofx
                        "reg-event-db" :event)}
     ~@(rest fn-def))))