(ns imcljsold.operations
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [imcljsold.utils :as utils :refer [cleanse-url]]
            [clojure.set :as set :refer [union intersection difference]]
            [imcljsold.search :as search]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn quicksearch
  "Returns the results of a quicksearch"
  [{root :root token :token} term]
  (let [root (utils/cleanse-url root)]
    (go (:results (:body (<! (http/get (str root "/search")
                                       {:query-params      {:q    term
                                                            :size 5}
                                        :with-credentials? false})))))))


(defn operation [service query-one query-two]
  (let [channels (map (partial search/raw-query-rows service) [query-one query-two])]
    (let [result-chan (chan)]
      (go (let [result-one (set (flatten (:results (<! (first channels)))))
                result-two (set (flatten (:results (<! (second channels)))))]
            (>! result-chan [result-one result-two])))
      result-chan)))