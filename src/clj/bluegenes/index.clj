(ns bluegenes.index
  (:require [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]
            [cheshire.core :as json]))

;; Hello dear maker of the internet. You want to edit *this* file for prod,
;; NOT resources/public/index.html.


; *** IMPORTANT ***
; The version binding below will be nil when BlueGenes is deployed as a dependency of another project (for example: Gradle).
; This is because project.clj does not get packaged into the "skinny" jar file when BlueGenes is pushed to clojars.
; TODO: Figure out how to get BlueGenes version without relying on project.clj
(defn version [] (:version (try
                             (-> "project.clj" slurp read-string (nth 2))
                             (catch Exception e nil))))

; A pure CSS loading animation to be displayed before the bluegenes javascript is read:
(def loader-style
  [:style
   "#wrappy{display:flex;justify-content:center;align-items:center;height:90vh;width:100%;flex-direction:column;font-family:sans-serif;font-size:2em;color:#999}#loader{flex-grow:1;display:flex;align-items:center;justify-content:center}.loader-organism{width:40px;height:0;display:block;border:12px solid #eee;border-radius:20px;opacity:.75;margin-right:-24px;animation-timing-function:ease-in;position:relative;animation-duration:2.8s;animation-name:pulse;animation-iteration-count:infinite}.worm{animation-delay:.2s}.zebra{animation-delay:.4s}.human{animation-delay:.6s}.yeast{animation-delay:.8s}.rat{animation-delay:1s}.mouse{animation-delay:1.2s}.fly{animation-delay:1.4s}@keyframes pulse{0%,100%{border-color:#3f51b5}15%{border-color:#9c27b0}30%{border-color:#e91e63}45%{border-color:#ff5722}60%{border-color:#ffc107}75%{border-color:#8bc34a}90%{border-color:#00bcd4}}\n    "])

(def css-compiling-style
  [:style
   "#csscompiling{position:fixed;bottom:0;right:0;padding:20px;height:100px;width:400px;background-color:#FFA726;}"])

(defn head []
  [:head
   loader-style
   css-compiling-style
   [:title "InterMine 2.0 BlueGenes"]
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/gridlex/2.2.0/gridlex.min.css")
   (include-css "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/main.sandboxed.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
   (include-css "css/site.css")
   (include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css")
   ; Meta data:
   [:meta {:charset "utf-8"}]
   [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
   ;;outputting clj-based vars for use in the cljs:
   [:script
    "var serverVars={googleAnalytics :'" (:google-analytics env) "'"
    (cond (:bluegenes-default-service-root env)
          (str ", intermineDefaults: {"
               "serviceRoot:'"  (:bluegenes-default-service-root env) "',"
               "mineName: '" (:bluegenes-default-mine-name env)  "'"
               "}")) "};"]
  ; Javascript:
   [:link {:rel "shortcut icon" :href "https://raw.githubusercontent.com/intermine/design-materials/f5f00be4/logos/intermine/fav32x32.png" :type "image/png"}]
   [:script {:src "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"}]
   [:script {:crossorigin "anonymous"
             :integrity "sha256-cCueBR6CsyA4/9szpPfrX3s49M9vUU5BgtiJj06wt/s="
             :src "https://code.jquery.com/jquery-3.1.0.min.js"}]
   [:script {:crossorigin "anonymous"
             :src "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.7/js/bootstrap.min.js"}]
   [:script {:src "https://apis.google.com/js/api.js"}]])

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
  "Hiccup markup that generates the landing page and loads the necessary assets.
  A user might optionally have their identity already stored in a session."
  [identity]
  ; Generate a JSON representation of the user's identity (name, tokens, etc)
  (let [json-identity (json/generate-string identity)]
    (html5
     (head)
     [:body
      (css-compiler)
      (loader)
      [:div#app]
       ; Bust the cache by using the project's version number as a URL parameter
      [:script {:src (str "js/compiled/app.js?v=" (version))}]
       ; Call the constructor of the bluegenes client and pass in the user's optional identity as an object
      [:script (str "bluegenes.core.init(" json-identity

                    ");")]])))
