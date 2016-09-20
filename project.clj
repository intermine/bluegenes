(defproject redgenes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [figwheel-sidecar "0.5.4-7"]
                 [clj-http "2.3.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [reagent "0.6.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.3.1-0"]
                 [binaryage/devtools "0.8.1"]
                 [re-frame "0.8.0"]
                 [secretary "1.2.3"]
                 [lein-cljsbuild "1.1.4"]
                 [compojure "1.5.1"]
                 [yogthos/config "0.8"]
                 [ring-jetty-component "0.3.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring "1.5.0"]
                 [json-html "0.4.0"]
                 [prismatic/dommy "1.1.0"]
                 [cljs-ajax "0.5.8"]
                 [com.stuartsierra/component "0.3.1"]
                 [day8.re-frame/http-fx "0.0.4"]
                 [org.clojure/core.async "0.2.391"]
                 [cljs-http "0.1.41"]
                 [venantius/accountant "0.1.7"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [com.rpl/specter "0.13.0"]
                 [servant "0.1.5"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.taoensso/carmine "2.14.0"]
                 [inflections "0.12.2"]
                 [fipp "0.6.6"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-less "1.7.5"]
            [lein-shell "0.5.0"]
            [lein-cljfmt "0.5.5"]]

  :aliases {"foreign" ["do"
                       ["shell" "curl" "-o" "resources/public/vendor/imtables.js" "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/imtables.min.js"]
                       ["shell" "curl" "-o" "resources/public/vendor/im.min.js" "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"]]}


  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs     ["resources/public/css"]
             :ring-handler redgenes.handler/dev-handler}

  :less {:source-paths ["less"]
         :target-path  "resources/public/css"}

  :profiles
  {:dev
   {:dependencies []

    :plugins      [[lein-figwheel "0.5.4-7"]
                   [lein-doo "0.1.6"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "redgenes.core/mount-root"}
     :compiler     {:main                 redgenes.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    ;:foreign-libs [{:file "resources/public/vendor/im.min.js"
                    ;                :provides ["intermine.imjs"]}
                    ;               {:file "resources/public/vendor/imtables.js"
                    ;                :provides ["intermine.imtables"]}]
                    }}

    {:id           "min"
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
    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/compiled/test.js"
                    :main          redgenes.runner
                    :optimizations :none}}
    ]}

  :main redgenes.server

  :aot [redgenes.server]

  ;:prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
