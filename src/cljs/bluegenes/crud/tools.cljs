(ns bluegenes.crud.tools)

(defn normalize-installed-tools
  "Return a map keyed by each tool's `[:names :cljs]`."
  [tools]
  (reduce (fn [m tool]
            (assoc m (get-in tool [:names :cljs]) tool))
          {}
          tools))

(defn update-installed-tools
  [db tools]
  (-> db
      (assoc-in [:tools :installed] tools)))
