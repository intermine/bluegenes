(ns bluegenes.sections.help.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [bluegenes.components.icons :as icons]
        ))



(defn main []
  (fn []
    [:div.approot.red
     [icons/icons]
     [:h1 "Help"]
     [:p "bluegenes is a new InterMine interface that operates via InterMine's Web Services. It's actively under development, so there may be some interesting bugs - but feel free to have a look around, "[:a {:href "https://github.com/intermine/bluegenes/issues"} "suggest improvements, report bugs,"] " etc. " [:a {:href "http://intermine.readthedocs.io/en/latest/about/contact-us/"} "Contact us"] " if you'd like to discuss anything"]
     [:h2 "Credits"]
     [:h3 "Design"]
     [:p "Icons are a mix of " [:a {:href "http://fontawesome.io/"} "fontawesome"]" and "[:a {:href "https://icomoon.io/"}"icomoon"] " free icons. We also use the "[:a {:href "http://fezvrasta.github.io/bootstrap-material-design/"} "bootstrap material design theme"] "."]
     [:h3 "Licence"]
     [:p "bluegenes, like InterMine, is released under the "[:a {:href "https://tldrlegal.com/license/gnu-lesser-general-public-license-v2.1-(lgpl-2.1)"}"LGPL 2.1 licence"]"."]
     ]))
