(ns redgenes.db)

(def default-db
  {:name "Intermine"
   :mine-url "http://www.flymine.org/query"
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
   :databrowser/whitelist #{:Gene :Author :Protein :Organism :Publication :GOAnnotation :GOTerm :Homologue :DataSet :genes :authors :proteins :organisms :publications :goAnnotation :goTerms :homologues :dataSets}
   :databrowser/root :Gene ;The default place to start in the data browser at /explore
   })
