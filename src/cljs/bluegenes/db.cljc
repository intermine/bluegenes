(ns bluegenes.db)

(def default-db
  {:name "Intermine"
   :mine-name :flymine
   ;;events/boot.cljs auto-selects the first mine available if current-mine
   ;;doesn't exist for any reason.
   :current-mine :flymine-beta
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
   :databrowser/whitelist #{:Gene :Author :Protein :Organism :Publication :GOAnnotation :GOTerm :Homologue :Interaction :DataSet :genes :authors :proteins :organisms :publications :goAnnotation :goTerms :homologues :dataSets :interactions}
   :databrowser/root nil ;The default place to start in the data browser at /explore
   :databrowser/node-locations {:Homologue {:x 120 :y 224 :radius 50} :Protein {:x 200 :y 300 :radius 17}}
   :results {:history []}
   :search {:selected-results #{}}

   :idresolver {:stage {:files nil
                        :textbox nil
                        :options {:case-sensitive false}
                        :status nil
                        :flags nil}
                :to-resolve {:total nil
                             :identifiers []}
                :save {:list-name nil}
                :response nil}

   :mymine {:dragging nil
            :dragging-over nil
            :selected #{}
            :focus [:unsorted]
            :stage-operation nil
            :checked #{}
            :sort-by {:key :label
                      :type :alphanum
                      :asc? true}
            :tree {}
            :tree-old {"test-folder" {:file-type :folder
                                      :open true
                                      :label "My Project"
                                      :children {67000011 {:file-type :list
                                                           :id 67000011}}}
                       "another-folder" {:file-type :folder
                                         :open true
                                         :label "My Stuff"
                                         :children {63000014 {:file-type :list
                                                              :id 63000014}}}}
            :list-operations {:selected #{}}}
   :lists {:controls {:filters {:text-filter nil
                                :flags {:authorized nil
                                        :favourite nil}}
                      :sort {:title :asc}}}

   :qb {:root-class :Gene
        :order []
        :qm nil
        :mappy {"Gene" {"secondaryIdentifier" {}, "organism" {"name" {}}, "symbol" {}}}}})
