(ns bluegenes.querybuilder-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.pages.querybuilder.logic :as logic]
            [bluegenes.pages.querybuilder.events :as events]))

(def qb-menu
  {"Gene" {"alleles" {}
           "childFeatures" {"organism" {}
                            :subclass "Chromosome"
                            "locations" {}}
           "chromosome" {}
           "interactions" {"participant2" {:subclass "Gene"}}}})

(def qb-menu-multiple-parent-subclasses
  {"Gene" {"alleles" {}
           "childFeatures" {"organism" {}
                            :subclass "Chromosome"
                            "locations" {}}
           "chromosome" {}
           "interactions" {"participant2" {:subclass "Gene"
                                           "childFeatures" {:subclass "Allele"}}}}})

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

(deftest get-all-subclasses
  (is (= (events/get-all-subclasses qb-menu-multiple-parent-subclasses
                                    ["Gene" "interactions" "participant2" "childFeatures" "name"])
         '([["Gene" "interactions" "participant2" "childFeatures"] "Allele"]
           [["Gene" "interactions" "participant2"] "Gene"]))))
