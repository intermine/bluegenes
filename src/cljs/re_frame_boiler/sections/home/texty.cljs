(ns re-frame-boiler.sections.home.texty
  (:require [re-frame.core :as re-frame]
            [re-frame-boiler.components.search :as search]
            [re-frame-boiler.components.templates.views :as t]))

(defn searchbox []
[:div.search
  [search/main]
  [:div.info
   [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
   " Search for genes, proteins, pathways, ontology terms, authors, etc."]])

(defn lists []
  [:div.feature.lists
  [:h3 "Lists"]
    [:div.upload
      [:div.piccie [:svg.icon.icon-search [:use {:xlinkHref "#icon-file"}]] ]
      [:p "Drag a .txt file of identifiers here, or " [:button {:type "button"} "browse"] " for a file."
      [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]]]
    [:div.divider [:p " OR " ]]
    [:div [:textarea {:cols 15 :rows 3 :placeholder "Type or paste a list of identifiers here, e.g. EVE, FKH, ADH, GATA1... "}]
    [:div.actions [:a "Show me an example"] [:button "Search"]]]
    [:div.divider [:p " OR " ]]
    [:div.browse "Browse popular lists, e.g. "
      [:table [:tbody
        [:tr
          [:td.data-type.Genes "70 Genes"]
          [:td [:a.Genes {:href "nope dude"} "GWAS_atherosclerosis"]]]
        [:tr
          [:td.data-type.Snps "29 SNPs"]
          [:td [:a {:href "nope dude"} "PL_T2DcandSNPs_Voight2010" ]]]
        [:tr
          [:td.data-type.Genes "9 Genes"]
          [:td [:a.Genes {:href "nope dude"} "PL_obesityMonogen_ORahilly09" ]]]
        [:tr
          [:td.data-type.Proteins "53 Proteins"]
          [:td [:a.Proteins {:href "nope dude"} "integrativeOmics_proteinlist"]]]
    ]]]
   ])


 (defn templates []
   [:div.feature.templates
   [:h3
      "Templates"]
   [:h4.subtitle "Example Searches"]
     [:div
        [:a.q {:href "nope"} "Genes‑>Proteins"]
        [:a.w {:href "nope"} "Pathway‑>Genes"]
        [:a.e {:href "nope"} "Gene‑>Gene expression"]
        [:a.w {:href "nope"} "GO Term‑>Genes"]
        [:a.q {:href "nope"} "Gene‑>Interactions"]
        [:a.e {:href "nope"} "Disease expression‑>Genes"]

        [:a.w {:href "nope"} "SNP‑>Gwas hits"]
        [:a.r {:href "nope"} "Gene + Pathway‑>Interactions"]
        [:a.e {:href "nope"} "Gene‑>Chromosomal locations"]


      [:a.w {:href "nope"} "Protein‑>Complex"]
        [:a.r {:href "nope"} "Gene‑>Orthologues"]
      ]
      [:h4
       [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]
       "Browse all templates"]
      [:h4
       [:svg.icon.icon-filter [:use {:xlinkHref "#icon-filter"}]]
       "Search / filter templates"]
    ])


(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:main.home.texty
        [searchbox]
        [:div.features
          [templates]
         ;[t/main]
          [lists]
        ]

       ])))
