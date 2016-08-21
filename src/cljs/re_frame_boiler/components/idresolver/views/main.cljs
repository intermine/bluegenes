(ns re-frame-boiler.components.idresolver.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [re-frame-boiler.components.idresolver.events]
            [re-frame-boiler.components.idresolver.subs]))

(def separators (set ".,; "))

(defn splitter
  "Splits a string on any one of a set of strings."
  [string]
  (->> (clojure.string/split string (re-pattern (str "[" (reduce str separators) "\\r?\\n]")))
       (remove nil?)
       (remove #(= "" %))))

(defn has-separator?
  "Returns true if a string contains any one of a set of strings."
  [str]
  (some? (some separators str)))

(defn controls []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-primary
      {:on-click (fn [] (dispatch [:idresolver/resolve "mad"]))}
      "Upload"]]))

(defn input-box []
  (let [val (reagent/atom nil)]
    (fn []
      [:input.freeform
       {:type        "text"
        :placeholder "Identifiers..."
        :value       @val
        :on-change   (fn [e]
                       (let [input (.. e -target -value)]
                         (if (has-separator? input)
                           (do
                             (reset! val "")
                             (dispatch [:idresolver/resolve (splitter input)]))
                           (reset! val input))))}])))

(defn input-item [i]
  (let [result (subscribe [:idresolver/results-item (:input i)])]
    (fn [i]
      (let [class (if (empty? @result)
                    "inactive"
                    (name (:status (second (first @result)))))]
        [:div.id-resolver-item {:class class}
         [:i.fa.fa-check.fa-1x.fa-fw]
         (:input i)]))))

(defn input-items []
  (let [bank (subscribe [:idresolver/bank])]
    (fn []
      (into [:div.input-items]
            (map (fn [i] [input-item i]) (reverse @bank))))))

(defn input-div []
  (fn []
    [:div.idresolver.form-control
     [input-items]
     [input-box]]))

(defn bank []
  (let [bank (subscribe [:idresolver/bank])]
    (fn []
      [:div (str "bank" @bank)])))

(defn results []
  (let [results (subscribe [:idresolver/results])]
    (fn []
      [:div (json-html/edn->hiccup @results)])))

(defn spinner []
  (let [resolving? (subscribe [:idresolver/resolving?])]
    (fn []
      (if @resolving?
        [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
        [:i.fa.fa-check]))))

(defn main []
  (fn []
    [:div.container
     [:h1 "List Upload"]
     [spinner]
     [input-div]
     [controls]
     [bank]
     [results]]))