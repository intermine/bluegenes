(ns bluegenes.pages.home.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.route :as route]
            [bluegenes.components.icons :refer [icon]]
            [markdown-to-hiccup.core :as md]
            [bluegenes.components.navbar.nav :refer [mine-icon]]
            [bluegenes.components.search.typeahead :as search]
            [clojure.string :as str]
            [bluegenes.utils :refer [ascii-arrows ascii->svg-arrows]]))

(defn mine-intro []
  (let [mine-name @(subscribe [:current-mine-human-name])
        current-mine @(subscribe [:current-mine])]
    [:div.row.section
     [:div.col-xs-12
      [:h2.text-center.text-uppercase mine-name]]
     [:div.col-xs-10.col-xs-offset-1
      [:div.row
       [:div.col-xs-12.col-sm-8
        [:p.mine-description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id."]
        [:div.search
         [search/main]
         [:div.search-info
          [icon "info"]
          [:span "Genes, proteins, pathways, ontology terms, authors, etc."]]]]
       [:div.col-sm-4.hidden-xs
        [mine-icon current-mine :class "img-responsive"]]]]]))

(defn call-to-action []
  [:div.row.section
   [:div.col-xs-12.col-sm-5.cta-block
    [:h3.text-uppercase "Analyse your biodata"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:a.btn.btn-home
     {:href (route/href ::route/upload)}
     "Analyse data"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2.cta-block
    [:h3.text-uppercase "Build your own query"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:a.btn.btn-home
     {:href (route/href ::route/querybuilder)}
     "Build query"]]])

(defn template-queries []
  (let [categories @(subscribe [:templates-by-popularity/all-categories])
        current-category (or @(subscribe [:home/active-template-category])
                             (first categories))
        templates @(subscribe [:templates-by-popularity/category current-category])]
    [:div.row.section
     [:div.col-xs-12
      [:h2.text-center "Go by Most Popular Queries"]]
     [:div.col-xs-12.template-preview
      [:ul.nav.nav-tabs.template-tabs
       (for [category categories]
         ^{:key category}
         [:li {:class (when (= category current-category) "active")}
          [:a {:on-click #(dispatch [:home/select-template-category category])}
           (str/replace category #"^im:aspect:" "")]])]
      [:ul.template-list
       (for [{:keys [title name]} templates]
         ^{:key name}
         [:li
          [:a {:href (route/href ::route/template {:template name})}
           (if (ascii-arrows title)
             (ascii->svg-arrows title)
             [:span title])]])]
      [:a.more-queries {:href (route/href ::route/templates)}
       "More queries here"]
      [:hr]]]))

(defn external-resources []
  [:div.row.section
   [:div.col-xs-12.col-sm-5.cta-block
    [:h3.text-uppercase "What's new?"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "View all posts"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2.cta-block
    [:h3.text-uppercase "API in different languages"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "Developer resources"]]
   [:div.col-xs-12.col-sm-6.col-sm-offset-3.cta-block
    [:h3.text-uppercase "Written and video tutorials"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "View documentation"]]])

(defn mine-selector-filter []
  (let [all-neighbourhoods @(subscribe [:home/all-registry-mine-neighbourhoods])
        current-neighbourhood (or @(subscribe [:home/active-mine-neighbourhood])
                                  (first all-neighbourhoods))]
    [:div.mine-neighbourhood-filter.text-center
     (for [neighbourhood all-neighbourhoods]
       [:label
        [:input {:type "radio"
                 :name neighbourhood
                 :checked (= neighbourhood current-neighbourhood)
                 :on-change #(dispatch [:home/select-mine-neighbourhood neighbourhood])}]
        neighbourhood])]))

(defn get-fg-color [mine-details]
  (get-in mine-details [:colors :header :text]))

(defn get-bg-color [mine-details]
  (get-in mine-details [:colors :header :main]))

(defn mine-selector-entry [[mine-key details] & {:keys [active?]}]
  (let [{:keys [name]} details]
    [:button.btn-link.col-xs-6.col-md-4.col-lg-3.mine-entry
     (merge
      {:class (when active? "mine-entry-active")
       :on-click #(dispatch [:home/select-preview-mine mine-key])}
      (when active?
        {:style {:color (get-fg-color details)
                 :background-color (get-bg-color details)}}))
     [:span (or name "default")]
     [icon "plus" nil [:pull-right]]]))

(defn mine-selector-preview []
  (let [{:keys [description name] :as preview-mine} @(subscribe [:home/preview-mine])
        mine-ns (-> preview-mine :namespace keyword)]
    [:div.col-xs-10.col-xs-offset-1.col-sm-offset-0.col-sm-3.mine-preview
     [:h4.text-center name]
     [:p description]
     [:div.preview-image
      [mine-icon preview-mine :class "img-responsive"]]
     [:button.btn.btn-block
      {:on-click #(dispatch [::route/navigate ::route/home {:mine mine-ns}])
       :style {:color (get-fg-color preview-mine)
               :background-color (get-bg-color preview-mine)}}
      (str "Switch to " name)]]))

(defn mine-selector []
  (let [registry-mines @(subscribe [:home/mines-by-neighbourhood])
        active-ns (-> @(subscribe [:home/preview-mine]) :namespace keyword)]
    [:div.row.section
     [:div.col-xs-12
      [:h2.text-center.text-uppercase "InterMine for all"]]
     [:div.col-xs-12.mine-selector
      [mine-selector-filter]
      [:div.row.mine-selector-body
       [:div.col-xs-12.col-sm-9.mine-selector-entries
        [:div.row
         (for [mine registry-mines]
           ^{:key (key mine)}
           [mine-selector-entry mine :active? (= active-ns (key mine))])]]
       [mine-selector-preview]]]]))

(defn external-tools []
  [:div.row.section
   [:div.col-xs-12
    [:h2.text-center "External tools"]]
   [:div.col-xs-12.col-sm-5.cta-block
    [:h3 "Data Browser"]
    [:p "A faceted search tool to display the data from InterMine database, allowing the users to search easily within the different mines available around InterMine without the requirement of having an extensive knowledge of the data model."]
    [:a.btn.btn-home
     {:href "http://data-browser.apps.intermine.org/"
      :target "_blank"}
     "Open Data Browser"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2.cta-block
    [:h3 "InterMOD GO"]
    [:p "This tool searches for homologous genes and associated GO terms across six model organisms (yeast, nematode worm, fruit fly, zebrafish, mouse, rat) and humans, with a heatmap, statistical enrichment, and a GO term relationship graph."]
    [:a.btn.btn-home
     {:href "http://gointermod.apps.intermine.org/"
      :target "_blank"}
     "Open InterMOD GO"]]])

(defn feedback []
  [:div.row.section
   [:div.col-xs-12
    [:h2.text-center "We value your opinion"]
    [:p.text-center "Feedback received by organisation@mail.com"]]
   [:div.col-xs-12
    [:p.text-center "Did our service meet your needs?"]
    [:div.btn-toolbar
     [:div.btn-group
      (for [number (range 1 6)]
        ^{:key number}
        [:button.btn.btn-raised
         number])]]
    [:p.text-center "Suggestions? Questions? Comments?"]
    [:div
     [:input.form-control
      {:type "email"
       :placeholder "Your email (optional)"}]
     [:textarea.form-control
      {:placeholder "Your feedback here"}]
     [:button.btn.btn-primary.btn-raised.btn-block
      "Submit"]]]])

(defn credits-entry [{:keys [text image url]}]
  (if (not-empty text)
    [:div.col-xs-12
     [:div.row.row-center-cols
      [:div.col-xs-4
       [:a {:href url}
        [:img.img-responsive
         {:src image}]]]
      [:div.col-xs-8
       (some-> text md/md->hiccup md/component (md/hiccup-in :div :p))]]]
    [:div.col-xs-4
     [:a {:href url}
      [:img.img-responsive
       {:src image}]]]))

;; TODO maybe this should be in the properties file by default?
;; - so the images will get hosted on the InterMine backend of the mine
(def credits-intermine
  [{:text "InterMine has been developed principally through support of the [Wellcome Trust](https://wellcome.ac.uk/). Complementary projects have been funded by the [NIH/NHGRI](https://www.nih.gov/) and the [BBSRC](https://bbsrc.ukri.org/)."
    :image "https://www.humanmine.org/humanmine/images/icons/intermine-footer-logo.png"
    :url "http://intermine.org/"}
   {:image "https://www.humanmine.org/humanmine/images/wellcome-logo-black.png"
    :url "https://wellcome.ac.uk/"}
   {:image "https://www.humanmine.org/humanmine/images/logo_nhgri.png"
    :url "https://www.nih.gov/"}
   {:image "https://www.humanmine.org/humanmine/images/bbsrc-logo.gif"
    :url "https://bbsrc.ukri.org/"}])

(defn credits []
  (let [mine-name @(subscribe [:current-mine-human-name])
        ;; TODO replace with web-properties
        entries [{:image "https://bar.utoronto.ca/thalemine/images/CAGEF.png"
                  :url "http://www.cagef.utoronto.ca/"}
                 {:image "https://bar.utoronto.ca/thalemine/images/UofT_Logo.svg"
                  :url "https://www.utoronto.ca/"}
                 {:image "https://mines.legumeinfo.org/cyverse_rgb-40.png"
                  :url "https://cyverse.org/"}
                 {:text "The [Legume Information System (LIS)](https://legumeinfo.org/) is a research project of the [USDA-ARS:Corn Insects and Crop Genetics Research](https://www.ars.usda.gov/midwest-area/ames/cicgru/) in Ames, IA."
                  :image "https://mines.legumeinfo.org/beanmine/model/images/USDA-92x67.png"
                  :url "https://usda.gov/"}]]
    [:div.row.section
     [:div.col-xs-12
      [:h3.text-center (str mine-name " is made possible by")]]
     [:div.col-xs-10.col-xs-offset-1
      [:div.row.row-center-cols.row-space-cols
       (for [[i entry] (map-indexed vector (concat entries credits-intermine))]
         ^{:key i}
         [credits-entry entry])]]]))

(defn main []
  [:div.container.home
   [mine-intro]
   [call-to-action]
   [template-queries]
   [external-resources]
   [mine-selector]
   [external-tools]
   [feedback]
   [credits]])
