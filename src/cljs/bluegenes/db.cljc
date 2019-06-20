(ns bluegenes.db)

(def default-db
  {;;events/boot.cljs auto-selects the first mine available if current-mine
   ;;doesn't exist for any reason.
   :current-route nil
   :current-mine :flymine-beta
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
   :lists {:controls {:filters {:text-filter nil
                                :flags {:authorized nil
                                        :favourite nil}}
                      :sort {:title :asc}}}

   :qb {:root-class :Gene
        :order []
        :qm nil
        ;;what is this??
        :mappy {"Gene" {"secondaryIdentifier" {}, "organism" {"name" {}}, "symbol" {}}}}})
