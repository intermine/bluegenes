(ns redgenes.main)

(enable-console-print!)

(defn ^:export init []
  (println "hi!")
  (let [
        e (.createElement js/document "div")
        t (.createTextNode js/document "hi there!!!")
        t (.. e (appendChild t))
        ]
      (.. js/document (getElementById "app") (appendChild e))))


; (cljs/build "src/cljs" (get-in (into {} (map vec (partition 2 (rest (read-string (slurp "project.clj")))))) [:cljsbuild :builds :modules :compiler]))