(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]))

(defn icon []
  (fn [fa-icon]
    [:i.fa {:class (str "fa fa-fw fa-" fa-icon)}]))


(def mock-data (r/atom [{:type :folder
                         :open true
                         :label "Folder One"}
                        {:type :folder
                         :open false
                         :label "Folder Two"
                         :children [{:type :folder
                                     :open false
                                     :label "Folder Three"
                                     :children [{:type :list
                                                 :label "Some List"}
                                                {:type :list
                                                 :label "Some List"}
                                                {:type :list
                                                 :label "Some List"}]}]}]))

(defn get-at [pos-vec data]
  (get-in data (interpose :children pos-vec)))

(defn toggle-open [pos-vec]
  (swap! mock-data update-in (interpose :children pos-vec) update :open not))

(defmulti tree (fn [item] (:type item)))

(defmethod tree :folder [{:keys [type label children open trail]}]
  (apply conj
         [:li.folder {:style {:margin-left (str (* (count trail) 10) "px")}
                      :on-click (fn [evt] (ocall evt :stopPropagation) (toggle-open trail))}
          (if open [icon "folder-open"] [icon "folder"])
          label]
         (when open
           (map-indexed
             (fn [idx c] [tree (assoc c :trail (conj trail idx))])
             children))))

(defmethod tree :list [{:keys [type label trail]}]
  [:li.item {:style {:margin-left (str (* (count trail) 10) "px")}}
   [icon "list"] label])

(defn main []
  (fn []
    [:div.container
     [:div.mymine (into [:ul]
                        (map-indexed
                          (fn [idx x] [tree (assoc x :trail [idx])])
                          @mock-data))]]))
