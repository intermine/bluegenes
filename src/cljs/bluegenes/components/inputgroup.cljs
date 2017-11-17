(ns bluegenes.components.inputgroup)

(defn group [properties content]
  [:div.input-group properties
    [:input {:type "text"}]
    [:div.content content]]
  )
