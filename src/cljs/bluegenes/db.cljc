(ns bluegenes.db)

(def default-db
  {:current-route nil
   :current-mine :default
   :fetching-assets? true
   :fetching-report? true
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
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
   ;; If you change this, you should change `remove-stateful-keys-from-db` too.
   :lists {:pagination {:per-page 20
                        :current-page 1}
           :controls {:filters {:keywords ""
                                :lists nil
                                :date nil
                                :type nil
                                :tags nil}
                      :sort {:column :timestamp
                             :order :desc}}
           :selected-lists #{}
           :expanded-paths #{}}

   :qb {:root-class :Gene
        :order []
        :qm nil
        ;;what is this??
        :mappy {"Gene" {"secondaryIdentifier" {}, "organism" {"name" {}}, "symbol" {}}}}})
