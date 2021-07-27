(ns bluegenes.index
  (:require [hiccup.page :refer [include-js include-css html5]]
            [taoensso.timbre :as timbre :refer [warnf]]
            [config.core :refer [env]]
            [cheshire.core :refer [generate-string]]
            [bluegenes.utils :as utils]
            [imcljs.fetch :as im-fetch]
            [clj-http.client :refer [with-middleware]]
            [clojure.core.cache.wrapped :as cache]
            [clojure.string :as str]))

;; Hello dear maker of the internet. You want to edit *this* file for prod,
;; NOT resources/public/index.html.

(def bundle-path (-> (utils/read-fingerprints) (utils/get-bundle-path)))
(def bundle-hash (utils/parse-bundle-hash bundle-path))

(def bluegenes-css (cond-> "/css/site.css"
                     (not= bundle-hash "dev") (utils/insert-filename-css bundle-hash)))
(def im-tables-css (cond-> "/css/im-tables.css"
                     (not= bundle-hash "dev") (utils/insert-filename-css bundle-hash)))

(defn escape-quotes [s]
  (str/replace s #"\"" (str/re-quote-replacement "\\\"")))

; A pure CSS loading animation to be displayed before the bluegenes javascript is read:
(def loader-style
  [:style
   "#wrappy{position:fixed;background-color:#f7f7f7;z-index:9999;display:flex;justify-content:center;align-items:center;height:100%;width:100%;flex-direction:column;font-family:sans-serif;font-size:2em;color:#999}#loader{display:flex;align-items:center;justify-content:center;margin-left:-20px;margin-top:60px;}.loader-organism{width:40px;height:0;display:block;border:12px solid #eee;border-radius:20px;opacity:.75;margin-right:-24px;animation-timing-function:ease-in;position:relative;animation-duration:2.8s;animation-name:pulse;animation-iteration-count:infinite}.worm{animation-delay:.2s}.zebra{animation-delay:.4s}.human{animation-delay:.6s}.yeast{animation-delay:.8s}.rat{animation-delay:1s}.mouse{animation-delay:1.2s}.fly{animation-delay:1.4s}@keyframes pulse{0%,100%{border-color:#3f51b5}15%{border-color:#9c27b0}30%{border-color:#e91e63}45%{border-color:#ff5722}60%{border-color:#ffc107}75%{border-color:#8bc34a}90%{border-color:#00bcd4}}\n    "])

(def css-compiling-style
  [:style
   "#csscompiling{position:fixed;bottom:0;right:0;padding:20px;height:100px;width:400px;background-color:#FFA726;}"])

(defonce smhp-cache
  ^{:doc
    "Semantic markup homepage cache. You'll have to restart BlueGenes to clear
    this when updating the semantic markup (which should be a manual process and
    happen rarely). If you're wondering why we're not using core.memoize instead,
    it's because that doesn't handle exceptions well."}
  (cache/lru-cache-factory {} :threshold 32))

(defn fetch-semantic-markup
  "Run an HTTP request to fetch semantic markup to be included with the HTML.
  As this blocks the initial HTTP response, we use a cache and a short timeout."
  [{:keys [semantic-markup object-id] {:keys [root]} :mine :as _options}]
  (let [service {:root root}]
    (try
      (with-middleware [#'clj-http.client/wrap-request ; default middleware
                        #'utils/wrap-timeout] ; very short request timeout
        (case semantic-markup
          :home (cache/lookup-or-miss smhp-cache service #(im-fetch/semantic-markup % "homepage"))
          :report (im-fetch/semantic-markup service "reportpage" {:id object-id})))
      (catch Exception e
        (warnf "Failed to acquire semantic markup for %s: %s"
               (str (name semantic-markup) object-id)
               (pr-str (.getMessage e)))
        nil))))

(defn fetch-rdf-link
  "Run an HTTP request to fetch a URL to be included with the HTML.
  As this blocks the initial HTTP response, we use a short timeout."
  [{:keys [object-id] {:keys [root]} :mine :as _options}]
  (let [service {:root root}]
    (try
      (with-middleware [#'clj-http.client/wrap-request ; default middleware
                        #'utils/wrap-timeout] ; very short request timeout
        (im-fetch/permanent-url service object-id {:type "rdf"}))
      (catch Exception e
        (warnf "Failed to acquire RDF link for object id %s: %s"
               object-id
               (pr-str (.getMessage e)))
        nil))))

(defn head
  ([]
   (head nil {}))
  ([init-vars]
   (head init-vars {}))
  ([init-vars options]
   [:head
    loader-style
    css-compiling-style
    [:title "InterMine 2.0 BlueGenes"]
    (when (= (:semantic-markup options) :report)
      (when-let [rdf-url (not-empty (fetch-rdf-link options))]
        [:link {:href rdf-url :rel "alternate" :type "application/rdf+xml" :title "RDF"}]))
    (include-css "https://cdnjs.cloudflare.com/ajax/libs/gridlex/2.2.0/gridlex.min.css")
    (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
    (include-css bluegenes-css)
    (include-css im-tables-css)
    (include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css")
    (include-css "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/styles/github.min.css")
    ; Meta data:
    [:meta {:charset "utf-8"}]
    [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
    ;;outputting clj-based vars for use in the cljs:
    [:script
     (str "var serverVars="
          (let [server-vars (merge (select-keys env [:google-analytics
                                                     :bluegenes-default-service-root :bluegenes-default-mine-name :bluegenes-default-namespace
                                                     :bluegenes-additional-mines :hide-registry-mines])
                                   {:version bundle-hash})]
            (str \" (escape-quotes (pr-str server-vars)) \"))
          ";")
     (str "var initVars="
          (if (map? init-vars)
            (str \" (escape-quotes (pr-str init-vars)) \")
            "null")
          ";")]
  ; Javascript:
    ;; This favicon is dynamically served; see routes.clj.
    [:link {:href "/favicon.ico" :type "image/x-icon" :rel "shortcut icon"}]
    [:script {:src "https://cdn.intermine.org/js/intermine/imjs/latest/im.min.js"}]
    [:script {:crossorigin "anonymous"
              :integrity "sha256-cCueBR6CsyA4/9szpPfrX3s49M9vUU5BgtiJj06wt/s="
              :src "https://code.jquery.com/jquery-3.1.0.min.js"}]
    [:script {:crossorigin "anonymous"
              :src "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.7/js/bootstrap.min.js"}]
    [:script {:src "https://apis.google.com/js/api.js"}]
    ;; Graphing library
    [:script {:src "https://cdn.jsdelivr.net/npm/vega@5.20.2"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/vega-lite@5.1.0"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/vega-embed@6.17.0"}]
    (when (:semantic-markup options)
      [:script {:type "application/ld+json"}
       (generate-string (fetch-semantic-markup options))])]))

(defn loader []
  [:div#wrappy
   [:div "LOADING INTERMINE"]
   [:div#loader
    [:div.worm.loader-organism]
    [:div.zebra.loader-organism]
    [:div.human.loader-organism]
    [:div.yeast.loader-organism]
    [:div.rat.loader-organism]
    [:div.mouse.loader-organism]
    [:div.fly.loader-organism]]])

(defn css-compiler []
  [:div#csscompiling

   [:div.alert.alert-danger
    [:h3 "Debug: Stylesheets not compiled"]
    [:p "This page is missing its stylesheet. Please tell your administrator to run <b>'lein less once'</b>."]
    [:div.clearfix]]])

(defn index
  "Hiccup markup that generates the landing page and loads the necessary assets."
  ([]
   (index nil {}))
  ([init-vars]
   (index init-vars {}))
  ([init-vars options]
   (html5
    (head init-vars options)
    [:body
     (css-compiler)
     (loader)
     [:div#app]
     [:script {:src bundle-path}]
     ;; Call the constructor of the bluegenes client and pass in the user's
     ;; optional identity as an object.
     [:script "bluegenes.core.init();"]])))
