(def props {:version "0.4.3-alpha"})


(defproject redgenes (:version props)
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [figwheel-sidecar "0.5.8"]
                 [clj-http "3.3.0"]
                 [org.clojure/clojurescript "1.9.456"]
                 [reagent "0.6.0"]
                 [binaryage/devtools "0.8.2"]
                 [reagent "0.6.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.3.1-0"]
                 [binaryage/devtools "0.9.0"]
                 [re-frame "0.8.0"]
                 [secretary "1.2.3"]
                 [lein-cljsbuild "1.1.5"]
                 [compojure "1.5.1"]
                 [yogthos/config "0.8"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring "1.5.0"]
                 [hiccup "1.0.5"]
                 [json-html "0.4.0"]
                 [prismatic/dommy "1.1.0"]
                 [day8.re-frame/http-fx "0.1.2"]
                 [org.clojure/core.async "0.2.395"]
                 [cljs-http "0.1.42"]
                 [intermine/accountant-fragments "0.1.8"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [day8.re-frame/undo "0.3.2"]
                 [com.rpl/specter "0.13.0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [servant "0.1.5"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.taoensso/carmine "2.15.0"]
                 [inflections "0.12.2"]
                 [fipp "0.6.6"]
                 [binaryage/oops "0.5.2"]
                 [inflections "0.12.2"]
                 [intermine/imcljs "0.1.14-SNAPSHOT"]
                 [intermine/im-tables "0.1.13-SNAPSHOT"]
                 [re-frisk "0.3.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-less "1.7.5"]
            [lein-shell "0.5.0"]
            [lein-cljfmt "0.5.5"]]


  :aliases {"foreign" ["do"
                       ["shell" "curl" "-o" "resources/public/vendor/imtables.js" "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/imtables.min.js"]
                       ["shell" "curl" "-o" "resources/public/vendor/im.min.js" "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"]]}


  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "src/workers" "script/"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"
                                    "test/js"]

  :figwheel {:css-dirs         ["resources/public/css"]
             :ring-handler     redgenes.handler/dev-handler
             :reload-clj-files {:cljc true}}

  :less {:source-paths ["less"]
         :target-path  "resources/public/css"}

  :profiles
  {:dev
   {:dependencies []

    :plugins      [[lein-figwheel "0.5.8"]
                   [lein-doo "0.1.6"]]}}


  :cljsbuild
  {
   :builds
   {
    :dev
    {
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "redgenes.core/mount-root"}
     :compiler     {
                    :main                 redgenes.core
                    :optimizations        :none
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled"
                    :asset-path           "js/compiled"
                    :source-map-timestamp true
                    :pretty-print         true
                    :parallel-build       true}}
    ;:foreign-libs [{:file "resources/public/vendor/im.min.js"
    ;                :provides ["intermine.imjs"]}
    ;               {:file "resources/public/vendor/imtables.js"
    ;                :provides ["intermine.imtables"]}]

    :modules
    {
     :source-paths ["src/cljs"]
     ;:figwheel     {:on-jsload "redgenes.core/mount-root"}
     :compiler     {

                    :optimizations        :simple
                    :output-dir           "resources/public/js/modules"
                    :source-map           "resources/public/js/modules"
                    :source-map-timestamp true
                    :pretty-print         true
                    :parallel-build       true
                    ;;:preamble             ["preamble.js"]
                    :modules
                                          {
                                           :app
                                           {
                                            :output-to "resources/public/js/modules/app.js"
                                            :entries   #{"redgenes.core"}}
                                           ;;:preamble             ["preamble.js"]

                                           :query-builder
                                           {
                                            :output-to "resources/public/js/modules/qb.js"
                                            ;;:preamble             ["preamble.js"]
                                            :entries
                                                       #{
                                                         "redgenes.components.querybuilder.views.main"}}


                                           :main
                                           {
                                            :output-to "resources/public/js/modules/main.js"
                                            ;;:preamble             ["preamble.js"]
                                            :entries   #{"redgenes.main" "redgenes.modules"}}}}}

    :min
    {
     :source-paths ["src/cljs"]
     :jar          true
     :compiler     {:main            redgenes.core



                    :output-to       "resources/public/js/compiled/app.js"
                    :output-dir      "resources/public/js/min/test"
                    :externs         ["externs/imjs.js"
                                      "externs/imtables.js"]
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false
                                      redgenes.core/version ~(:version props)}
                    :pretty-print    false}}
    ;:foreign-libs [{:file "resources/public/vendor/im.min.js"
    ;                :provides ["intermine.imjs"]}
    ;               {:file "resources/public/vendor/imtables.min.js"
    ;                :provides ["intermine.imtables"]}]

    :test
    {
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/test/test.js"
                    :output-dir    "resources/public/js/test"
                    :main          redgenes.runner
                    :optimizations :none}}}}


  :main redgenes.server

  ;:aot [redgenes.server]

  ;:prep-tasks [["cljsbuild" "once" "min"] "compile"]

  :repositories [
                 ["clojars"
                  {:url    "https://clojars.org/repo"
                   ;; How often should this repository be checked for
                   ;; snapshot updates? (:daily, :always, or :never)
                   ;:update :always
                   }]])
