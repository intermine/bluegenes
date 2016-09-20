(ns redgenes.api.modelcountcacher
  (:require [clj-http.client :as client]))


(defn load-model []
  (require '[clojure.core.async :as async :refer :all])
  (client/get "http://beta.flymine.org/query/service/model?format=json" {:keywordize-keys? true :as :json}))
