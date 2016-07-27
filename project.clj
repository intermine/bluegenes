(defproject re-frame-boiler "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [reagent "0.6.0-rc"]
                 [binaryage/devtools "0.6.1"]
                 [re-frame "0.8.0-alpha4"]
                 [secretary "1.2.3"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.4.0"]
                 [json-html "0.4.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-less "1.7.5"]
            [lein-bower "0.5.1"]
            [lein-npm "0.6.2"]]

  :bower-dependencies [[bootstrap "3.3.6"]]

  :npm {:dependencies [[bootswatch "3.3.6"]]}

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler re-frame-boiler.handler/dev-handler}

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
     :figwheel     {:on-jsload "re-frame-boiler.core/mount-root"}
     :compiler     {:main                 re-frame-boiler.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            re-frame-boiler.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/compiled/test.js"
                    :main          re-frame-boiler.runner
                    :optimizations :none}}
    ]}

  :main re-frame-boiler.server

  :aot [re-frame-boiler.server]

  :prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
