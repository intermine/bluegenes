(ns bluegenes.ws.rss
  "Our Wordpress blog doesn't allow CORS, so we need to use the backend as a
  proxy to fetch the RSS feed. Since we're already doing this, we might as well
  parse the RSS here to reduce the payload back to the frontend."
  (:require [compojure.core :refer [defroutes GET]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.util.http-response :as response]
            [ring.util.http-predicates :as predicates]
            [clj-http.client :as client]
            [clojure.data.xml :as xml]
            [clojure.zip :as z]))

(defn find-items
  "Returns a seq of item nodes for a zipper location."
  [loc]
  (cond
    (z/end? loc) nil
    (= :item (:tag (z/node loc))) (cons (z/node loc)
                                        (filter map? (z/rights loc)))
    :else (recur (z/next loc))))

(defn find-tags
  "Returns a map from tags to values for a zipper location."
  ([loc tags] (find-tags loc tags {}))
  ([loc tags m]
   (cond
     (z/end? loc) m
     (empty? tags) m
     (contains? tags (:tag (z/node loc)))
     (let [{:keys [tag content]} (z/node loc)]
       (recur (z/right loc) (disj tags tag) (assoc m tag (first content))))
     :else (recur (z/next loc) tags m))))

(defn parse-rss [xml-feed]
  (->> (-> xml-feed xml/parse-str z/xml-zip find-items)
       (map z/xml-zip)
       (map #(find-tags % #{:title :link :pubDate :description}))))

(defn fetch-rss [req]
  (if-let [url (get-in req [:params :url])]
    (let [res (client/get url)]
      (if (predicates/ok? res)
        (try
          (if-let [parsed-items (not-empty (parse-rss (:body res)))]
            (response/ok parsed-items)
            (response/bad-request "Failed to parse RSS feed."))
          (catch Exception e
            (response/bad-request
             (str "Error occured when parsing RSS feed: " (.getMessage e)))))
        res))
    (response/bad-request "Please pass a `url` query parameter pointing to an RSS feed.")))

(defroutes routes
  (wrap-params
   (wrap-keyword-params
    (GET "/parse" req fetch-rss))))
