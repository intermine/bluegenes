(ns bluegenes.auth
  (:require [buddy.hashers :as hs]
            [bluegenes.db.users :as queries]
            [buddy.sign.jwt :as jwt]
            [config.core :refer [env]]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [clojure.string :refer [blank?]]))

(defn fetch-token
  [{:keys [service-str username password token]}]
  (auth/basic-auth {:root service-str} username password))