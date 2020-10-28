(ns bluegenes.pages.reportpage.components.toc
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.pages.reportpage.events :as events]
            [bluegenes.pages.reportpage.subs :as subs]
            [bluegenes.pages.reportpage.utils :as utils]
            [goog.dom :as gdom]
            [goog.fx.dom :as gfx]
            [goog.fx.easing :as geasing]
            [goog.style :as gstyle]
            [oops.core :refer [ocall oget]]))

(defn scroll-into-view! [id & [parent-id]]
  ;; If the table's parent section is collapsed, we scroll to the parent instead.
  (when-let [elem (or (nil? id) (gdom/getElement id) (gdom/getElement parent-id))]
    (let [current-scroll (clj->js ((juxt #(oget % :x) #(oget % :y))
                                   (gdom/getDocumentScroll)))
          target-scroll (if (nil? id)
                          #js [0 0] ; Scroll to top if no ID specified.
                          (clj->js ((juxt #(- (oget % :x) 60) #(- (oget % :y) 60))
                                    (gstyle/getRelativePosition elem (gdom/getDocumentScrollElement)))))]
      (doto (gfx/Scroll. (gdom/getDocumentScrollElement)
                         current-scroll
                         target-scroll
                         300
                         geasing/inAndOut)
        (.play)))))

(defn main []
  (let [summary (subscribe [::subs/report-summary])
        {:keys [rootClass]} @summary
        categories (subscribe [:current-mine/report-layout rootClass])]
    ;; Usually we'd dispatch this in the route controller, but we really need
    ;; this subscription in this case.
    (dispatch [::events/start-scroll-handling @categories])

    (fn []
      (let [title @(subscribe [::subs/report-title])
            active-toc @(subscribe [::subs/report-active-toc])]
        [:<>
         [:div.toc-hide-heading]
         [:div.toc-container
          [:a.toc-heading {:on-click #(scroll-into-view! nil)}
           [:h4.toc-title title
            [:code.start {:class (str "start-" rootClass)} rootClass]]]
          (into [:ul.toc]
                (for [{:keys [category children] parent-id :id}
                      (cons {:category "Summary" :id utils/pre-section-id} @categories)]
                  [:<>
                   [:a {:on-click #(scroll-into-view! (str parent-id))
                        :class (when (= active-toc (str parent-id)) :active)}
                    [:li category]]
                   (when (and (seq children)
                              (or (= active-toc (str parent-id))
                                  (some #{active-toc} (map (comp str :id) children))))
                     (into [:ul]
                           (for [{:keys [label id]} children]
                             [:a {:on-click #(scroll-into-view! (str id) (str parent-id))
                                  :class (when (= active-toc (str id)) :active)}
                              [:li label]])))]))]]))))
