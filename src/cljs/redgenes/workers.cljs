(ns redgenes.workers)

;(js/importScripts "app.js")

;(apply + [1 2 3])

;(println "setting this worker's on-message")

(.log js/console "hello from worker")

(set! (.-onmessage js/self)
  (fn [e]
    (.log js/console "<<<<" e)
    (try
      (println "hello from cljs")
      (catch js/Error ee (.log js/console ee)))
    (.postMessage js/self "hi from worker")))
