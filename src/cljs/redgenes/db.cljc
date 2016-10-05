(ns redgenes.db)

(def default-db
  {:name "Intermine"
  ; :mine-url "http://www.flymine.org/query"
  ; :mine-url "http://www.mousemine.org/mousemine/service/query/results"
   :mine-url "http://www.humanmine.org/humanmine"
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
   :databrowser/whitelist #{:Gene :Author :Protein :Organism :Publication :GOAnnotation :GOTerm :Homologue :Interaction :DataSet :genes :authors :proteins :organisms :publications :goAnnotation :goTerms :homologues :dataSets :interactions}
   :databrowser/root :Gene ;The default place to start in the data browser at /explore
   :databrowser/node-locations {:Homologue {:x 20 :y 30} :Protein {:x 200 :y 300}}
   })
