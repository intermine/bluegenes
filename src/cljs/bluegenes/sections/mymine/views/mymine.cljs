(ns bluegenes.sections.mymine.views.mymine
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [clojure.walk :as walk]))

(defn icon []
  (fn [fa-icon]
    [:i.fa {:class (str "fa fa-fw fa-" fa-icon)}]))

(def mock-tree (r/atom {:type     :folder
                        :open     true
                        :children [{:type  :folder
                                    :open  true
                                    :label "Folder One"}
                                   {:type     :folder
                                    :open     false
                                    :label    "Folder Two"
                                    :children [{:type     :folder
                                                :open     false
                                                :label    "Folder Three"
                                                :children [{:type  :list
                                                            :label "Some List"}
                                                           {:type  :list
                                                            :label "Some List"}
                                                           {:type  :list
                                                            :label "Some List"}]}]}
                                   {:type  :folder
                                    :open  true
                                    :label "Folder FOUR"}]}))

(defn branch? [m] (and (:open m) (contains? m :children)))
(defn children [m] (map-indexed (fn [idx child]
                                  (assoc child :trail (vec (conj (:trail m) idx))))
                                (:children m)))
(defn flattify [m] (tree-seq branch? children m))

(defn get-at [pos-vec data]
  (get-in data (conj (interpose :children pos-vec) :children)))

(defn toggle-open [pos-vec]
  (swap! mock-tree update-in
         (conj (interpose :children pos-vec) :children) update :open not))

(defmulti tree (fn [item] (:type item)))

(defmethod tree :folder [{:keys [type label children open trail]}]
  [:tr
   [:td
    [:span {:style    {:padding-left (str (* (dec (count trail)) 40) "px")}
            :on-click (fn [evt]
                        (ocall evt :stopPropagation)
                        (println "got" (get-at trail @mock-tree))
                        (toggle-open trail)
                        )}
     [:span.fa-stack.fa-lg
      {:style {:position "relative"}}
      [:i.fa.fa-circle.fa-stack-2x]
      (if open
        [:i.fa.fa-folder-open.fa-stack-1x.fa-inverse]
        [:i.fa.fa-folder.fa-stack-1x.fa-inverse])
      ]
     label]]
   [:td [:span "Today"]]])

(defmethod tree :list [{:keys [type label trail]}]
  [:tr
   [:td
    [:span {:style {:padding-left (str (* (dec (count trail)) 40) "px")}}
     [:span.fa-stack.fa-lg
      [:i.fa.fa-circle.fa-stack-2x]
      [:i.fa.fa-list.fa-stack-1x.fa-inverse]] label]]
   [:td [:span "Today"]]])

(defn main []
  (fn []
    [:div.container-fluid
     [:div.mymine
      [:table.table.table-striped
       [:thead
        [:tr
         [:th "Name"]
         [:th "Last Modified"]
         ]]
       (into [:tbody] (map (fn [x] [tree x]) (rest (flattify @mock-tree))))]]]))
