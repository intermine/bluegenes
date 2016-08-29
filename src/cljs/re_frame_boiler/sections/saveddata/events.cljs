(ns re-frame-boiler.sections.saveddata.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(reg-event-db
  :results/set-query
  (fn [db [_ query]]
    (assoc-in db [:results :query] query)))