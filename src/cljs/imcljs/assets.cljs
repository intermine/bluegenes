(ns imcljs.assets
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [cljs-http.client :as http]
            [imcljs.utils :as utils :refer [cleanse-url]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn templates
  "Returns a list of templates available on the server"
  [{root :root token :token}]
  (go (:templates (:body (<! (http/get (str @(subscribe [:mine-url]) "/service/templates")
                                       {:query-params      {:format "json"}
                                        :with-credentials? false}))))))

(defn lists
  "Returns the public lists available on the server and any private lists associated with a logged in token"
  [{root :root token :token}]
  (go (:lists (:body (<! (http/get (str (cleanse-url root) "/lists")
                                   {:query-params      {:format "json"}
                                    :with-credentials? false}))))))

(defn model
  "Returns the InterMine data model for the given mine"
  [{root :root token :token}]
  (go (:classes (:model (:body (<! (http/get (str (cleanse-url root) "/model")
                                    {:query-params      {:format "json"}
                                     :with-credentials? false})))))))

(defn summary-fields
  "Returns the summary fields associated with the model"
  [{root :root token :token}]
  (go (:classes (:body (<! (http/get (str (cleanse-url root) "/summaryfields")
                                   {:with-credentials? false}))))))
