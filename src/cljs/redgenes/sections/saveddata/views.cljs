(ns redgenes.sections.saveddata.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [redgenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [redgenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]))



(defn circle-intersections
  "Determines the pair of X and Y coordinates where two circles intersect."
  [x0 y0 r0 x1 y1 r1]
  (let [dx       (- x1 x0)
        dy       (- y1 y0)
        d        (.sqrt js/Math (+ (* dy dy) (* dx dx)))
        a        (/ (+ (- (* r0 r0) (* r1 r1)) (* d d)) (* 2 d))
        x2       (+ x0 (* dx (/ a d)))
        y2       (+ y0 (* dy (/ a d)))
        h        (.sqrt js/Math (- (* r0 r0) (* a a)))
        rx       (* (* -1 dy) (/ h d))
        ry       (* dx (/ h d))
        xi       (+ x2 rx)
        xi-prime (- x2 rx)
        yi       (+ y2 ry)
        yi-prime (- y2 ry)]
    [xi xi-prime yi yi-prime]))

(def width 300)
(def height 300)

(def anchor1 {:x (- (* .33 width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 width) (/ width 2)) :y 0})
(def radius (* .33 width))

(println "anchor2" anchor2)

(def center {:x 250 :y 250})
;(def radius 150)


(defn overlap []
  (let [intersection-points (circle-intersections (:x anchor1) 0 radius (:x anchor2) 0 radius)]
    (fn []

      [:path.part
       {:d (clojure.string/join
             " "
             ["M" (nth intersection-points 1) (nth intersection-points 3)
              "A" radius radius 0 0 1
              (nth intersection-points 0) (nth intersection-points 2)
              "A" radius radius 0 0 1
              (nth intersection-points 1) (nth intersection-points 3)
              "Z"])}])))

(defn circle2 []
  (let []
    (fn []
      [:circle.part.selected
       {:r radius}])))

(defn circle1 []
  (let []
    (fn []
      [:circle.part
       {:r radius}])))

(defn svg []
  (fn []
    [:svg.venn {:width width :height height}
     [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
      [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circle1]]
      [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circle2]]
      [overlap]
      ]]))

(defn main2 []
  (fn []
    [svg]))




















;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
    (fn [{:keys [id parts created label type value] :as all}]
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



(defn editor-action []
  [:div.item
   #_[:span.dropdown
      [:button.btn.btn-primary.btn-raised.dropdown-toggle
       {:type "button" :data-toggle "dropdown"}
       "Test"]
      [:ul.dropdown-menu
       [:li [:a "Test"]]]]
   [:button.btn.btn-primary "All items"]
   [:button.btn.btn-primary "A minus B"]
   [:button.btn.btn-primary "B minus A"]
   [:button.btn.btn-primary [:svg {:height "30" :width "60"}
                             [:circle {:cx   "20" :cy "15" :r 15
                                       :fill "cornflowerblue"}]
                             [:circle {:cx   "40" :cy "15" :r 15
                                       :fill "cornflowerblue"}]]]])

(defn blank-item []
  [:div.blank-item
   [:h1 "Choose Item"]])

(defn editor-item []
  (fn [item]
    [:div.blank-item
     [:h4 (:label item)]
     [:span (str (get-in item [:value :select]))]]))

(defn editor-drawer []
  (let [edit-mode (subscribe [:saved-data/edit-mode])
        items     (subscribe [:saved-data/editor-items])]
    (fn []
      [:div.editable-items-drawer
       {:class (if @edit-mode "open" "closed")}
       (if (empty? @items)
         [blank-item]
         (let [comps (for [item @items] [editor-item item])]
           (if (= 2 (count comps))
             (interpose [main2] comps)
             comps)))])))


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