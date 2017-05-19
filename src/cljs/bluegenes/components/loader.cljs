(ns bluegenes.components.loader)

(defn loader [whatever]
  [:div
   [:div.loading (str "LOADING " whatever)]
   [:div#loader
    [:div.worm.loader-organism]
    [:div.zebra.loader-organism]
    [:div.human.loader-organism]
    [:div.yeast.loader-organism]
    [:div.rat.loader-organism]
    [:div.mouse.loader-organism]
    [:div.fly.loader-organism]]])

(defn mini-loader [size]
   [:div.mini-loader {:class size}
    [:div.mini-loader-content
     [:div.loader-organism.one]
     [:div.middle [:div.loader-organism.two]
      [:div.loader-organism.dot]
      [:div.loader-organism.three]]
     [:div.loader-organism.four]]])
