(ns redgenes.sections.saveddata.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [redgenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [redgenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]))


(def built-in-formatter (tf/formatter "HH:mm:ss dd/MM/YYYY"))

(defn toggle-editor []
  (dispatch [:saved-data/toggle-edit-mode]))

(defn perform-merge []
  (dispatch [:saved-data/perform-operation]))

(defn set-text-filter [e]
  (dispatch [:saved-data/set-text-filter (.. e -target -value)]))

(defn toolbar []
  [:div.btn-toolbar
   [:div.btn.btn-info.btn-raised
    {:on-click toggle-editor} "Merge Lists"]
   [:div.btn.btn-info.btn-raised
    {:on-click perform-merge} "Perform Op"]])

(defn editable-breakdown []
  (let [saved-ids (subscribe [:saved-data/editable-ids])
        model     (subscribe [:model])]
    (fn [id deconstructed-query]
      [:div.panel.panel-default
       (into [:div.panel-body]
             (map (fn [[category-kw paths]]
                    (let [display-name (plural (get-in @model [category-kw :displayName]))
                          path-count   (count paths)]
                      (if (> path-count 1)
                        [:div.dropdown
                         [:div.category.btn.btn-primary.dropdown-toggle
                          {:data-toggle "dropdown"}
                          (str display-name " ")
                          [:span.caret]]
                         (into [:ul.dropdown-menu]
                               (map (fn [part-info]
                                      [:li
                                       {:on-click (fn []
                                                    (dispatch [:saved-data/toggle-editable-item id part-info]))}
                                       [:a (str (:path part-info))]]) paths))]
                        (let [present? (= (first paths) (get-in @saved-ids [id]))]
                          [:div.category.btn
                           {:class (str (if present? "btn-success btn-raised" "btn-primary"))
                            :on-click
                                   (fn []
                                     (dispatch [:saved-data/toggle-editable-item id (first paths)])
                                     (dispatch [:saved-data/set-type-filter category-kw id]))}
                           (str display-name)]))))
                  deconstructed-query))])))

(defn simple-breakdown []
  (let [model (subscribe [:model])]
    (fn [deconstructed-query]
      [:div.panel.panel-default
       (into [:div.panel-body]
             (map (fn [category-kw]
                    (let [display-name (plural (get-in @model [category-kw :displayName]))]
                      [:div.category display-name]))
                  (keys deconstructed-query)))])))

(defn saved-data-item []
  (let [edit-mode (subscribe [:saved-data/edit-mode])]
    (fn [[id {:keys [parts created label type value] :as all}]]
      [:div.col
       [:div.saved-data-item.panel.panel-default
        [:div.panel-heading
         [:div.save-bar
          [:span (tf/unparse built-in-formatter created)]
          ;[:i.fa.fa-2x.fa-times]
          ;[:i.fa.fa-2x.fa-star]
          ]]
        [:div.panel-body
         [:h3 (str label)]
         (if @edit-mode
           [editable-breakdown id parts]
           [simple-breakdown parts])
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn []
                       (dispatch ^:flush-dom [:results/set-query value])
                       (navigate! "#/results"))}
          "View"]]]])))

(defn editor-item []
  (fn [item]
    [:div (:label item)]))

(defn editor-options []
  [:div.item
   [:span.dropdown
    [:button.btn.btn-primary.btn-raised.dropdown-toggle
     {:type "button" :data-toggle "dropdown"}
     "Test"]
    [:ul.dropdown-menu
     [:li [:a [:svg {:width "20px" :height "20px"}
               [:circle {:cx           "10"
                         :cy           "10"
                         :r            "20"
                         :stroke       "black"
                         :stroke-width "1"}]]]]]]])

(defn editor-drawer []
  (let [edit-mode (subscribe [:saved-data/edit-mode])
        items     (subscribe [:saved-data/editor-items])]
    (fn []
      [:div.editable-items-drawer
       {:class (if @edit-mode "open" "closed")}
       [:h1 "Merge results"]
       [editor-options]
       (into [:div]
             (map (fn [item] [editor-item item])) @items)])))


(defn debug []
  (let [saved-data-section (subscribe [:saved-data/section])]
    [:div
     (json-html/edn->hiccup (:editor @saved-data-section))]))



(defn text-filter []
  (let [text           (subscribe [:saved-data/text-filter])
        filtered-items (subscribe [:saved-data/filtered-items])]
    (fn []
      [:div.panel.panel-default
       {:class (cond
                 (and @text (not-empty @filtered-items)) (str "panel-success")
                 (empty? @filtered-items) (str "panel-warning"))}
       [:div.panel-heading "Search"]
       [:div.panel-body
        [:form.form
         [:input.form-control.input-lg.square
          {:type      "text"
           :on-change set-text-filter}]]]])))

(defn main []
  (let [edit-mode      (subscribe [:saved-data/edit-mode])
        filtered-items (subscribe [:saved-data/filtered-items])]
    (reagent/create-class
      {:component-did-mount
       (fn [e] (let [node (-> e reagent/dom-node js/$)]))
       :reagent-render
       (fn []
         [:div {:style {:margin-top "-10px"}}
          [toolbar]
          [:div.edit-fade
           {:class (if @edit-mode "show" "not-show")}]
          [:div.container-fluid
           [:div.container
            [text-filter]
            (into [:div.grid-4_md-3_sm-1.saved-data-container]
                  (map (fn [e] [saved-data-item e]) @filtered-items))]]
          [editor-drawer]
          ;[debug]
          ])})))