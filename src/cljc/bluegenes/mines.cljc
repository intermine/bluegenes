
(ns bluegenes.mines)

(def mines {:humanmine     {:id       :humanmine
                            :name "HumanMine"
                            :service {:root "www.humanmine.org/humanmine"
                                      :token nil}}

            :flymine       {:id :flymine
                            :name "FlyMine"
                            :service {:root "www.flymine.org/query"
                                      :token nil}}

            :humanmine-beta {:id      :humanmine-beta
                             :name "HumanMine Beta"
                             :service  {:root "beta.humanmine.org/beta"
                                        :token nil}}

            :flymine-beta   {:id         :flymine-beta
                             :name "FlyMine Beta"
                             :service    {:root "beta.flymine.org/beta"
                                          :token nil}}
            :default        {:id         :default
                             :name "FlyMine Beta"
                             :service    {:root "beta.flymine.org/beta"
                                          :token nil}}})
