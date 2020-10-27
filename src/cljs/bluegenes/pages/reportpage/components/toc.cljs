(ns bluegenes.pages.reportpage.components.toc
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.pages.reportpage.subs :as subs]
            [bluegenes.pages.reportpage.utils :as utils]
            [goog.dom :as gdom]
            [goog.fx.dom :as gfx]
            [goog.fx.easing :as geasing]
            [goog.style :as gstyle]
            [oops.core :refer [ocall oget]]))

;; TODO subscribe to categories data, probably from a safer place in app-db

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
  (let [categories @(subscribe [:bluegenes.pages.admin.subs/categories])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])
        title @(subscribe [::subs/report-title])
        active-toc @(subscribe [::subs/report-active-toc])]
    [:div.toc-container
     [:a.toc-heading {:on-click #(scroll-into-view! nil)}
      [:h4.toc-title title
       [:code.start {:class (str "start-" rootClass)} rootClass]]]
     (into [:ul.toc]
           (for [{:keys [category children] parent-id :id}
                 (cons {:category "Summary" :id utils/pre-section-id} categories)]
             [:<>
              [:a {:on-click #(scroll-into-view! (str parent-id))
                   :class (when (= active-toc (str parent-id)) :active)}
               [:li category]]
              (when (seq children)
                (into [:ul]
                      (for [{:keys [label id]} children]
                        [:a {:on-click #(scroll-into-view! (str id) (str parent-id))
                             :class (when (= active-toc (str id)) :active)}
                         [:li label]])))]))]))
