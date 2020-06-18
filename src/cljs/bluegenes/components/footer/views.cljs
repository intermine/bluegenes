(ns bluegenes.components.footer.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.version :as version]
            [clojure.string :as str]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [version-string->vec]]
            [bluegenes.components.icons :refer [icon]]))

(defn link [href label]
  [:a {:href href :target "_blank"} label])

(defn pretty-version [vstring]
  (->> vstring version-string->vec (str/join ".")))

(defn main []
  (let [short-version (nth (re-matches #"v?([0-9\.]+)-.*" version/release) 1 "dev")
        intermine-version (pretty-version @(subscribe [:version]))
        api-version (pretty-version @(subscribe [:api-version]))]
    [:footer.footer
     [:div.section.column
      [:span.version (str "BlueGenes " short-version " powered by ")
       [poppable {:data [:span (str "Version: " intermine-version)
                         [:br] (str "API: " api-version)]
                  :children [link "http://www.intermine.org" "InterMine"]}]]
      [:div.column.inner
       [:span.thin "FUNDED BY"]
       [:span
        [link "https://www.nih.gov/" "NIH"]
        [:span.thin " | "]
        [link "https://www.wellcome.ac.uk/" "Wellcome Trust"]]]]
     [:div.section
      [link "https://github.com/intermine/" [icon "github" 2]]
      [link "mailto:info@intermine.org" [icon "mail" 2]]
      [link "https://intermineorg.wordpress.com/" [icon "blog" 2]]
      [link "https://twitter.com/intermineorg/" [icon "twitter" 2]]
      [link "http://chat.intermine.org/" [icon "discord" 2]]]
     [:div.section.column
      [link "https://intermineorg.wordpress.com/cite/" "CITE US!"]
      [link "https://intermine.readthedocs.io/en/latest/about/" "ABOUT US"]
      [link "https://intermine.readthedocs.io/en/latest/about/privacy-policy/" "PRIVACY POLICY"]]]))
