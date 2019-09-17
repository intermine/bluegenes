(def props {:version "0.9.10"})

(defproject org.intermine/bluegenes (:version props)
  :licence "LGPL-2.1-only"
  :description "Bluegenes is a Clojure-powered user interface for InterMine, the biological data warehouse"
  :url "http://www.intermine.org"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.500"]

                 ; MVC
                 [re-frame "0.10.8"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [day8.re-frame/forward-events-fx "0.0.6"]
                 [day8.re-frame/undo "0.3.2"]
                 [reagent "0.8.1"]
                 [cljsjs/react-transition-group "1.2.0-0"
                  :exclusions [cljsjs/react cljsjs/react-dom]]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "1.1.0"]
                 [metosin/reitit "0.3.9"]
                 [servant "0.1.5"]
                 [json-html "0.4.5"]
                 [markdown-to-hiccup "0.6.2"]

                 ; HTTP
                 [clj-http "3.10.0"]
                 [cljs-http "0.1.46"]
                 [compojure "1.6.1"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire.core]]
                 [cheshire "5.8.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [ring-middleware-format "0.7.4"]

                 ; Build tools
                 [yogthos/config "0.9"]

                 ; Utility libraries
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/carmine "2.19.1"]
                 [inflections "0.13.2"]
                 [fipp "0.6.18"]
                 [binaryage/oops "0.7.0"]
                 [inflections "0.13.2"]
                 [cljsjs/google-analytics "2015.04.13-0"]

                 ; Logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.14"]

                 ; Security
                 [buddy/buddy-auth "2.2.0"]
                 [buddy/buddy-sign "2.2.0"]
                 [buddy/buddy-hashers "1.4.0"]

                 [com.cemerick/friend "0.2.3"]
                 [clojusc/friend-oauth2 "0.2.0"]
                 [lambdaisland/uri "1.1.0"]


                 ; Intermine Assets
                 [org.intermine/im-tables "0.8.3"]
                 [org.intermine/imcljs "0.7.0"]
                 [org.intermine/bluegenes-tool-store "0.2.0-ALPHA-1"]]

  :deploy-repositories {"clojars" {:sign-releases false}}
  :codox {:language :clojurescript}
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-less "1.7.5"]
            [org.clojure/core.unify "0.5.7"]
            [lein-codox "0.10.5"]
            [lein-ancient "0.6.15"]
            [lein-pdo "0.1.1"]
            [lein-cljfmt "0.6.1"]]

  :aliases {"dev" ["do" "clean"
                   ["pdo" ["figwheel" "dev"]
                    ["less" "auto"]
                    ["run"]]]
            "build" ["do" "clean"
                     ["cljsbuild" "once" "min"]
                     ["less" "once"]]
            "prod" ["do" "build" ["pdo" ["run"]]]
            "format" ["cljfmt" "fix"]}

  :min-lein-version "2.8.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "src/workers" "script/"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             ;:ring-handler bluegenes.handler/handler
             :reload-clj-files {:cljc true}}

  :less {:source-paths ["less"]
         :target-path "resources/public/css"}

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [day8.re-frame/re-frame-10x "0.4.2"]
                                  [figwheel-sidecar "0.5.19"]
                                  [cider/piggieback "0.4.1"]]
                   :resource-paths ["config/dev" "tools" "config/defaults"]
                   :plugins [[lein-figwheel "0.5.19"]
                             [lein-doo "0.1.8"]]}
             :repl {:source-paths ["env/dev"]}
             :prod {:dependencies []
                    :resource-paths ["config/prod" "tools"  "config/defaults"]
                    :plugins []}
             :uberjar {:resource-paths ["config/prod" "config/defaults"]
                       :prep-tasks ["clean" ["less" "once"] ["cljsbuild" "once" "min"] "compile"]
                       :aot :all}
             :java9 {  :jvm-opts ["--add-modules" "java.xml.bind"]}}

  :cljsbuild {:builds {:dev {:source-paths ["src/cljs"]
                             :figwheel {:on-jsload "bluegenes.core/mount-root"}
                             :compiler {:main bluegenes.core
                                        :optimizations :none
                                        :output-to "resources/public/js/compiled/app.js"
                                        :output-dir "resources/public/js/compiled"
                                        :asset-path "/js/compiled"
                                        :source-map-timestamp true
                                        :pretty-print true
                                        ;:parallel-build true
                                        :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                                        :preloads [devtools.preload
                                                   day8.re-frame-10x.preload]
                                        :external-config {:devtools/config {:features-to-install :all}}}}


                       :min {:source-paths ["src/cljs"]
                             :jar true
                             :compiler {:main bluegenes.core
                                        :output-to "resources/public/js/compiled/app.js"
                                        ;:output-dir "resources/public/js/compiled"
                                        :optimizations :advanced
                                        :closure-defines {goog.DEBUG false
                                                          bluegenes.core/version ~(:version props)}
                                        :pretty-print false}}}}

  :main bluegenes.core

  :uberjar-name "bluegenes.jar"

  :repositories [
                 ["clojars"
                  {:url "https://clojars.org/repo"}]])
                   ;; How often should this repository be checked for
                   ;; snapshot updates? (:daily, :always, or :never)
                   ;:update :always

