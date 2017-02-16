(ns redgenes.db)

(def default-db
  {:name                       "Intermine"
   :mine-name                  :fly
   ;;events/boot.cljs auto-selects the first mine available if current-mine
   ;;doesn't exist for any reason.
   :current-mine               :flymine-beta
   :saved-data                 {:items {}}
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
   :databrowser/whitelist      #{:Gene :Author :Protein :Organism :Publication :GOAnnotation :GOTerm :Homologue :Interaction :DataSet :genes :authors :proteins :organisms :publications :goAnnotation :goTerms :homologues :dataSets :interactions}
   :databrowser/root           nil ;The default place to start in the data browser at /explore
   :databrowser/node-locations {:Homologue {:x 120 :y 224 :radius 50} :Protein {:x 200 :y 300 :radius 17}}
   :results                    {:history []}
   :lists                      {:controls {:filters {:text-filter nil
                                                     :flags       {:authorized nil
                                                                   :favourite  nil}}
                                           :sort    {:title :asc}}}

   :qb                         {:root-class :Gene
                                :order []
                                :qm         nil
                                :mappy      {"Gene" {"secondaryIdentifier" {}, "organism" {"name" {}}, "symbol" {}}}}})




{"Gene" {:visible     true
         :constraints []
         "symbol"     {:visible     true
                       :constraints [{:op    "="
                                      :value "zen"}]}
         "organism"   {"name" {:constraints []
                               :visible     true}}
         "alleles"    {"name"     {:visible true}
                       "dataSets" {"description" {:visible true}}}}}