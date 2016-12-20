(ns redgenes.components.loader)

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
