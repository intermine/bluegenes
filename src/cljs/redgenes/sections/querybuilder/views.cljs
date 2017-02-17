(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]
            [clojure.string :refer [split join blank?]]
            [oops.core :refer [ocall oget]]
            [clojure.string :as str :refer [starts-with? ends-with?]]
            [redgenes.utils :refer [uncamel]]
            [redgenes.components.bootstrap :refer [tooltip-new]]
            [redgenes.components.ui.constraint :refer [constraint]]
            [imcljs.path :as im-path]
            [cljs.reader :refer [read]]
            [cljs.reader :refer [read-string]]
            [redgenes.components.ui.results_preview :refer [preview-table]]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(def auto (reagent/atom true))





(def aquery {:from            "Gene"
             :constraintLogic "A or B"
             :select          ["symbol"
                               "organism.name"
                               "alleles.name"
                               "alleles.dataSets.description"]
             :where           [{:path  "Gene.symbol"
                                :op    "="
                                :code  "A"
                                :value "zen"}
                               {:path  "Gene.symbol"
                                :op    "="
                                :code  "B"
                                :value "mad"}]})

(defn root-class-dropdown []
  (let [current-mine (subscribe [:current-mine])
        root-class   (subscribe [:qb/root-class])]
    (fn []
      (into [:select.form-control
             {:on-change (fn [e] (dispatch [:qb/set-root-class (oget e :target :value)]))
              :value     @root-class}]
            (map (fn [[class-kw details]]
                   [:option {:value class-kw} (:displayName details)])
                 (sort-by (comp :displayName second) (get-in @current-mine [:service :model :classes])))))))




(defn dotsplit [string] (split string "."))

(defn im-query->map [{:keys [select]}]
  (reduce (fn [total next] (assoc-in total next {})) {} (map dotsplit select)))

(defn attributes-first [[_ v]] (empty? v))

(defn has-children? [[_ v]] (some? (not-empty v)))

(defn within? [haystack needle] (some? (some #{needle} haystack)))

(defn not-selected [selected [k attributes-map]]
  (not (within? selected (name k))))

(def q {:from   "Gene"
        :select ["Gene.symbol"
                 "Gene.secondaryIdentifier"
                 "Gene.organism.name"]})

(defn attribute []
  (let [mappy (subscribe [:qb/mappy])]
    (fn [model [k properties] & [trail]]
      (let [path      (conj trail (name k))
            selected? (get-in @mappy path)]
        [:li.haschildren
         [:p
          [:span
           {:on-click (fn []
                        (if selected?
                          (dispatch [:qb/mappy-remove-view path])
                          (dispatch [:qb/mappy-add-view path])))}
           (if (get-in @mappy path)
             [:i.fa.fa-check-square-o.fa-fw]
             [:i.fa.fa-square-o.fa-fw.light])
           [:span.qb-label (str " " (uncamel (:name properties)))]]]]))))

(defn node [model [k properties] & [trail o]]
  (let [mappy (subscribe [:qb/mappy])
        open? (reagent/atom o)]
    (fn [model [k properties] & [trail]]
      (let [path     (vec (conj trail (name k)))
            selected (get-in @mappy path)]
        [:li.haschildren
         [:p
          [:span {:on-click (fn []
                              ;(reset! auto false)
                              (swap! open? not)
                              )}
           [:i.fa.fa-plus.fa-fw.arr.semilight {:class (when @open? "arrow-down")}]
           [:span.qb-class (uncamel (:name properties))]]]
         (when (or @open? (and @auto selected))
           (into [:ul]
                 (concat
                   (map (fn [i] [attribute model i path]) (sort (im-path/attributes model (:referencedType properties))))
                   (map (fn [i] [node model i path false]) (sort (im-path/relationships model (:referencedType properties)))))))]))))

(defn model-browser []
  (fn [model root-class]
    [:div.model-browser
     (let [path [root-class]]
       (into [:ul]
             (concat
               (map (fn [i] [attribute model i path]) (sort (im-path/attributes model root-class)))
               (map (fn [i] [node model i path]) (sort (im-path/relationships model root-class))))))]))


(defn dissoc-keywords [m]
  (apply dissoc m (filter keyword? (keys m))))

(defn queryview-node []
  (fn [model [k properties] & [trail]]
    (let [path (vec (conj trail (name k)))]
      [:li.haschildren
       [:p.flexmex
        [:span.lab {:class (if (im-path/class? model (join "." path)) "qb-class" "qb-attribute")}
         [:span.qb-label {:style {:margin-left 5}} [:a (uncamel k)]]
         [:i.fa.fa-trash-o.fa-fw.semilight {:on-click (fn [] (dispatch [:qb/mappy-remove-view path]))}]
         [:span
          {:on-click (fn [] (dispatch [:qb/mappy-add-constraint path]))}
          [:span [:span [:i.fa.fa-filter.semilight]]]]
         (when-let [c (:id-count properties)]
           [:span.label.label-soft
            {:class (when (= 0 c) "label-no-results")
             :style {:margin-left 5}} (str c " row" (when (not= c 1) "s"))])]]
       (when-let [constraints (:constraints properties)]
         (into [:ul]
               (map-indexed (fn [idx con]
                              [:li.haschildren
                               [:p
                                [:div.contract
                                 {:class (when (empty? (:value con)) "empty")}
                                 [constraint
                                  :model model
                                  :path (join "." path)
                                  ;:lists (second (first @lists))
                                  :code (:code con)
                                  :on-remove (fn [] (dispatch [:qb/mappy-remove-constraint path idx]))
                                  ;:possible-values (map :value possible-values)
                                  :value (:value con)
                                  :op (:op con)
                                  :on-change (fn [c]
                                               (dispatch [:qb/mappy-update-constraint path idx c]))
                                  :on-blur (fn [x] (dispatch [:qb/mappy-build-im-query]))
                                  ;(dispatch [:qb/build-im-query])

                                  :label? false]]]]) constraints)))
       (when (not-empty properties)
         (let [classes    (filter (fn [[k p]] (im-path/class? model (join "." (conj path k)))) (dissoc-keywords properties))
               attributes (filter (fn [[k p]] ((complement im-path/class?) model (join "." (conj path k)))) (dissoc-keywords properties))]
           (into [:ul]
                 (concat
                   (map (fn [n] [queryview-node model n path]) (sort attributes))
                   (map (fn [n] [queryview-node model n path]) (sort classes))))))])))

(defn queryview-browser []
  (let [mappy (subscribe [:qb/mappy])]
    (fn [model]
      (if (not-empty @mappy)
        [:div.query-browser
         (into [:ul]
               (map (fn [n]
                      [queryview-node model n]) @mappy))]
        [:div "Please select an attribute."]))))

(defn listify [logic-vector]
  (let [v logic-vector]
    (let [first-and (first (keep-indexed (fn [idx e] (if (some #{e} #{'and 'AND}) idx)) v))
          first-or  (or (first (keep-indexed (fn [idx e] (if (and (some #{e} #{'or 'OR}) (>= idx first-and)) idx)) v)) (count v))]
      (if first-and
        (let [trunk     (subvec v 0 (dec first-and))
              anded     (subvec v (dec first-and) first-or)
              remaining (subvec v first-or (count v))
              fixed     (reduce conj (conj trunk anded) remaining)]
          (recur fixed))
        (do
          (if (and (= 1 (count v)) (vector? (first v)))
            (first v)
            v))))))

(defn add-code [l code]
  (listify (reduce conj l [:and code])))


(defn wrap-string-in-parens [string]
  (if (and (starts-with? string "(") (ends-with? string ")"))
    string
    (str "(" string ")")))

(defn replace-parens-with-brackets [string]
  (-> string
      (clojure.string/replace (re-pattern "\\[") "(")
      (clojure.string/replace (re-pattern "\\]") ")")))

(defn stringify-vec [v]
  (apply str (interpose " " v)))

(defn lists-to-vectors [s]
  (clojure.walk/prewalk (fn [e] (if (list? e) (vec e) e)) s))


(defn format-logic-string [string]
  (->> string
       wrap-string-in-parens
       read-string
       vec
       listify
       stringify-vec
       replace-parens-with-brackets))


(def <sub (comp deref subscribe))

(defn append-code [v code]
  (vec (concat v ['and code])))

(defn index-of [haystack needle]
  (first (keep-indexed (fn [idx e] (when (= needle e) idx)) haystack)))

(defn group-ands
  "Recurisvely groups entities in a vector that are connected by the 'and symbol"
  [v]
  (let [first-part (vec (take (dec (index-of v 'and)) v))
        grouped    (vec (take-while (partial not= 'or) (drop (count first-part) v)))
        end        (take-last (- (count v) (+ (count first-part) (count grouped))) v)
        grouped (if (= 1 (count grouped)) (first grouped) grouped)]
    (let [final (reduce conj (conj first-part grouped) end)]
      (if (index-of final 'and) (recur final) final))))

(defn without-operators [col]
  (vec (filter (fn [item] (not (some? (some #{item} #{'and 'or})))) col)))

(defn single-vec-of-vec? [item]
  "Is the item a vector containing one vector? [[A]]"
  (and (= (count item) 1) (vector? (first item))))

(defn single-vec-of-symbol? [item]
  "Is the item a vector containing a symbol? ['A]"
  (and (= (count item) 1) (symbol? (first item))))

(defn remove-code
  "Recursively removes a symbol from a tree and raises neighbours with a count of one"
  [v code]
  (clojure.walk/postwalk
    (fn [e]
      (if (vector? e)
        (let [removed     (vec (remove (partial = code) e))
              without-ops (without-operators removed)]
          (cond
            (single-vec-of-vec? without-ops) (vec (mapcat identity without-ops))
            (single-vec-of-symbol? without-ops) (first without-ops)
            :else removed))
        e))
    v))

(defn vec->list
  "Recursively convert vectors to lists"
  [v]
  (clojure.walk/postwalk (fn [e] (if (vector? e) (apply list e) e)) v))

(defn list->vec
  "Recursively convert lists to vectors"
  [v]
  (clojure.walk/postwalk (fn [e] (if (list? e) (vec e) e)) v))





(defn logic-box-2 []
  (let [
        ;logic     (reagent/atom ['A 'or ['B 'and ['C 'and 'B]]])
        logic     (reagent/atom ['A 'and 'B 'or ['C 'and 'D]])

        str-value (reagent/atom nil)]
    (fn []
      [:div
       [:pre (str @logic)]
       [:input.form-control
        {:style     {:max-width "300"}
         :type      "text"
         :value     @str-value
         :on-change (fn [e] (reset! str-value (oget e :target :value)))}]
       [:button.btn.btn-primary "Validate"]
       [:button.btn.btn-primary
        {:on-click (fn [] (swap! logic vec->list))} "LIST"]
       [:button.btn.btn-primary
        {:on-click (fn [] (println "READ" (map type (read-string "(A or B and C)"))))} "READ"]
       [:button.btn.btn-primary
        {:on-click (fn [] (println "without code" (->
                                                    (remove-code @logic 'D)
                                                    ;raise
                                                    )))} "Remove Code"]
       [:button.btn.btn-primary
        {:on-click (fn [] (println "grouping" (group-ands @logic)))} "Group"]
       [:button.btn.btn-primary
        {:on-click (fn [] (println (group-ands (append-code @logic 'X))))} "Add Code"]
       ])))

(defn logic-box []
  (let [logic (subscribe [:qb/constraint-logic])]
    (fn []
      [:div
       [:input.form-control
        {:style     {:max-width "300"}
         :type      "text"
         :value     @logic
         :on-blur   (fn [] (dispatch [:qb/update-constraint-logic (format-logic-string @logic)]))
         :on-change (fn [e] (dispatch [:qb/update-constraint-logic (oget e :target :value)]))}]])))

(defn controls []
  [:div.button-group
   [:button.btn.btn-primary.btn-raised
    {:on-click (fn [] (dispatch [:qb/load-query aquery]))}
    ;{:on-click (fn [] (println "finished" (listify nil)))}

    "Example"]
   [:button.btn.btn-primary.btn-raised
    {:on-click (fn [] (dispatch [:qb/mappy-build-im-query]))}
    ;{:on-click (fn [] (println "finished" (listify nil)))}

    "Show Results"]])

(defn preview [result-count]
  (let [results-preview   (subscribe [:qb/preview])
        fetching-preview? (subscribe [:idresolver/fetching-preview?])]
    [preview-table
     :loading? @fetching-preview?
     :query-results @results-preview]))



(defn drop-nth [n coll] (vec (keep-indexed #(if (not= %1 n) %2) coll)))

(defn sortable-list []
  (let [order (subscribe [:qb/order])
        state (reagent/atom {:items ["A" "B" "C" "D"] :selected nil})]
    (fn []
      (into [:div.sort-order-container
             {:class (when (some? (:selected @state)) "dragtest")}]
            (map-indexed
              (fn [idx i]
                [:div {:class         (when (= idx (:selected @state)) "dragging")
                       :draggable     true
                       :on-drag-start (fn [e]
                                        (ocall e "dataTransfer.setData" "banana" "cakes")
                                        (swap! state assoc :selected idx))
                       :on-drag-enter (fn [] (let [selected-idx  (:selected @state)
                                                   items         @order
                                                   selected-item (get items selected-idx)
                                                   [before after] (split-at idx (drop-nth selected-idx items))]
                                               #_(swap! state assoc
                                                        :selected idx
                                                        :items (vec (concat (vec before) (vec (list selected-item)) (vec after))))
                                               (swap! state assoc :selected idx)
                                               (dispatch [:qb/set-order (vec (concat (vec before) (vec (list selected-item)) (vec after)))])))
                       :on-drag-end   (fn [] (swap! state assoc :selected nil))}
                 (map (fn [part] [:span.part part]) (interpose ">" (map uncamel (split i "."))))])) @order
            ))))

(defn main []
  (let [query        (subscribe [:qb/query])
        current-mine (subscribe [:current-mine])
        root-class   (subscribe [:qb/root-class])]
    (reagent/create-class
      {:component-did-mount (fn [x]
                              (when (empty? @query)
                                (dispatch [:qb/set-root-class "Gene"])))
       :reagent-render      (fn []
                              [:div.column-container
                               [:div.model-browser-column
                                [:div.container-fluid
                                 [:h4 "Model Browser"]
                                 [:span "Starting with..."]
                                 [root-class-dropdown]
                                 [model-browser (:model (:service @current-mine)) (name @root-class)]]]
                               [:div.query-view-column
                                [:div.container-fluid
                                 [:div.row
                                  [:div.col-md-6
                                   [:div
                                    [:h4 "Query"]
                                    [queryview-browser (:model (:service @current-mine))]
                                    [:h4 "Constraint Logic"]
                                    [logic-box-2]]]
                                  [:div.col-md-6
                                   [:div
                                    [:div
                                     [:h4 "Column Order"]
                                     [sortable-list]]
                                    [controls]
                                    ]]]]]])})))




;[:h4 "Preview"]

;[preview]




(defn toggle-all-checkbox []
  [:span
   [:input {:type    "checkbox"
            :checked @(subscribe [:todos/all-complete?])}]
   [:label {:for "toggle-all"} "Mark all as complete"]])