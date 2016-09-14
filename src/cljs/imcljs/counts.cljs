(ns imcljs.counts
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [cljs-http.client :as http]
            [imcljs.utils :refer [cleanse-url]])
)

(defn count-rows
  "Counts the number of a certain datatype given a suitable path for the query"
  [{root :root token :token} path]
  (go (:body (<! (http/post (str (cleanse-url root) "/query/results")
                                   {:with-credentials? false
                                    :form-params {:format "count"
                                                  :query (str "<query model=\"genomic\" view=\"" path "\"></query>")}})))))
