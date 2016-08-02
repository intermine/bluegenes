(ns imjs.search
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [imjs.utils :as utils]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn quicksearch
  "Returns the results of a quicksearch"
  [{root :root token :token} term]
  (let [root (utils/cleanse-url root)]
    (go (:results (:body (<! (http/get (str root "/search")
                                       {:query-params      {:q    term
                                                            :size 5}
                                        :with-credentials? false})))))))