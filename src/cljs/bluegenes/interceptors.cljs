(ns bluegenes.interceptors
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [clojure.spec :as s]
            [oops.core :refer [ocall oget]]))

; (defmacro qwe [f v] `(fn [~v] (~f v)))

(defn abort-spec [spec]
  (re-frame.core/->interceptor
    :id :stopper
    :before (fn [context]
              (let [[_ data] (get-in context [:coeffects :event])]
                (if-not (s/valid? spec data)
                  (do
                    (throw (s/explain-str spec data))
                    ;(dispatch [:add-toast "I AM TEST"])
                    (-> context
                        (dissoc :queue)
                        (re-frame.core/enqueue [])))
                  context)))))

(defn clear-tooltips []
  (re-frame.core/->interceptor
    :id :clear-tooltips
    :after (fn [context] (ocall (js/$ ".popover") "remove") context)))

