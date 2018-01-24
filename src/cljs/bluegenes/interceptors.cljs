(ns bluegenes.interceptors
  "Custom interceptors for BlueGenes. To learn more about interceptors visit
  https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md"
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [clojure.spec.alpha :as s]
            [oops.core :refer [ocall oget]]))

(defn abort-spec
  "Provides a re-frame interceptor that accepts a Clojure spec. When the event fires it uses the provided spec
  to validate the first value proceeding the event name in the event vector. If the spec does not validate
  the data then the event is rolled back.
  Note: I wrote this as an experiment to combine spec / interceptors but it's actually useful because it enqueues
  events rather than letting them fail. A good use case would be: dispatching an event that runs a query. If the query
  doesn't match the query spec then the event is rolled back, and when used in combination with an Undo effect you won't
  need to manage an extra 'bad' dispatch."
  [spec]
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

(defn clear-tooltips
  "This interceptor is an example of something you do when you want to beat Bootstrap with a hammer.
  It will look for any popovers in the DOM and force them to close. Sticky popovers
  are a common problem for us because Bootstrap doesn't know when a React component that triggered
  a popover unmounts."
  []
  (re-frame.core/->interceptor
    :id :clear-tooltips
    :after (fn [context] (ocall (js/$ ".popover") "remove") context)))

