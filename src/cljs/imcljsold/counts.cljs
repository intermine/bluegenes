(ns imcljsold.counts
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [cljs-http.client :as http]
            [imcljsold.utils :refer [cleanse-url]])
)

(def databrowser-root "api/model/count")

(defn count-rows
  "Counts the number of a certain datatype given a suitable path for the query"
  [{root :root token :token} path]
  (go (:body (<! (http/get databrowser-root
                                   {:with-credentials? false
                                    :query-params {:mine "fly"
                                                   :paths path}})))))
