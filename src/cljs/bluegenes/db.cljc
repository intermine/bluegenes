(ns bluegenes.db)

(def default-db
  {:current-route nil
   :current-mine :default
   :fetching-assets? true
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
            :list-operations {:selected #{}}}
   :lists {:controls {:filters {:keywords ""
                                :lists nil
                                :date nil
                                :type nil
                                :tags nil}
                      :sort {:column :timestamp
                             :order :desc}}
           :expanded-paths #{}}

   :qb {:root-class :Gene
        :order []
        :qm nil
        ;;what is this??
        :mappy {"Gene" {"secondaryIdentifier" {}, "organism" {"name" {}}, "symbol" {}}}}})
