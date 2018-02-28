(ns bluegenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.table :as table]
            [bluegenes.sections.results.events]
            [bluegenes.sections.results.subs]
            [bluegenes.components.enrichment.views :as enrichment]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]
            [accountant.core :as accountant]
            [json-html.core :as json-html]
            [im-tables.views.core :as tables]))

(defn adjust-str-to-length [length string]
  (if (< length (count string)) (str (clojure.string/join (take (- length 3) string)) "...") string))

(defn breadcrumb []
  (let [history (subscribe [:results/history])
        history-index (subscribe [:results/history-index])]
    (fn []
      [:div.breadcrumb-container
       [:svg.icon.icon-history [:use {:xlinkHref "#icon-history"}]]
       (into [:div.breadcrumb]
             (map-indexed
               (fn [idx {{title :title} :value}]
                 (let [adjusted-title (if (not= idx @history-index) (adjust-str-to-length 20 title) title)]
                   [:div {:class (if (= @history-index idx) "active")
                          :on-click #(accountant/navigate! (str "/results/" idx))}
                    [tooltip
                     {:title title}
                     adjusted-title]])) @history))])))

(defn no-results []
  [:div "Hmmm. There are no results. How did this happen? Whoopsie! "
   [:a {:on-click #(accountant/navigate! "/")} "There's no place like home."]])

(defn main []
  (let [are-there-results? (subscribe [:results/are-there-results?])]
    (fn []
      (if @are-there-results?
        ;;show results
        [:div.container-fluid.results
         [breadcrumb]
         [:div.results-and-enrichment
          [:div.col-md-8.col-sm-12.panel
           ;;[:results :fortable] is the key where the imtables data (appdb) are stored.
           [tables/main [:results :fortable]]]
          [:div.col-md-4.col-sm-12
           [enrichment/enrich]
           ]]]
        ;;oh noes, somehow we made it here with noresults. Fail elegantly, not just console errors.
        [no-results]
        )
      )))
