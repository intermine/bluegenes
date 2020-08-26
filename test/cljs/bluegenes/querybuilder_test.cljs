(ns bluegenes.querybuilder-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.pages.querybuilder.logic :as logic]))

(def qb-menu
  {"Gene" {"alleles" {}
           "childFeatures" {"organism" {}
                            :subclass "Chromosome"
                            "locations" {}}
           "chromosome" {}
           "interactions" {"participant2" {:subclass "Gene"}}}})

(def qb-menu-no-subclass
  {"Gene" {"alleles" {}
           "childFeatures" {"organism" {}
                            "locations" {}}
           "chromosome" {}
           "interactions" {"participant2" {}}}})

(deftest qb-menu->type-constraints
  (is (= (logic/qb-menu->type-constraints qb-menu)
         [{:path "Gene.childFeatures", :type "Chromosome"}
          {:path "Gene.interactions.participant2", :type "Gene"}]))
  (is (= (logic/qb-menu->type-constraints qb-menu-no-subclass)
         [])))
