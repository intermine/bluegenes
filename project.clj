(def props {:version "0.4.7-mymine-preview"})

(defproject bluegenes (str (:version props) "-SNAPSHOT")
  :dependencies [; Clojure
                 [org.clojure/clojure "1.9.0-beta4"]
                 [org.clojure/clojurescript "1.9.671"]
                 [org.clojure/core.async "0.3.443"]

                 ; MVC
                 [re-frame "0.9.4"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [day8.re-frame/async-flow-fx "0.0.8"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [day8.re-frame/undo "0.3.2"]
                 [reagent "0.7.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.6.1-0"]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "1.1.0"]
                 [secretary "1.2.3"]
                 [servant "0.1.5"]

                 [figwheel-sidecar "0.5.11"]

                 ; HTTP
                 [clj-http "3.6.1"]
                 [cljs-http "0.1.43"]
                 [compojure "1.6.0"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire.core]]
                 [cheshire "5.8.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [ring-middleware-format "0.7.2"]

                 ; Dev tools
                 [re-frisk "0.5.0"]

                 ; Build tools
                 [yogthos/config "0.8"]

                 ; Utility libraries
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [com.taoensso/carmine "2.16.0"]
                 [inflections "0.13.0"]
                 [fipp "0.6.9"]
                 [binaryage/oops "0.5.5"]
                 [inflections "0.13.0"]
                 [cljsjs/google-analytics "2015.04.13-0"]

                 ; Database
                 [org.clojure/java.jdbc "0.7.0"]
                 [org.postgresql/postgresql "42.1.3"]
                 [hikari-cp "1.7.6"]
                 [migratus "0.9.8"]
                 [com.layerware/hugsql "0.4.7"]
                 [postgre-types "0.0.4"]

                 ; Components
                 [mount "0.1.11"]

                 ; Logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]

                 ; Security
                 [buddy/buddy-auth "1.4.1"]
                 [buddy/buddy-sign "1.5.0"]
                 [buddy/buddy-hashers "1.2.0"]

                 [com.cemerick/friend "0.2.3"]
                 [clojusc/friend-oauth2 "0.2.0"]
                 [com.cemerick/url "0.1.1"]


                 ; Intermine Assets
                 [intermine/imcljs "0.1.36"]
                 [intermine/im-tables "0.3.3"]
                 [intermine/accountant-fragments "0.1.8"]

                 ]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.10"]
            [lein-pdo "0.1.1"]]

  :aliases {"dev" ["do" "clean"
                   ["pdo" ["figwheel" "dev"]
                    ["less" "auto"]
                    ["run"]]]
            "build" ["do" "clean"
                     ["cljsbuild" "once" "min"]
                     ["less" "once"]]
            "prod" ["do" "build" ["pdo" ["run"]]]}

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "src/workers" "script/"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler bluegenes.handler/dev-handler
             :reload-clj-files {:cljc true}}

  :less {:source-paths ["less"]
         :target-path "resources/public/css"}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.4"]]
                   :resource-paths ["config/dev"]
                   :plugins [[lein-figwheel "0.5.11"]
                             [lein-doo "0.1.7"]]}
             :prod {:dependencies []
                    :resource-paths ["config/prod"]
                    :plugins []}
             :uberjar {:resource-paths ["config/prod"]
                       :prep-tasks ["clean" ["less" "once"] ["cljsbuild" "once" "min"] "compile"]}}

  :cljsbuild {:builds {:dev {:source-paths ["src/cljs"]
                             :figwheel {:on-jsload "bluegenes.core/mount-root"}
                             :compiler {:main bluegenes.core
                                        :optimizations :none
                                        :output-to "resources/public/js/compiled/app.js"
                                        :output-dir "resources/public/js/compiled"
                                        :asset-path "js/compiled"
                                        :source-map-timestamp true
                                        :pretty-print true
                                        ;:parallel-build true
                                        :preloads [devtools.preload
                                                   re-frisk.preload]
                                        :external-config {:devtools/config {:features-to-install :all}}
                                        }}

                       :min {:source-paths ["src/cljs"]
                             :jar true
                             :compiler {:main bluegenes.core
                                        :parallel-build true
                                        :output-to "resources/public/js/compiled/app.js"
                                        ;:output-dir "resources/public/js/compiled"
                                        :optimizations :advanced
                                        :closure-defines {goog.DEBUG false}
                                        :pretty-print false}}

                       :test {:source-paths ["src/cljs" "test/cljs"]
                              :compiler {:output-to "resources/public/js/test/test.js"
                                         :output-dir "resources/public/js/test"
                                         :main bluegenes.runner
                                         :optimizations :none}}}}


  :main bluegenes.server

  :uberjar-name "bluegenes.jar"

  ;:aot [bluegenes.server]

  :repositories [
                 ["clojars"
                  {:url "https://clojars.org/repo"
                   ;; How often should this repository be checked for
                   ;; snapshot updates? (:daily, :always, or :never)
                   ;:update :always
                   }]])