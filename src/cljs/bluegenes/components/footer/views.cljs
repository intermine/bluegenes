(ns bluegenes.components.footer.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.version :as version]
            [clojure.string :as str]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [version-string->vec]]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.route :as route]))

(defn link [href label]
  [:a {:href href :target "_blank"} label])

(defn pretty-version [vstring]
  (->> vstring version-string->vec (str/join ".")))

(defn main []
  (let [short-version     (nth (re-matches #"v?([0-9\.]+)(?:-.*)?" version/release) 1 "dev")
        ;; Note that the following versions can be nil when switching mines.
        intermine-version (some-> @(subscribe [:version]) pretty-version)
        api-version       @(subscribe [:api-version])
        release-version   (when-let [v @(subscribe [:release-version])]
                            (nth (re-find #"\"(.*)\"" v) 1 (str/trim v)))]
    [:footer.footer
     [:div.section.column
      [:span.version
       "BlueGenes "
       [:span
        ;; Secret way to access developer page. Sshhhh.
        {:on-click #(dispatch [::route/navigate ::route/debug {:panel "main"}])}
        short-version]
       " powered by "
       [poppable {:data [:span (str "Version: " intermine-version)
                         [:br] (str "API: " api-version)
                         [:br] (str "Build: " release-version)]
                  :children [link "http://www.intermine.org" "InterMine"]}]]
      [:div.column.inner
       [:span.thin "FUNDED BY"]
       [:span
        [link "https://www.wellcome.ac.uk/" "Wellcome Trust"]
        [:span.thin " | "]
        [link "https://www.nih.gov/" "NIH"]
        [:span.thin " | "]
        [link "https://bbsrc.ukri.org/" "BBSRC"]]]]
     [:div.section
      (when-let [github-url @(subscribe [:current-mine/url :github])]
        [link github-url
         [poppable {:data [:span "Check out our open-source software"]
                    :children [icon "github" 2]}]])
      (when-let [email @(subscribe [:current-mine/support-email])]
        [link (str "mailto:" email)
         [poppable {:data [:span "Send us an email"]
                    :children [icon "mail" 2]}]])
      (when-let [news-url @(subscribe [:current-mine/news])]
        [link news-url
         [poppable {:data [:span "Read our blog"]
                    :children [icon "blog" 2]}]])
      (when-let [twitter-url @(subscribe [:current-mine/url :twitter])]
        [link twitter-url
         [poppable {:data [:span "Follow us on Twitter"]
                    :children [icon "twitter" 2]}]])
      (when-let [discord-url @(subscribe [:current-mine/url :discord])]
        [link discord-url
         [poppable {:data [:span "Chat with us on Discord"]
                    :children [icon "discord" 2]}]])]
     [:div.section.column
      [link @(subscribe [:current-mine/citation])
       (str "CITE " (some-> @(subscribe [:current-mine-human-name]) str/upper-case))]
      [link @(subscribe [:current-mine/url :aboutUs]) "ABOUT US"]
      [link @(subscribe [:current-mine/url :privacyPolicy]) "PRIVACY POLICY"]]]))
