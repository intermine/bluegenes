(ns redgenes.main)

(comment
  (defn ^:export init []
   ;(println "hi!")
   (let [
         e (.createElement js/document "div")
         t (.createTextNode js/document "hi there!!!")
         t (.. e (appendChild t))
         ]
     (.. js/document (getElementById "app") (appendChild e)))
   (comment
     (modules/set-modules!
       (modules/with-modules {"app" [] "main" [] "qb" []})))
   (comment
     (modules/load-module! "app"
       (fn []
         (println "app loaded")
         ;(redgenes.core/init)
         )))))


; (cljs/build "src/cljs" (get-in (into {} (map vec (partition 2 (rest (read-string (slurp "project.clj")))))) [:cljsbuild :builds :modules :compiler]))