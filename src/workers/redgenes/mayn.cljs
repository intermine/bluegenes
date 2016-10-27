(ns redgenes.mayn
  (:require [clojure.string :as string]))

(enable-console-print!)

(defn get! [f url]
 (let [r (js/XMLHttpRequest.)]
   (.addEventListener r :load f)
   (.open r :GET url)
   (.send r)))

(defn as-blob [code]
 (let [
        b (js/Blob. (clj->js [code]) (clj->js {:type "text/javascript"}))
       ]
   {
    :source code
    :blob b
    :url (.createObjectURL js/URL b)
   }))

(defn qoot [s] (str "'" s "'"))

(defn with-scripts
 ([source urls]
   (str
     "self.importScripts("
     (string/join "," (map qoot urls))
     ");"
     source)))

(defn get-code
 ([codez [{:keys [naym url]} & r]]
  (if url
    (get!
     (fn [e]
       (this-as thys
         (if r
          (recur
            (assoc codez naym (as-blob (.-responseText thys)))
            r)
            codez)))
     "js/no-react/cljs_base.js")
     codez))
 ([base]
   (get!
     (fn [e]
       (println "add base")
       (this-as thys
         (let
           [
            code (as-blob (with-scripts (.-responseText thys) [(:url base)]))
            codez {
                   "base"    base
                   "workers" code
                   }]
           (set! (.-codez js/self) (clj->js codez))
           (println "got all that"))))
     "js/no-react/redgenes/workers.js")))

(get-code)