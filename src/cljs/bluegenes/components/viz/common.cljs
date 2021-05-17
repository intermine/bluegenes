(ns bluegenes.components.viz.common
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

;; Taken from the oz project and adjusted to our usecase.
;; https://github.com/metasoarous/oz/blob/master/src/cljs/oz/core.cljs

;; An alternative way to update the data input which *could* result in better
;; performance would be by using data streaming.
;; https://vega.github.io/vega-lite/tutorials/streaming.html
;; https://vega.github.io/vega/docs/api/view/#view_data
;; We would have to give each viz a unique named data source, and save the
;; delivered promise returned by vegaEmbed (probably in a regular atom).
;; Then we're able to use `promiseResult.view.data(name, newData).run()` to only
;; update the data (we can check in `:component-did-update` whether only the
;; data changed) instead of unnecessarily re-running vegaEmbed.

(defn ^:no-doc embed-vega
  ([elem doc] (embed-vega elem doc {}))
  ([elem doc opts]
   (when doc
     (let [doc (clj->js doc)
           opts' (merge {:renderer :canvas
                         ;; Have to think about how we want the defaults here to behave
                         :mode "vega-lite"}
                        (dissoc opts :callback))]
       (-> (js/vegaEmbed elem doc (clj->js opts'))
           (.then (get opts :callback #()))
           (.catch (fn [err]
                     (.warn js/console err))))))))

(def local-keys
  "Keys that may be passed in opts which we only use in our local implementation.
  I.e., not intended for consumption by vegaEmbed."
  [:class])

(defn vega
  "Reagent component that renders vega"
  ([doc] (vega doc {}))
  ([doc opts]
   ;; Is this the right way to do this? So vega component behaves abstractly like a vega-lite potentially?
   (let [opts (merge {:mode "vega"} opts)]
     (r/create-class
      {:display-name "vega"
       :component-did-mount (fn [this]
                              (embed-vega (rd/dom-node this) doc (apply dissoc opts local-keys)))
       :component-did-update (fn [this [_ old-doc old-opts]]
                               (let [[_ new-doc new-opts] (r/argv this)]
                                 (when (or (not= old-doc new-doc)
                                           (not= old-opts new-opts))
                                   (embed-vega (rd/dom-node this) new-doc (apply dissoc new-opts local-keys)))))
       :reagent-render (fn []
                         [:div
                          {:class (:class opts)}])}))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  ([doc] (vega-lite doc {}))
  ([doc opts]
   ;; Which way should the merge go?
   (vega doc (merge opts {:mode "vega-lite"}))))
