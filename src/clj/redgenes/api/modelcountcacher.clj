(ns redgenes.api.modelcountcacher
  (:require [clj-http.client :as client]
            [org.httpkit.client :as http]
            [redgenes.whitelist :as config]
            [redgenes.redis :refer [wcar*]]
            [taoensso.carmine :as car :refer (wcar)]
))

(defn store-response [item mine thecount]
  (wcar* (car/hset (str "modelcount-" mine) item thecount))
  (println "response 3" item mine thecount)
)

(defn count-query [path]
    (str "<query model=\"genomic\" view=\"" path ".id\" ></query>")
  )

(defn get-count
"asynchronously loads the count for a given path"
  [item mine]
  (http/post (str mine "/query/results")
    {:form-params
     {:format "count"
      :query (count-query (name item))}}
    (fn [{:keys [status headers body error]}] ;; asynchronous response handling
      (if error
        (println "Failed, exception is " error) ;;Oh noes :(
        (store-response item mine body) ;;success - store the stuff!
))))

(defn load-model []
  (let [mine "http://beta.flymine.org/query/service"
        model (client/get (str mine "/model?format=json") {:keywordize-keys? true :as :json})
        whitelisted-model (select-keys (:classes (:model (:body model))) config/whitelist)
        promises (doall (map #(get-count % mine) (keys whitelisted-model)))
        ]
    model
))
