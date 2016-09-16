(ns redgenes.sections.saveddata.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [redgenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [redgenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]
            [clojure.string :refer [join]]))

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
(def circle-width 300)
(def height 200)
(def bracket-width 10)
(def bracket-padding 10)

(def anchor1 {:x (- (* .33 circle-width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 circle-width) (/ width 2)) :y 0})
(def radius (* .25 (- circle-width (* 2 bracket-width))))


(defn overlap []
  (let [selected?           (subscribe [:saved-data/merge-intersection])
        intersection-points (circle-intersections (:x anchor1) 0 radius (:x anchor2) 0 radius)]
    (fn []
      [:path.part
       {:class    (if @selected? "selected")
        :d        (clojure.string/join
                    " "
                    ["M" (nth intersection-points 1) (nth intersection-points 3)
                     "A" radius radius 0 0 1
                     (nth intersection-points 0) (nth intersection-points 2)
                     "A" radius radius 0 0 1
                     (nth intersection-points 1) (nth intersection-points 3)
                     "Z"])
        :on-click (fn [] (dispatch [:saved-data/toggle-keep-intersections]))}])))

(defn left-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" 0 bracket-padding
                           "L" bracket-width bracket-padding
                           "L" bracket-width (- height bracket-padding)
                           "L" 0 (- height bracket-padding)])}]))

(defn right-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" circle-width bracket-padding
                           "L" (- circle-width bracket-width) bracket-padding
                           "L" (- circle-width bracket-width) (- height bracket-padding)
                           "L" circle-width (- height bracket-padding)])}]))

(defn top-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" (* 2 bracket-padding) 0
                           "L" (* 2 bracket-padding) bracket-padding
                           "L" (- circle-width (* 2 bracket-padding)) bracket-padding
                           "L" (- circle-width (* 2 bracket-padding)) 0
                           ])}]))

(defn circ []
  (let []
    (fn [{:keys [type path keep id]}]
      [:g
       [:circle.part
        {:class    (if (:self keep) "selected")
         :r        radius
         :on-click (fn [x] (dispatch [:saved-data/toggle-keep id]))}]
       #_[:text {:text-anchor "end"
                 :x           50} (str path)]])))

(defn venn []
  (let [editable-ids (subscribe [:saved-data/editable-ids])]
    (fn []
      (let [[item-1 item-2] (take 2 @editable-ids)]
        [:svg.venn {:width width :height height}
         [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
          [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circ item-1]]
          [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circ item-2]]
          [overlap]]
         [left-bracket]
         [right-bracket]
         ;[top-bracket]
         ]))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))


(def built-in-formatter (tf/formatter "HH:mm dd/MM/YYYY"))

(def time-formatter (tf/formatter "HH:mm"))
(def date-formatter (tf/formatter "dd/MM/YYYY"))

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
   [:div.btn.btn-warning.btn-raised
    {:on-click toggle-editor}
    [:span [:i.fa.fa-pie-chart] " Combine Results"]]])

(defn breakdown-item [category-kw id path]
  (let [model (subscribe [:model])
        me    (subscribe [:saved-data/editable-id id path])
        f     (subscribe [:saved-data/type-filter])]
    (fn []
      (let [display-name (plural (get-in @model [category-kw :displayName]))
            can-click?   (if @f (= category-kw @f) true)]
        [:div.category.btn
         {:class (clojure.string/join
                   " "
                   [(if @me "btn-success btn-raised")
                    (if (not can-click?) "disabled not-active" "btn-primary")])
          :on-click
                 (fn []
                   (dispatch [:saved-data/toggle-editable-item id path])
                   (dispatch [:saved-data/set-type-filter category-kw id]))}
         (str display-name)]))))

(defn breakdown-item-dropdown [category-kw id paths]
  (let [model (subscribe [:model])
        f     (subscribe [:saved-data/type-filter])
        subs  (map (fn [p] (subscribe [:saved-data/editable-id id p])) paths)]
    (fn []
      (let [display-name (plural (get-in @model [category-kw :displayName]))
            selected?    (some some? (map deref subs))

            can-click?   (if @f (= category-kw @f) true)]
        [:div.dropdown
         [:div.category.btn.dropdown-toggle
          {:class       (clojure.string/join
                          " "
                          [(if selected? "btn-success btn-raised")
                           (if (not can-click?) "disabled not-active" "btn-primary")])
           :data-toggle "dropdown"}
          (str display-name " ") [:span.caret]]
         (into [:ul.dropdown-menu]
               (map (fn [part-info]
                      [:li
                       {:on-click
                        (fn []
                          (dispatch [:saved-data/toggle-editable-item id part-info])
                          (dispatch [:saved-data/set-type-filter category-kw id]))}
                       [:a (str (:path part-info))]]) paths))]))))

(defn editable-breakdown []
  (let [model (subscribe [:model])]
    (fn [id deconstructed-query]
      (into [:div.panel.panel-body]
            (map (fn [[category-kw paths]]
                   (let [display-name (plural (get-in @model [category-kw :displayName]))
                         path-count   (count paths)]
                     (if (> path-count 1)
                       ^{:key {:id id :paths paths}}
                       [breakdown-item-dropdown category-kw id paths]
                       ^{:key {:id id :paths (first paths)}}
                       [breakdown-item category-kw id (first paths)])))
                 deconstructed-query)))))

(defn simple-breakdown []
  (let [model (subscribe [:model])]
    (fn [deconstructed-query]
      (into [:div]
            (map (fn [category-kw]
                   (let [display-name (plural (get-in @model [category-kw :displayName]))]
                     [:div.category display-name]))
                 (keys deconstructed-query))))))

#_(defn saved-data-item []
  (let [edit-mode (subscribe [:saved-data/edit-mode])]
    (fn [{:keys [count id parts created label type value] :as all}]
      [:div.col
       [:div.saved-data-item.panel.panel-default
        [:div.panel-heading
         [:div.save-bar
          [:span.badge (str count "rows")]
          [:span.pull-right (tf/unparse built-in-formatter created)]
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
          "View"]
         ]]])))

(defn sdi []
  (let [edit-mode (subscribe [:saved-data/edit-mode])]
    (fn [{:keys [count id parts created label type value] :as all}]
      [:div.col
       [:div.sdi.alert.alert-neutral
        [:div.heading
         [:div.save-bar
          {:style {:text-align "right"}}
          [:i.fa.fa-trash-o]]
         [:div.save-bar
          [:span (str count "rows")]
          [:span.pull-right
           [:span (if (t/after? created (t/today-at-midnight))
                    "Today"
                    (tf/unparse date-formatter created))]
           [:span (str " " (tf/unparse time-formatter created))]]]]
        [:h4.grow (str label)]
        [:div.spacer]
        (if @edit-mode
          [editable-breakdown id parts]
          [simple-breakdown parts])
        ;[:div.spacer]
        [:div.btn-toolbar
         {:style {:text-align "right"}}
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn []
                       (dispatch ^:flush-dom [:results/set-query value])
                       (navigate! "#/results"))}
          [:span "View"]]
         ]]])))


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
           [venn]]
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
      [:div.alert
       {:class (cond
                 (nil? @text) "alert-neutral"
                 (and @text (not-empty @filtered-items)) (str "alert-neutral")
                 (empty? @filtered-items) (str "alert-warning"))}
       [:div.panel-heading "Search"]
       [:div.panel-body
        [:form.form
         [:input.form-control.input-lg.square
          {:type      "text"
           :style {:color "white"
                   :font-size "24px"}
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
          [:div.edit-fade
           {:class (if @edit-mode "show" "not-show")}]
          [:div.container-fluid
           [toolbar]
           [text-filter]
           [css-transition-group
            {:transition-name "foo"}
            (into [:div.grid-4_md-3_sm-1.saved-data-container]
                  (map (fn [e]
                         ^{:key (:id e)} [sdi e]) @filtered-items))]]
          [editor-drawer]
          ;[debug]
          ])})))