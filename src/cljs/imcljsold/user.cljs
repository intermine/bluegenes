(ns imcljsold.user
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.utils :refer [cleanse-url]]))


(defn session
  "Returns the summary fields associated with the model"
  [{root :root token :token}]
  (go (:token (:body (<! (http/get (str (cleanse-url root) "/session")
                            {:with-credentials? false}))))))

