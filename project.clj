(defproject redgenes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [figwheel-sidecar "0.5.8"]
                 [clj-http "3.3.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.0"]
                 [binaryage/devtools "0.8.2"]
                 [reagent "0.6.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.3.1-0"]
                 [binaryage/devtools "0.8.2"]
                 [re-frame "0.8.0"]
                 [secretary "1.2.3"]
                 [lein-cljsbuild "1.1.4"]
                 [compojure "1.5.1"]
                 [yogthos/config "0.8"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring "1.5.0"]
                 [json-html "0.4.0"]
                 [prismatic/dommy "1.1.0"]
                 [cljs-ajax "0.5.8"]
                 [day8.re-frame/http-fx "0.1.2"]
                 [org.clojure/core.async "0.2.395"]
                 [cljs-http "0.1.42"]
                 [venantius/accountant "0.1.7"]
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
                 [inflections "0.12.2"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-less "1.7.5"]
            [lein-shell "0.5.0"]
            [lein-cljfmt "0.5.5"]]

  :aliases {"foreign" ["do"
                       ["shell" "curl" "-o" "resources/public/vendor/imtables.js" "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/imtables.min.js"]
                       ["shell" "curl" "-o" "resources/public/vendor/im.min.js" "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"]]}


  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs     ["resources/public/css"]
             :ring-handler redgenes.handler/dev-handler
             :reload-clj-files {:cljc true}}

  :less {:source-paths ["less"]
         :target-path  "resources/public/css"}

  :profiles
  {:dev
   {:dependencies []

    :plugins      [[lein-figwheel "0.5.8"]
                   [lein-doo "0.1.6"]]
    }}

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
                    :optimizations        :whitespace
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled"
                    :asset-path           "js/compiled"
                    :source-map-timestamp true
                    :pretty-print         true
                    :parallel-build       true
                    ;:foreign-libs [{:file "resources/public/vendor/im.min.js"
                    ;                :provides ["intermine.imjs"]}
                    ;               {:file "resources/public/vendor/imtables.js"
                    ;                :provides ["intermine.imtables"]}]
                    }}
    :modules
    {
     :source-paths ["src/cljs"]
     ;:figwheel     {:on-jsload "redgenes.core/mount-root"}
     :compiler     {
                    :optimizations        :simple
                    :output-dir           "resources/public/js"
                    :source-map           true
                    :source-map-timestamp true
                    :pretty-print         true
                    :parallel-build       true
                    :modules
                                          {
                                             :app
                                             {
                                              :output-to  "resources/public/js/app.js"
                                              :entries    #{"redgenes.core"}
                                              }
                                             :query-builder
                                             {
                                              :output-to  "resources/public/js/qb.js"
                                              :entries
                                                          #{
                                                            "redgenes.components.querybuilder.views.main"
                                                            }
                                              }
                                             :main
                                             {
                                              :output-to  "resources/public/js/main.js"
                                              :entries    #{"redgenes.main"}
                                              }
                                           }
                    }}
    :modules1
    {
     :source-paths ["src/cljs"]
     ;:figwheel     {:on-jsload "redgenes.core/mount-root"}
     :compiler     {
                    :optimizations        :simple
                    :output-dir           "resources/public/js/mod"
                    :source-map           true
                    :source-map-timestamp true
                    :pretty-print         true
                    :parallel-build       true
                    :modules
                    {
                       :main
                       {
                        :output-to  "resources/public/js/mod/main.js"
                        :entries    #{"redgenes.main"}
                        }
                       :workers
                       {
                        :output-to  "resources/public/js/mod/workers.js"
                        :entries    #{"redgenes.workers"}
                        }
                     }
                    }}
    :min
    {
     :source-paths ["src/cljs"]
     :jar          true
     :compiler     {:main            redgenes.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :externs         ["externs/imjs.js"
                                      "externs/imtables.js"]
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false
                    ;:foreign-libs [{:file "resources/public/vendor/im.min.js"
                    ;                :provides ["intermine.imjs"]}
                    ;               {:file "resources/public/vendor/imtables.min.js"
                    ;                :provides ["intermine.imtables"]}]
                    }}
    :test
    {
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/test/test.js"
                    :output-dir     "resources/public/js/test"
                    :main          redgenes.runner
                    :optimizations :none}}
    }}

  :main redgenes.server

  ;:aot [redgenes.server]

  ;:prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
