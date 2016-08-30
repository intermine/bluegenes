(ns redgenes.sections.help.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [redgenes.components.icons :as icons]
        ))



(defn main []
  (fn []
    [:div.approot.red
     [icons/icons]
     [:h1 "Help"]
     [:p "BlueGenes is a new InterMine interface intended to look modern and exciting. It's actively under development, so there may be some interesting bugs - but feel free to have a look around, suggest improvements, report bugs, etc. " [:a {:href "http://intermine.readthedocs.io/en/latest/about/contact-us/"} "Contact us"] " if you'd like to discuss anything"]
     ]))
