(ns imcljs.utils)

(defn missing-http?- [val] (not (re-find #"^https?://" val)))

(defn missing-service?- [val] (not (re-find #"/service$" val)))

(defn append- [text val] (str val text))

(defn cleanse-url
  "Ensures that a url starts with an http protocol and ends with /service"
  [url]
  (cond->> url
           (missing-http?- url) (str "http://")
           (missing-service?- url) (append- "/service")))