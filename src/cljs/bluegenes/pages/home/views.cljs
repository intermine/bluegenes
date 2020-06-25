(ns bluegenes.pages.home.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.route :as route]
            [bluegenes.components.icons :refer [icon]]))

(defn mine-intro []
  (let [mine-name @(subscribe [:current-mine-human-name])]
    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [:div.row
       [:div.col-xs-12
        [:h2.text-center mine-name]]]
      [:div.row
       [:div.col-xs-12.col-sm-8
        [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi dignissim integer proin suscipit mi aliquet semper. Quis potenti odio elit leo amet. Pulvinar turpis in odio elit dui enim, ipsum. Sed vitae etiam turpis gravida malesuada massa vel lectus. Massa malesuada nunc id nibh eget metus, condimentum faucibus amet. Lectus lorem cursus sem et dignissim. At gravida sed viverra sapien neque pellentesque adipiscing rhoncus neque. Sit sit ac eget ut nisl proin mauris diam porta. Donec velit sed."]
        [:button.btn.btn-primary.btn-raised
         "Get video tutorial here"]]
       [:div.col-sm-4.hidden-xs
        [:img.img-responsive
         {:src "https://source.unsplash.com/random"
          :alt ""}]]]]]))

(defn call-to-action []
  [:div.row
   [:div.col-xs-12.col-sm-5
    [:h3 "Build your own query"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-primary.btn-raised
     "Build query"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2
    [:h3 "Analyse your biodata"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-primary.btn-raised
     "Analyse data"]]
   [:div.col-xs-12.col-sm-5
    [:h3 "What's new?"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-primary.btn-raised
     "View all posts"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2
    [:h3 "API in different languages"]
    [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Eu dui morbi nisl, velit aliquam nec porta laoreet magna. Cras sollicitudin varius nulla id. Sed ullamcorper nibh ut arcu nulla aliquam diam cras."]
    [:button.btn.btn-primary.btn-raised
     "Developer resources"]]])

(defn template-queries []
  [:div.row
   [:div.col-xs-12
    [:h2.text-center "Go by Most Popular Queries"]
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
   [:div.row
    [:div.col-xs-12
     [:h2.text-center "InterMine for all"]
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
       [:button.btn.btn-primary.btn-raised
        "Switch to BMAP"]]]]]))

(defn external-tools []
  [:div.row
   [:div.col-xs-12
    [:h2.text-center "External tools"]
    [:p.text-center "Explore InterMine data with alternative tools"]]
   [:div.col-xs-12.col-sm-5
    [:h3 "Data Browser"]
    [:p "A faceted search tool to display the data from InterMine database, allowing the users to search easily within the different mines available around InterMine without the requirement of having an extensive knowledge of the data model."]
    [:a.btn.btn-primary.btn-raised
     {:href "http://data-browser.apps.intermine.org/"
      :target "_blank"}
     "Open Data Browser"]]
   [:div.col-xs-12.col-sm-5.col-sm-offset-2
    [:h3 "InterMOD GO"]
    [:p "This tool searches for homologous genes and associated GO terms across six model organisms (yeast, nematode worm, fruit fly, zebrafish, mouse, rat) and humans, with a heatmap, statistical enrichment, and a GO term relationship graph."]
    [:a.btn.btn-primary.btn-raised
     {:href "http://gointermod.apps.intermine.org/"
      :target "_blank"}
     "Open InterMOD GO"]]])

(defn feedback []
  [:div.row
   [:div.col-xs-12
    [:h2.text-center "We value your opinion"]
    [:p.text-center "Feedback received by organisation@mail.com"]
    [:div
     [:h4.text-center "Did our service meet your needs?"]
     [:div.btn-toolbar
      [:div.btn-group
       (for [number (range 1 6)]
         ^{:key number}
         [:button.btn.btn-raised
          number])]]
     [:h4.text-center "Suggestions? Questions? Comments?"]
     [:div
      [:input.form-control
       {:type "email"
        :placeholder "Your email (optional)"}]
      [:textarea.form-control
       {:placeholder "Your feedback here"}]
      [:button.btn.btn-primary.btn-raised.btn-block
       "Submit"]]]]])

(defn credit-funders []
  (let [mine-name @(subscribe [:current-mine-human-name])]
    [:div.row
     [:div.col-xs-12
      [:h3.text-center (str mine-name " is funded by")]
      [:h3.text-center "InterMine is funded by"]]]))

(defn main []
  [:div.container
   [mine-intro]
   [call-to-action]
   [template-queries]
   [mine-selector]
   [external-tools]
   [feedback]
   [credit-funders]])
