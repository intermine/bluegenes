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
  (let [short-version     (nth (re-matches #"v?([0-9\.]+)-.*" version/release) 1 "dev")
        ;; Note that the following versions can be nil when switching mines.
        intermine-version (some-> @(subscribe [:version]) pretty-version)
        api-version       (some-> @(subscribe [:api-version]) pretty-version)
        release-version   (when-let [v @(subscribe [:release-version])]
                            (nth (re-find #"\"(.*)\"" v) 1 (str/trim v)))]
    [:footer.footer
     [:div.section.column
      [:span.version (str "BlueGenes " short-version " powered by ")
       [poppable {:data [:span (str "Version: " intermine-version)
                         [:br] (str "API: " api-version)
                         [:br] (str "Build: " release-version)]
                  :children [link "http://www.intermine.org" "InterMine"]}]]
      [:div.column.inner
       [:span.thin "FUNDED BY"]
       [:span
        [link "https://www.nih.gov/" "NIH"]
        [:span.thin " | "]
        [link "https://www.wellcome.ac.uk/" "Wellcome Trust"]]]]
     [:div.section
      [link "https://github.com/intermine/"
       [poppable {:data [:span "Check out our open-source software"]
                  :children [icon "github" 2]}]]
      [link "mailto:info@intermine.org"
       [poppable {:data [:span "Send us an email"]
                  :children [icon "mail" 2]}]]
      [link "https://intermineorg.wordpress.com/"
       [poppable {:data [:span "Read our blog"]
                  :children [icon "blog" 2]}]]
      [link "https://twitter.com/intermineorg/"
       [poppable {:data [:span "Follow us on Twitter"]
                  :children [icon "twitter" 2]}]]
      [link "http://chat.intermine.org/"
       [poppable {:data [:span "Chat with us on Discord"]
                  :children [icon "discord" 2]}]]]
     [:div.section.column
      [link "https://intermineorg.wordpress.com/cite/" "CITE US!"]
      [link "https://intermine.readthedocs.io/en/latest/about/" "ABOUT US"]
      [link "https://intermine.readthedocs.io/en/latest/about/privacy-policy/" "PRIVACY POLICY"]]]))
