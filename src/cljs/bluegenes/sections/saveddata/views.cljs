(ns bluegenes.sections.saveddata.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [bluegenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]
            [clojure.string :refer [join]]
            [bluegenes.sections.saveddata.views.saveddataitem :as saved-data-item]
            [bluegenes.sections.saveddata.views.venn :as venn]))


(defn toggle-editor []
  (dispatch [:saved-data/toggle-edit-mode]))

(defn count-all []
  (dispatch [:saved-data/count-all]))

(defn perform-merge []
  (dispatch [:saved-data/perform-operation]))

(defn set-text-filter [e]
  (dispatch [:saved-data/set-text-filter (.. e -target -value)]))

(defn toolbar []
  [:div.btn-toolbar
   [:div.btn.btn-primary.btn-raised
    {:on-click toggle-editor}
    [:span [:i.fa.fa-pie-chart] " Combine Results"]]
   [:div.btn.btn-primary.btn-raised
    {:on-click count-all}
    [:span [:i.fa.fa-pie-chart] " Count All"]]])





(defn missing []
  [:h4 "Please select some data"])

(defn editor-drawer []
  (let [edit-mode (subscribe [:saved-data/edit-mode])
        items     (subscribe [:saved-data/editor-items])]
    (fn []
      (let [[item-1 item-2] (into [] (take 2 @items))]
        [:div.editable-items-drawer.up-shadow
         {:class (if @edit-mode "open" "closed")}
         [:div.venn
          [:div.section.align-right
           (if-not item-1
             [missing]
             [:div
              [:h4 (:label item-1)]
              [:h4 (:path (:selected item-1))]])]
          [:div.section.cant-grow
           [:h4 "Genes"]
           [venn/main]]
          [:div.section.align-left
           (if-not item-2
             [missing]
             [:div
              [:h4 (:label item-2)]
              [:h4 (:path (:selected item-2))]])]]
         [:div.controls
          [:div.btn.btn-info.btn-raised
           {:on-click perform-merge} "Save Results"]]]))))

(defn debug []
  (let [saved-data-section (subscribe [:saved-data/section])]
    [:div
     (json-html/edn->hiccup (->
                              (:editor @saved-data-section)
                              (dissoc :results)))]))



(defn text-filter []
  (let [text           (subscribe [:saved-data/text-filter])
        filtered-items (subscribe [:saved-data/filtered-items])]
    (fn []
      [:div.pane
       {:class (cond
                 (nil? @text) "alert-neutral"
                 (and @text (not-empty @filtered-items)) (str "alert-neutral")
                 (empty? @filtered-items) (str "alert-warning"))}
       [:div.pane-heading "Search"]
       [:div.pane-body
        [:form.form
         [:input.form-control.input-lg.square
          {:type        "text"
           :placeholder "Filter text..."
           :style       {:color     "white"
                         :font-size "24px"}
           :on-change   set-text-filter}]]]])))

(defn main []
  (let [edit-mode      (subscribe [:saved-data/edit-mode])
        filtered-items (subscribe [:saved-data/filtered-items])]
    (reagent/create-class
      {:component-did-mount
       (fn [e] (let [node (-> e reagent/dom-node js/$)]))
       :reagent-render
       (fn []
         [:div.container
          [:div.edit-fade
           {:class (if @edit-mode "show" "not-show")}]
          [:div
           [text-filter]
           [toolbar]
           (into [:div.grid-5_md-4_sm-3_xs-1.saved-data-container]
                 (map (fn [e]
                        ^{:key (:id e)} [saved-data-item/main e]) @filtered-items))]
          [editor-drawer]
          ;[debug]
          ])})))
