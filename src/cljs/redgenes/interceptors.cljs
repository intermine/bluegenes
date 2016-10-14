(ns redgenes.interceptors
  (:require [re-frame.core :as re-frame]
            [clojure.spec :as s]))

; (defmacro qwe [f v] `(fn [~v] (~f v)))

(defn abort-spec [spec]
  (re-frame.core/->interceptor
    :id :stopper
    :before (fn [context]
              (let [[_ data] (get-in context [:coeffects :event])]
                (if-not (s/valid? spec data)
                  (do
                    (throw (s/explain-str spec data))
                    (-> context
                        (dissoc :queue)
                        (re-frame.core/enqueue [])))
                  context)))))

