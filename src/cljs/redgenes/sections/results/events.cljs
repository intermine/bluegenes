(ns redgenes.sections.results.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as filters]
            [redgenes.sections.saveddata.events :as sd]))

(reg-event-db
  :results/set-query
  (fn [db [_ query]]
    (assoc-in db [:results :query] query)))