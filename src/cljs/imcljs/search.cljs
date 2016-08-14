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
                 (go (let [response (<! (http/post (str "http://" (:root service) "/service/query/results")
                                                   {:with-credentials? false
                                                    :form-params       (merge options {:query (.toXML q)})}))]
                       (>! c (-> response :body))
                       (close! c))))))
    c))