(ns bluegenes.time
  (:require [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]))

(def query-formatter (time-format/formatter "d MMM, HH:mm"))

(defn format-query [{:keys [last-executed] :as _query}]
  (time-format/unparse query-formatter (time-coerce/from-long last-executed)))
