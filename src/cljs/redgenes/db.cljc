(ns redgenes.db)

(def default-db
  {:name                       "Intermine"
   :short-name                 "IM"
   :mine-name                  :fly
   :saved-data                 {:items {}}
   :quicksearch-selected-index -1                           ;;this defaults to select all in the quicksearch
   :databrowser/whitelist      #{:Gene :Author :Protein :Organism :Publication :GOAnnotation :GOTerm :Homologue :Interaction :DataSet :genes :authors :proteins :organisms :publications :goAnnotation :goTerms :homologues :dataSets :interactions}
   :databrowser/root           nil                          ;The default place to start in the data browser at /explore
   :databrowser/node-locations {:Homologue {:x 120 :y 224 :radius 50} :Protein {:x 200 :y 300 :radius 17}}
   :results                    {:history []}
   :lists                      {:controls {:filters {:text-filter nil}
                                           :sort    {:title :asc}}}
   :query-builder
                               {
                                :count  0
                                :dcount 0
                                :query
                                        {
                                         :q/select         #{}
                                         :q/where          []
                                         :constraint-paths #{}
                                         }
                                }
   })
