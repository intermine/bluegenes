(ns imcljsold.idresolver
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [imcljsold.utils :refer [cleanse-url]]
            [cljs.core.async :refer [chan <! >! timeout close!]]))

(defn check-job
  "Check the status of an ID resolution job"
  [{root :root token :token} uid]
  (go (:body (<! (http/get
                   (str (cleanse-url root) "/ids/" uid "/status")
                   {:with-credentials? false})))))

(defn create-job
  "Create an ID resolution job"
  [{root :root token :token} params]
  (go (:body (<! (http/post
                   (str (cleanse-url root) "/ids")
                   {:json-params       params
                    :with-credentials? false})))))

(defn delete-job
  "Delete an ID resolution job"
  [{root :root token :token} uid]
  (go (:body (<! (http/delete
                   (str (cleanse-url root) "/ids/" uid)
                   {:with-credentials? false})))))

(defn retrieve-job
  "Retrieves the results of a an ID resolution job"
  [{root :root token :token} uid]
  (go (:body (<! (http/get
                   (str (cleanse-url root) "/ids/" uid "/results")
                   {:with-credentials? false})))))

(defn im-resolve
  "Polls intermine for resolved identifiers."
  [service params]
  (let [c (chan) job (create-job service params)]
    (go (let [uid (:uid (<! job))]
          (go-loop [ms 200]
                   (<! (timeout ms))
                   (let [status (<! (check-job service uid))]
                     (if (and (< ms 10001) (= "RUNNING" (:status status)))
                       (recur (* ms 2.5))
                       (do
                         (delete-job service uid)
                         (>! c (:results (<! (retrieve-job service uid)))))))))) c))
