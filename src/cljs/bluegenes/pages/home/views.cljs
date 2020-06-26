(ns bluegenes.pages.home.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.route :as route]
            [bluegenes.components.icons :refer [icon]]
            [markdown-to-hiccup.core :as md]))

(defn mine-intro []
  (let [mine-name @(subscribe [:current-mine-human-name])]
    [:div.row.section
     [:div.col-xs-12
      [:h2.text-center.text-uppercase mine-name]]
     [:div.col-xs-10.col-xs-offset-1
      [:div.row
       [:div.col-xs-12.col-sm-8
        [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi dignissim integer proin suscipit mi aliquet semper. Quis potenti odio elit leo amet. Pulvinar turpis in odio elit dui enim, ipsum. Sed vitae etiam turpis gravida malesuada massa vel lectus. Massa malesuada nunc id nibh eget metus, condimentum faucibus amet. Lectus lorem cursus sem et dignissim. At gravida sed viverra sapien neque pellentesque adipiscing rhoncus neque. Sit sit ac eget ut nisl proin mauris diam porta. Donec velit sed."]
        [:button.btn.btn-home
         "Watch video tutorial"]]
       [:div.col-sm-4.hidden-xs
        [:img.img-responsive
         {:src "https://source.unsplash.com/random"
          :alt ""}]]]]]))

(defn call-to-action []
  [:div.row.section
   [:div.col-xs-12.col-sm-5.cta-block
    [:h3.text-uppercase "Build your own query"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "Build query"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2.cta-block
    [:h3.text-uppercase "Analyse your biodata"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "Analyse data"]]
   [:div.col-xs-12.col-sm-5.cta-block
    [:h3.text-uppercase "What's new?"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "View all posts"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2.cta-block
    [:h3.text-uppercase "API in different languages"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-home
     "Developer resources"]]])

(defn template-queries []
  [:div.row.section
   [:div.col-xs-12
    [:h2.text-center "Go by Most Popular Queries"]]
   [:div.col-xs-12
    [:ul.nav.nav-tabs
     [:li.active [:a "Regulation"]]
     [:li [:a "Genes"]]]
    [:ul
     [:li [:a "Lorem ipsum dolor sit amet -> consectetur adipiscing elit"]]
     [:li [:a "Lorem ipsum dolor sit amet -> consectetur adipiscing elit"]]
     [:li [:a "Lorem ipsum dolor sit amet -> consectetur adipiscing elit"]]
     [:li [:a "Lorem ipsum dolor sit amet -> consectetur adipiscing elit"]]]
    [:a {:ref (route/href ::route/templates)}
     "More queries here"]
    [:hr]]])

(defn mine-selector-filter []
  [:div.mine-neighbourhood-filter.text-center
   [:label
    [:input {:type "radio" :name "Animals" :value "animals" :checked true}]
    "Animals"]
   [:label
    [:input {:type "radio" :name "Plants" :value "plants" :checked false}]
    "Plants"]])

(defn mine-selector-entry [[mine-key details]]
  (let [{:keys [name]} details]
    [:div.col-xs-3
     [:span name]
     [icon "plus" nil [:pull-right]]]))

(defn mine-selector []
  (let [registry-mines @(subscribe [:registry])]
   [:div.row.section
    [:div.col-xs-12
     [:h2.text-center.text-uppercase "InterMine for all"]]
    [:div.col-xs-12
     [mine-selector-filter]
     [:div.row
      [:div.col-xs-12.col-sm-8
       [:div.row
        (for [mine registry-mines]
          ^{:key (key mine)}
          [mine-selector-entry mine])]]
      [:div.col-xs-10.col-xs-offset-1.col-sm-offset-0.col-sm-4
       [:h4.text-center "BMAP"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nisi, pretium praesent varius velit."]
       [:img.img-responsive
        {:src "https://source.unsplash.com/random/300x200"
         :alt ""}]
       [:button.btn.btn-primary.btn-raised.btn-block
        "Switch to BMAP"]]]]]))

(defn external-tools []
  [:div.row.section
   [:div.col-xs-12
    [:h2.text-center "External tools"]
    [:p.text-center "Explore InterMine data with alternative tools"]]
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
   [mine-selector]
   [external-tools]
   [feedback]
   [credits]])
