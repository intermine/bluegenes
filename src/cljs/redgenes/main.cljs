(ns redgenes.main
  (:require
    [redgenes.modules :as modules]))

(enable-console-print!)

(defn ^:export init []
  (println "hi!")
  (let [
        e (.createElement js/document "div")
        t (.createTextNode js/document "hi there!!!")
        t (.. e (appendChild t))
        ]
      (.. js/document (getElementById "app") (appendChild e)))
  (modules/set-modules!
    (modules/with-modules {"app" [] "main" [] "qb" []}))
  (modules/load-module! "app"))


; (cljs/build "src/cljs" (get-in (into {} (map vec (partition 2 (rest (read-string (slurp "project.clj")))))) [:cljsbuild :builds :modules :compiler]))