(def version
  "This is Git release tag like 'v0.10.0-5-gb2dae83e'."
  (-> (clojure.java.shell/sh "git" "describe" "--always")
      :out
      clojure.string/trim))

(defproject org.intermine/bluegenes "1.2.1"
  :licence "LGPL-2.1-only"
  :description "Bluegenes is a Clojure-powered user interface for InterMine, the biological data warehouse"
  :url "http://www.intermine.org"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "1.0.567"]
                 [org.clojure/core.cache "0.8.2"]

                 ; MVC
                 [re-frame "0.12.0"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [day8.re-frame/forward-events-fx "0.0.6"]
                 [day8.re-frame/undo "0.3.3"]
                 [reagent "0.10.0"]
                 [cljsjs/react-transition-group "1.2.0-0"
                  :exclusions [cljsjs/react cljsjs/react-dom]]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "1.1.0"]
                 [metosin/reitit "0.5.12"]
                 [json-html "0.4.7"]
                 [markdown-to-hiccup "0.6.2"]
                 [cljsjs/react-day-picker "7.3.0-1"]
                 [cljsjs/react-select "2.4.4-0"]

                 ; HTTP
                 [clj-http "3.10.0"]
                 [cljs-http "0.1.46"]
                 [compojure "1.6.2"]
                 [ring "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [cheshire "5.10.0"]
                 [metosin/ring-http-response "0.9.1"]
                 [metosin/muuntaja "0.6.7"]

                 ; Build tools
                 [yogthos/config "1.1.7"]

                 ; Utility libraries
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [inflections "0.13.2"]
                 [binaryage/oops "0.7.0"]
                 [inflections "0.13.2"]
                 [cljsjs/google-analytics "2017.09.21-0"]
                 [day8.re-frame/test "0.1.5"]
                 [cljs-bean "1.5.0"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [lambdaisland/uri "1.2.1"]

                 ; Logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.19"]

                 ; Intermine Assets
                 [org.intermine/imcljs "1.4.5"]
                 [org.intermine/im-tables "0.14.0"]
                 [org.intermine/bluegenes-tool-store "0.2.2"]]

  :deploy-repositories {"clojars" {:sign-releases false}}
  :codox {:language :clojurescript}
  :plugins [[lein-cljsbuild "1.1.7"]
            [org.clojure/core.unify "0.5.7"]
            [lein-codox "0.10.5"]
            [lein-ancient "0.6.15"]
            [lein-pdo "0.1.1"]
            [lein-cljfmt "0.6.1"]
            [lein-shell "0.5.0"]
            ;; Populates .lein-env with a profile's :env map,
            ;; so they're accessible to config.
            [lein-environ "1.2.0"]]

  :cljfmt {:indents {wait-for [[:inner 0]]
                     cond-> [[:inner 0]]
                     cond->> [[:inner 0]]}}

  :aliases ~(let [compile-less ["npx" "lessc" "less/site.less" "resources/public/css/site.css"]
                  compile-less-prod (conj compile-less "-x")
                  watch-less ["npx" "chokidar" "less/**/*.less" "-c" (clojure.string/join " " compile-less) "--initial"]
                  watch-less-silent (conj watch-less "--silent")]
              {"assets" ["run" "-m" "bluegenes.prep/prepare-assets"]
               "fingerprint-css" ["run" "-m" "bluegenes.prep/fingerprint-css"]
               "dev" ["do" "clean," "assets,"
                      ["pdo"
                       (into ["shell"] watch-less-silent)
                       ["with-profile" "+repl" "run"]]]
               "build" ["do" "clean," "assets,"
                        (into ["shell"] compile-less-prod)
                        ["with-profile" "prod" "cljsbuild" "once" "min"]
                        "fingerprint-css"]
               "prod" ["do" "build,"
                       ["with-profile" "prod" "run"]]
               "biotestmine" ["do" "build,"
                              ["with-profile" "biotestmine" "run"]]
               "deploy" ["with-profile" "+uberjar" "deploy" "clojars"]
               "format" ["cljfmt" "fix"]
               "kaocha" ["with-profile" "kaocha" "run" "-m" "kaocha.runner"]
               "tools" ["run" "-m" "bluegenes-tool-store.tools"]
               "less" ["do" "assets,"
                       (into ["shell"] watch-less)]})

  :min-lein-version "2.8.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "src/workers" "script/"]

  :test-paths ["test/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "out" "resources/public/css"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             ;:ring-handler bluegenes.handler/handler
             :reload-clj-files {:cljc true}}

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                 :init (-main)
                 :timeout 120000}

  :profiles {:dev {:dependencies [[binaryage/devtools "1.0.0"]
                                  [day8.re-frame/re-frame-10x "0.6.0"]
                                  [day8.re-frame/tracing "0.5.3"]
                                  [figwheel-sidecar "0.5.19"]
                                  [cider/piggieback "0.4.2"]]
                   :resource-paths ^:replace ["config/dev" "config/defaults" "resources"]
                   :plugins [[lein-figwheel "0.5.19"]
                             [lein-doo "0.1.8"]]
                   :env {:development true}}
             :kaocha {:dependencies [[lambdaisland/kaocha "1.0.700"]
                                     [lambdaisland/kaocha-cljs "0.0-71"]]}
             :repl {:source-paths ["dev"]}
             :prod {:resource-paths ^:replace ["config/prod" "config/defaults" "resources"]}
             :uberjar {:resource-paths ^:replace ["config/defaults" "resources"]
                       :prep-tasks ["build" "compile"]
                       :aot :all}
             :java9 {:jvm-opts ["--add-modules" "java.xml.bind"]}
             :biotestmine {;; We'd prefer to have resource path config/defaults here, but for some reason that makes the envvars below not apply.
                           :env {:bluegenes-default-service-root "http://localhost:9999/biotestmine"
                                 :bluegenes-default-mine-name "Biotestmine"
                                 :bluegenes-default-namespace "biotestmine"
                                 :bluegenes-additional-mines [{:root "https://www.flymine.org/flymine" :name "FlyMine" :namespace "flymine"}]}}}

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
                                        :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true
                                                          "day8.re_frame.tracing.trace_enabled_QMARK_"  true}
                                        :preloads [devtools.preload
                                                   day8.re-frame-10x.preload]
                                        :external-config {:devtools/config {:features-to-install :all}}}}


                       :min {:source-paths ["src/cljs"]
                             :jar true
                             :compiler {:main bluegenes.core
                                        :output-to "resources/public/js/compiled/app.js"
                                        :fingerprint true
                                        :optimizations :advanced
                                        :closure-defines {goog.DEBUG false
                                                          "bluegenes.version.release" ~version}
                                        :pretty-print false}}}}

  :main bluegenes.core

  :uberjar-name "bluegenes.jar"

  :repositories [
                 ["clojars"
                  {:url "https://clojars.org/repo"}]]
                   ;; How often should this repository be checked for
                   ;; snapshot updates? (:daily, :always, or :never)
                   ;:update :always

  :release-tasks [["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["vcs" "push"]])
