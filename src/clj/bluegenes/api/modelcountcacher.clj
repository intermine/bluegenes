(ns bluegenes.api.modelcountcacher
  (:require [clj-http.client :as client]
            [org.httpkit.client :as http]
            [clojure.string :refer [trim-newline]]
            [bluegenes.whitelist :as config]
            [bluegenes.mines :as mines]
            [bluegenes.redis :refer [wcar*]]
            [taoensso.carmine :as car :refer (wcar)]
))

(defn mine-name-to-url [mine-name]
  (get-in mines/mines [(keyword mine-name) :mine :service :root]))

(defn store-response [item mine thecount]
  (wcar* (car/hset (str "modelcount-" mine) item thecount))
  (println mine "(response: " item thecount ")")
)

(defn count-query
  "Builds the query for a count of a given path"
  [path]
    (str "<query model=\"genomic\" view=\"" path ".id\" ></query>")
  )

(defn get-count
"asynchronously loads the count for a given path"
  [item mine-name mine-url]
  (http/post (str "http://" mine-url "/service/query/results")
    {:form-params
     {:format "count"
      :query (count-query (name item))}}
    (fn [{:keys [status headers body error]}] ;; asynchronous response handling
      (if error
        (println "Failed, exception is " error) ;;Oh noes :(
        (store-response item mine-name (trim-newline body)) ;;success - store the stuff!
))))

(defn second-level
  "Gets the second level of ids, i.e. Gene.proteins.id. In the future this will probably need to be more recursive-ish, but babysteps. Also, the model itself is recursive so we mustn't go too far and make the world explode."
  [whitelisted-model mine-name]
  (doall (map (fn [[parent vals]]
    (let [collections (select-keys (:collections (parent whitelisted-model)) config/whitelist)
          mine-url (mine-name-to-url mine-name)]
      (doall (map (fn [[collection-name vals]]
        (let [path (str (name parent) "." (:name vals))]
          (get-count path mine-name mine-url)
      )) collections))
    )) whitelisted-model))
)

(defn load-model [mine-name]
  (let [mine-url (mine-name-to-url mine-name)
        model (client/get
               (str "http://" mine-url "/service/model?format=json")
               {:keywordize-keys? true :as :json})
        whitelisted-model (select-keys (:classes (:model (:body model))) config/whitelist)
        promises (doall (map #(get-count % mine-name mine-url) (keys whitelisted-model)))
        ]
    (second-level whitelisted-model mine-name)
    "ok"
))

(defn load-all-models
  "Loads all the model counts in the known mines.cljc file"
  []
  (let [promises
    (doall (map
      (fn [[mine-name _]]
        (println "\n loading details for" mine-name)
        (load-model (name mine-name)) "OK..") mines/mines))]
"Loading"))
