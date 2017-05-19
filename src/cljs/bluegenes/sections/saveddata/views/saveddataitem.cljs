(ns bluegenes.sections.saveddata.views.saveddataitem
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [bluegenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]
            [clojure.string :refer [join]]))

(def time-formatter (tf/formatter "HH:mm"))
(def date-formatter (tf/formatter "dd/MM/YYYY"))

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

(defn main []
  (let [edit-mode (subscribe [:saved-data/edit-mode])]
    (fn [{:keys [sd/count sd/id sd/parts sd/created sd/label sd/type sd/value] :as all}]
      [:div.col
       [:div.sdi.pane.soft
        [:h4.grow (str label)]
        [:div.spacer]
        (case type
            :list [:div [:div.category (plural (:type value))]]
            :query (if (and @edit-mode false)
                     [editable-breakdown id parts]
                     [simple-breakdown parts]))
        ;[:div.spacer]
        [:div.btn-toolbar
         {:style {:text-align "right"}}
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn []
                       (dispatch ^:flush-dom [:saved-data/view-query id])
                       ;(println "set query" value)

                       )}
          [:span "View"]]
         ]
        [:div.save-bar
         {:style {:text-align "right"}}
         [:div.heading [:span (str count " rows, ")]
          (if created [:span (if (t/after? created (t/today-at-midnight))
                    "Today"
                    (tf/unparse date-formatter created))])
          [:span (str " " (tf/unparse time-formatter created))]]
         ;[:span [:i.fa.fa-trash-o]]
         ]
        ]])))