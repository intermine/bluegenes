(ns bluegenes.components.footer.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.version :as version]
            [clojure.string :as str]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [version-string->vec]]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.route :as route]))

(def defaults
  {:email "info@intermine.org"
   :twitter "intermineorg"
   :citation "https://intermineorg.wordpress.com/cite/"})

(defn link [href label]
  [:a {:href href :target "_blank"} label])

(defn pretty-version [vstring]
  (->> vstring version-string->vec (str/join ".")))

(defn main []
  (let [mine-name         @(subscribe [:current-mine-human-name])
        mine-twitter      @(subscribe [:registry/twitter])
        mine-email        @(subscribe [:registry/email])
        mine-citation     @(subscribe [:current-mine/citation])
        short-version     (nth (re-matches #"v?([0-9\.]+)-.*" version/release) 1 "dev")
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
      [link "https://github.com/intermine/"
       [poppable {:data [:span "Check out our open-source software"]
                  :children [icon "github" 2]}]]
      [link (str "mailto:" (or mine-email (:email defaults)))
       [poppable {:data [:span (if (and mine-email (not= mine-email (:email defaults)))
                                 (str "Send " mine-name " an email")
                                 "Send us an email")]
                  :children [icon "mail" 2]}]]
      [link "https://intermineorg.wordpress.com/"
       [poppable {:data [:span "Read our blog"]
                  :children [icon "blog" 2]}]]
      [link (str "https://twitter.com/" (or mine-twitter (:twitter defaults)))
       [poppable {:data [:span (if (and mine-twitter (not= mine-twitter (:twitter defaults)))
                                 (str "Follow " mine-name " on Twitter")
                                 "Follow us on Twitter")]
                  :children [icon "twitter" 2]}]]
      [link "http://chat.intermine.org/"
       [poppable {:data [:span "Chat with us on Discord"]
                  :children [icon "discord" 2]}]]]
     [:div.section.column
      [link (or mine-citation (:citation defaults))
       (str "CITE " (some-> mine-name str/upper-case))]
      [link "https://intermine.readthedocs.io/en/latest/about/" "ABOUT US"]
      [link "https://intermine.readthedocs.io/en/latest/about/privacy-policy/" "PRIVACY POLICY"]]]))
