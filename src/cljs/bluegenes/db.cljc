(ns bluegenes.db)

(def default-db
  {:current-route nil
   :current-mine nil
   ;; fetching assets needs to be true so we can block `:set-active-panel`
   ;; event until we have `:finished-loading-assets`, as some routes might
   ;; attempt to build a query which is dependent on `db.assets.summary-fields`
   ;; before it gets populated by `:assets/success-fetch-summary-fields`.
   :fetching-assets? true
   :fetching-report? true
   :quicksearch-selected-index -1 ;;this defaults to select all in the quicksearch
   :results {:history []}
   :search {:selected-results #{}}
   :idresolver {:stage {:files nil
                        :textbox nil
                        :options {:case-sensitive false
                                  :type "Gene"
                                  :organism nil}
                        :status nil
                        :flags nil}
                :to-resolve {:total nil
                             :identifiers []}
                :save {:list-name nil}
                :response nil
                :error nil}
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

   :qb {:constraint-logic nil
        :order []
        :sort []
        :joins #{}
        :preview nil
        :im-query nil
        :enhance-query {}
        :root-class nil}

   :admin {:active-pill :admin.pill/report}})
