(ns redgenes.api.modelcountcacher
  (:require [clj-http.client :as client]
            [org.httpkit.client :as http]
    [redgenes.whitelist :as config]
))

(defn count-query [path]
    ;(str "%3Cquery%20model%3D%22genomic%22%20view%3D%22" path ".id%22%20%3E%3C%2Fquery%3E")
    (str "<query model=\"genomic\" view=\"" path ".id\" ></query>")
  )

(defn get-count [item mine]
  (println item mine (count-query (name item)))
   (let [count (client/post (str mine "/query/results")
     {:form-params
       {:format "count"
        :query (count-query (name item))}

      ;:client-params {"format" "json"
      ;                "query" (count-query (name item))}
      })]
     (println (:body count))
   )
  )

(defn load-model []
  (let [mine "http://beta.flymine.org/query/service"
        model (client/get (str mine "/model?format=json") {:keywordize-keys? true :as :json})
        whitelisted-model (select-keys (:classes (:model (:body model))) config/whitelist)
        thingy (map (fn [x y] (println x y)) (keys whitelisted-model))]
    (println "Jordan's alive! whitelisted-model" (keys whitelisted-model) )
    (doall (map #(get-count % mine) (keys whitelisted-model)))
;    (doall (map println (keys whitelisted-model)))
;    (doall (map (fn [x y] (println x y)) (keys whitelisted-model)))
    model
  ))
