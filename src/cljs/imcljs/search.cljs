(ns imcljs.search
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [imcljs.utils :as utils :refer [cleanse-url]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn quicksearch
  "Returns the results of a quicksearch"
  [{root :root token :token} term]
  (let [root (utils/cleanse-url root)]
    (go (:results (:body (<! (http/get (str root "/search")
                                       {:query-params      {:q    term
                                                            :size 5}
                                        :with-credentials? false})))))))

(defn raw-query-rows
  "Returns IMJS row-style result"
  [service query options]
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js service))
        (.query (clj->js query))
        (.then (fn [q]
                 (go (let [root (utils/cleanse-url (:root service))
                           response (<! (http/post (str root "/query/results")
                                                   {:with-credentials? false
                                                    :form-params       (merge {:format "json"} options {:query (.toXML q)})}))]
                       (>! c (-> response :body))
                       (close! c))))))
    c))


(defn enrichment
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [{root :root token :token} {:keys [ids list widget maxp correction population]}]
  (go (:body (<! (http/post
                   (str (cleanse-url root) "/list/enrichment")
                   {:with-credentials? false
                    :keywordize-keys?  true
                    :form-params       (merge {:widget     widget
                                               :maxp       maxp
                                               :format     "json"
                                               :correction correction}
                                              (cond
                                                ids {:ids (clojure.string/join "," ids)}
                                                list {:list list})
                                              (if-not (nil? population) {:population population}))})))))
