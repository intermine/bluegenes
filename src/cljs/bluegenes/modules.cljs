(ns bluegenes.modules
  "
  https://rasterize.io/blog/cljs-dynamic-module-loading.html

  "
  (:require [cljs.core.async :as a]
            [goog.module :as module]
            [goog.module.ModuleManager :as module-manager]
            [goog.module.ModuleLoader])
  (:import goog.module.ModuleManager))

(comment (def modules
   "map of id -> urls"
   #js {"inner" "/js/inner.js"
        "outer" "/js/outer.js"}))

(comment (def module-info
   "An object that contains a mapping from module id (String) to
     list of dependency module ids (Array)."
   #js {"inner" []
        "outer" []}))

(defn with-modules
  [module-info]
  {
   :module-info module-info
   :modules
                (into {}
                  (map vector
                    (keys module-info)
                    (map (fn [m] (str "/js/" m ".js")) (keys module-info))))})

(defn set-modules!
  [{:keys [modules module-info]}]
  (println "set modules" modules module-info)
  (let [
        manager (module-manager/getInstance)
      ]
    (println "loader" (.getLoader manager))
    (cond (nil? (.getLoader manager))
          (.setLoader manager (goog.module.ModuleLoader.)))
    (.setAllModuleInfo manager (clj->js module-info))
    (.setModuleUris manager (clj->js modules))))

(defn loaded? [id]
  (if-let [module (.getModuleInfo (module-manager/getInstance) id)]
    (.isLoaded module)
    false))

(defn load-module!
  "Loads module from the network if necessary. Always returns a
  channel that will be closed when the module is loaded (sometimes
  immediately)"
  [id f]
  (.log js/console (module-manager/getInstance))
  (.execOnLoad (module-manager/getInstance) id f))

(defn set-loaded! [naym]
  (try
    (.setLoaded (module-manager/getInstance) naym)
    (catch js/Error e (println "problem setting module" naym e))))