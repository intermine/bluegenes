(ns bluegenes.index
  (:require [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]
            [cheshire.core :as json]))

;;Hello dear maker of the internet. You want to edit *this* file for prod, NOT the index.html copy. Why did we configure it this way? It's not to drive you mad, I assure you. It's because figwheel likes to highjack routes at / and display a default page if there is no index.html in place. Naughty figwheel!

(defn version [] (:version (-> "project.clj" slurp read-string (nth 2))))

(def loader-style
  [:style
   "#wrappy{display:flex;justify-content:center;align-items:center;height:90vh;width:100%;flex-direction:column;font-family:sans-serif;font-size:2em;color:#999}#loader{flex-grow:1;display:flex;align-items:center;justify-content:center}.loader-organism{width:40px;height:0;display:block;border:12px solid #eee;border-radius:20px;opacity:.75;margin-right:-24px;animation-timing-function:ease-in;position:relative;animation-duration:2.8s;animation-name:pulse;animation-iteration-count:infinite}.worm{animation-delay:.2s}.zebra{animation-delay:.4s}.human{animation-delay:.6s}.yeast{animation-delay:.8s}.rat{animation-delay:1s}.mouse{animation-delay:1.2s}.fly{animation-delay:1.4s}@keyframes pulse{0%,100%{border-color:#3f51b5}15%{border-color:#9c27b0}30%{border-color:#e91e63}45%{border-color:#ff5722}60%{border-color:#ffc107}75%{border-color:#8bc34a}90%{border-color:#00bcd4}}\n    "])




(defn head []
  [:head
   loader-style

    [:title "InterMine 2.0 bluegenes"]
    (include-css
      "https://cdnjs.cloudflare.com/ajax/libs/gridlex/2.2.0/gridlex.min.css")
    (include-css
      "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/main.sandboxed.css")
    (include-css "css/site.css")
    [:meta {:charset "utf-8"}]
    [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
    [:script "var serverVars={googleAnalytics :'" (:google-analytics env) "'}" ]
    [:link {:rel "shortcut icon" :href "https://cdn.rawgit.com/intermine/design-materials/f5f00be4/logos/intermine/fav32x32.png" :type "image/png"}]
    [:script {:src
      "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"}]
    [:script
     {:crossorigin "anonymous",
      :integrity "sha256-cCueBR6CsyA4/9szpPfrX3s49M9vUU5BgtiJj06wt/s=",
      :src "https://code.jquery.com/jquery-3.1.0.min.js"}]
    [:script {:src "vendor/bootstrap/dist/js/bootstrap.js"}]
    [:script {:src "vendor/bootstrap/js/tooltip.js"}]
    [:script {:src "vendor/bootstrap/js/popover.js"}]
   [:script {:src "https://apis.google.com/js/api.js"}]
  ]
)


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


(defn index [identity]
  (let [
        ;json-identity (json/generate-string identity)
        ]
    (html5
      (head)
      [:body
       (loader)
       [:div#app]
       [:script {:src (str "js/compiled/app.js?v=" (version))}]
       [:script (str "bluegenes.core.init(" json-identity ");")]
       ]
      )))
