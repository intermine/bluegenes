(ns imjs.search
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn quicksearch [term]
  (go (:results (:body (<! (http/get "http://www.flymine.org/query/service/search"
                            {:query-params      {:q    term
                                                 :size 5}
                             :with-credentials? false}))))))


;(POST "/send-message"
;      {:params {:message "Hello World"
;                :user    "Bob"}
;       :handler handler
;       :error-handler error-handler
;       :response-format :json
;       :keywords? true})

