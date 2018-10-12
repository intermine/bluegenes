(ns bluegenes.sections.reportpage.components.minelinks
  (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [re-frame.core :as re-frame :refer [subscribe]]
     [cljs.core.async :refer [put! chan <! >! timeout close!]]
     [bluegenes.sections.reportpage.components.homologues :refer [homologues]]
     [bluegenes.components.loader :refer [mini-loader]]
     [reagent.core :as reagent]
    )
  )

(def search-results (reagent.core/atom nil))

(defn load-data [id]
  "Loads homologues from each mine."
  (let [current-mine (subscribe [:current-mine])
        svc          (select-keys (:service @current-mine) [:root])]
    (doall (for [[minename details] @(subscribe [:mines])]
      (go (let [
        homologues (<! (homologues svc (:service (:mine details)) "Gene" id (get-in details [:abbrev])))]
          (swap! search-results assoc minename homologues)
      ))))))

(defn get-identifier [homologue]
  "returns an identifier. looks for the symbol first, if there is one, or otherwise uses the primary identifier."
  (let [pi (second homologue)
        symbol (nth homologue 2)]
  (if symbol
    symbol
    pi)
))

(defn homie-list [homologues url]
  (into [:ul.homologues] (map (fn [homie]
    [:li
     [:a {
        :href (str "http://" url "/report.do?id=" (first homie))
        :target "_blank"}
      [:svg.icon.icon-external [:use {:xlinkHref "#icon-external"}]]
      (get-identifier homie)
      ]]) homologues)))

(defn list-homologues [homologues url]
  "Visual component. Given a list of homologues as an IMJS result, output all in a list format"
  (let [showing-all? (reagent/atom false)
        number-to-show 5
        num-homies (count homologues)]  (fn []
    (if @showing-all?
      (homie-list homologues url)
      (conj (homie-list (take number-to-show homologues) url)
        ;;only add a show more link if there are more than 5.
        (cond (> num-homies number-to-show) [:li {:on-click (fn [] (reset! showing-all? true))} [:a.show-more "Show " (- (count homologues) number-to-show) " more"]])))
)))

(defn status-no-known-homologues [empty-mines]
  "outputs visual list of mines for which we have 0 homologue results"
  [:div.no-homologues
   "No known homologues found in: "
    (doall
      (map (fn [mine]
        ^{:key mine}
        [:span.no-homies (:name (:mine (mine @(subscribe [:mines]))))]) empty-mines))])

(defn status-waiting-for-homologues []
  "Visually output mine list for which we still have no results."
  [:div.awaiting-homologues
   [mini-loader "tiny"]
   "Awaiting results from: "
    (doall
      (for [[k v] @(subscribe [:mines])]
        (do
;(.log js/console "%ck" "color:hotpink;font-weight:bold;" (clj->js k) (clj->js v))
        (cond (nil? (k @search-results))
        ^{:key k}
        [:span (:name (:mine v))]))))])

(defn status-list []
  "Give the user status of mines for which we are still loading or have no results for"
  (let [mine-names (set (keys @(subscribe [:mines])))
        active-mines (set (keys @search-results))
        waiting-mines (clojure.set/difference mine-names active-mines)
        empty-mines (keys (filter (fn [[k v]] (empty? (:results v))) @search-results))
        ]
    [:div.status-list
     ;(.log js/console "all" (clj->js mine-names) "active" (clj->js active-mines) "waiting" (clj->js waiting-mines) "Empty:" (clj->js empty-mines))
      ;;output mines from which we're still awaiting results
      (cond (seq waiting-mines)
        [status-waiting-for-homologues])
      ;;output mines with 0 results.
      (cond (seq empty-mines)
        [status-no-known-homologues empty-mines])]))


(defn successful-homologue-results []
  "visually outputs results for each mine that has more than 0 homologues."
     [:div.homologuelinks
     (doall (for [[k v] @search-results]
       (let [this-mine (k @(subscribe [:mines]))
;       (.log js/console "%cvals" "color:red;font-weight:bold;" v)
             homies (:results v)]
         (if (> (count homies) 0)
           ;;Output successful mines
           (doall
             ^{:key k}
             [:div.onemine
               [:h6 (:name (:mine this-mine))]
               [:div.subtitle (:abbrev this-mine)]
               [:div [list-homologues homies (:url (:mine this-mine))]]])
      ))))
     ])


(defn homologue-links []
  "Visual link show component that shows one result per mine"
  [:div.outbound
    [:h4 "Homologues in other Mines"]
    (if (some? @search-results)
      ;;if there are results
      [:div
        [status-list]
        [successful-homologue-results]
        ;;let's tell them we have no homologues if no mines have results,
        ;;but not if it's just because searches haven't come back yet.
        (cond (< (count @search-results) 1)
          [:p "No homologues found. "])]
      ;;if there are no results
      [:div.disabled
        [:svg.icon.icon-external [:use {:xlinkHref "#icon-question"}]]
       [mini-loader]])

      ])

(defn main [x]
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render []
        [homologue-links])
      :component-did-mount (fn [this]
        (load-data x))
      :component-will-update (fn []
        (reset! search-results nil))
      :component-did-update (fn [this]
          (load-data x);)
          )})))
