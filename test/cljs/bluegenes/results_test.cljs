(ns bluegenes.results-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.pages.results.events :refer [unorder-query-parts]]))

(def query-parts
  {:Gene [{:path "Gene.id",
           :type :Gene,
           :query {:title "PL FBgg0000203: U-BOX UBIQUITIN LIGASES",
                   :from "Gene",
                   :select ["Gene.id"],
                   :where [{:path "Gene", :op "IN", :value "PL FBgg0000203: U-BOX UBIQUITIN LIGASES", :code "A"}],
                   :sortOrder ["Gene.symbol" "DESC"]
                   :constraintLogic "A"}}],
   :Organism [{:path "Gene.organism.id",
               :type :Organism,
               :query {:title "PL FBgg0000203: U-BOX UBIQUITIN LIGASES",
                       :from "Gene",
                       :select ["Gene.organism.id"],
                       :orderBy ["Gene.organism.name" "ASC"]
                       :where [{:path "Gene", :op "IN", :value "PL FBgg0000203: U-BOX UBIQUITIN LIGASES", :code "A"}],
                       :constraintLogic "A"}}]})

(def query-parts-unordered
  {:Gene [{:path "Gene.id",
           :type :Gene,
           :query {:title "PL FBgg0000203: U-BOX UBIQUITIN LIGASES",
                   :from "Gene",
                   :select ["Gene.id"],
                   :where [{:path "Gene", :op "IN", :value "PL FBgg0000203: U-BOX UBIQUITIN LIGASES", :code "A"}],
                   :constraintLogic "A"}}],
   :Organism [{:path "Gene.organism.id",
               :type :Organism,
               :query {:title "PL FBgg0000203: U-BOX UBIQUITIN LIGASES",
                       :from "Gene",
                       :select ["Gene.organism.id"],
                       :where [{:path "Gene", :op "IN", :value "PL FBgg0000203: U-BOX UBIQUITIN LIGASES", :code "A"}],
                       :constraintLogic "A"}}]})

(deftest unorder-query-parts-test
  (is (= (unorder-query-parts query-parts) query-parts-unordered)))
