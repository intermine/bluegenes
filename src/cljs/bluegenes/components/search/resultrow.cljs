(ns bluegenes.components.search.resultrow
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall oget]]
            [bluegenes.route :as route]
            [bluegenes.utils :refer [highlight-substring]]
            [imcljs.path :as im-path]
            [clojure.string :as str]))

(defn result-selection-control
  "UI control suggesting to the user that there is only one result selectable at any one time; there's no actual form functionality here."
  [result]
  (let [selected? @(subscribe [:search/am-i-selected? result])]
    [:input {:type "checkbox"
             :on-change (fn [e]
                          (if (oget e :target :checked)
                            (dispatch [:search/select-result result])
                            (dispatch [:search/deselect-result result])))
             ;; on-change is the React idiomatic way to handle interaction, but
             ;; for some reason calling stopPropagation in there doesn't work!
             :on-click #(ocall % :stopPropagation)
             :checked selected?
             :name "keyword-search"}]))

(defn row-structure
  "This method abstracts away most of the common components for all the result-row baby methods."
  [row-data contents]
  (let [{:keys [id type] :as result} (:result row-data)
        category-filter? (subscribe [:search/category-filter?])
        selected?        (subscribe [:search/am-i-selected? id])]
    ;;Todo: Add a conditional row highlight on selected rows.
    [:div.result
     {:on-click #(dispatch [::route/navigate ::route/report {:type type :id id}])
      :class (if @selected? "selected")}
     (when @category-filter?
       [result-selection-control result])
     [:span.result-type {:class (str "type-" type)} type]
     [contents]]))

(defn result-row [row-data]
  [row-structure row-data
   (fn []
     (let [model @(subscribe [:current-model])
           summary-fields @(subscribe [:current-summary-fields])
           {:keys [fields type]} (:result row-data)
           result-fields (into {}
                               (map (fn [[k v]] [(str type "." (name k)) v]))
                               fields)]
       [:div.details
        (into [:ul]
              (keep (fn [path]
                      (when-let [field (find result-fields path)]
                        [:li
                         [:h6.default-description (str/join " > " (drop 1 (im-path/display-name model path)))]
                         [:div.default-value
                          (when-let [v (not-empty (str (val field)))]
                            (into [:span]
                                  (highlight-substring v (:search-term row-data) :searchterm)))]]))
                    (get summary-fields (keyword type))))]))])
