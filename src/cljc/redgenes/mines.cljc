(ns redgenes.mines)

;;; NOTE FOR PEOPLE EDITING THIS FILE: If you don't give a mine a regionsearch-example,
;;; We assume that there *is* no regionsearch in your mine, and there's no link it in the navbar.

(def mines  {
          ; :humanmine     {:id                 :humanmine
          ;                   :service            {:root "www.humanmine.org/humanmine" :token nil}
          ;                   :name               "HumanMine"
          ;                   :common             "Human"
          ;                   :icon               "icon-human"
          ;                   :output?            true
          ;                   :abbrev             "H. sapiens"
          ;                   :default-organism   "H. sapiens"
          ;                   :default-object-types   ["Gene" "Protein"]
          ;                   :status             {:status :na}
          ;                   :idresolver-example "PPARG, FTO, 3949, LEP, 946, MC3R, 9607, LPL, LDLR, P55916, 335, GLUT4, Notch1, SLC27A1"
          ;                   :regionsearch-example ["2:14615455..14619002"
          ;                                          "4:5866646..5868384"
          ;                                          "3:2578486..2580016"]
          ;                   :mine
          ;                                       {:name    "HumanMine"
          ;                                        :url     "www.humanmine.org/humanmine"
          ;                                        :service {:root "www.humanmine.org/humanmine"}}}
          ;   ;  :url "beta.humanmine.org/beta"
          ;   ;  :service {:root "beta.humanmine.org/beta"}}}
          ;   :flymine       {:id                 :flymine
          ;                   :service            {:root "www.flymine.org/query" :token nil}
          ;                   :name               "FlyMine"
          ;                   :common             "Fly"
          ;                   :icon               "icon-fly"
          ;                   :status             {:status :na}
          ;                   :output?            true
          ;                   :abbrev             "D. melanogaster"
          ;                   :default-object-types   ["Gene" "Protein"]
          ;                   :idresolver-example "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt"
          ;                   :regionsearch-example ["2L:14615455..14619002"
          ;                                           "2R:5866646..5868384"
          ;                                           "3R:2578486..2580016"]
          ;                   :mine
          ;                                       {:name    "FlyMine"
          ;                                        :url     "www.flymine.org/query"
          ;                                        :service {:root "www.flymine.org/query"}}}
          ;
          :humanmine-beta  {:id                 :humanmine-beta
                            :service            {:root "beta.humanmine.org/beta" :token nil}
                            :name               "HumanMine Beta"
                            :common             "Human"
                            :icon               "icon-human"
                            :output?            true
                            :abbrev             "H. sapiens"
                            :default-organism   "H. sapiens"
                            :default-object-types   ["Gene" "Protein"]
                            :status             {:status :na}
                            :idresolver-example "PPARG, FTO, 3949, LEP, 946, MC3R, 9607, LPL, LDLR, P55916, 335, GLUT4, Notch1, SLC27A1"
                            :regionsearch-example ["2:14615455..14619002"
                                                   "4:5866646..5868384"
                                                   "3:2578486..2580016"]
                            :mine
                                                {:name    "HumanMine"
                                                 :url     "beta.humanmine.org/beta"
                                                 :service {:root "beta.humanmine.org/beta"}}}

            :flymine-beta  {:id                 :flymine-beta
                            :service            {:root "beta.flymine.org/beta" :token nil}
                            :name               "Flymine Beta"
                            :common             "Fly"
                            :icon               "icon-fly"
                            :status             {:status :na}
                            :output?            true
                            :abbrev             "D. melanogaster"
                            :default-object-types   ["Gene" "Protein"]
                            :default-organism   "D. melanogaster"
                            :regionsearch-example ["2L:14615455..14619002"
                                                    "2R:5866646..5868384"
                                                    "3R:2578486..2580016"]
                            :idresolver-example "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt"
                            :mine
                                                {:name    "FlyMine"
                                                 ;:url "beta.flymine.org/beta"
                                                 :url     "beta.flymine.org/beta"
                                                 :service {:root "beta.flymine.org/beta"}}}
            ; :mousemine     {:id                 :mousemine
            ;                 :service            {:root "www.mousemine.org/mousemine"}
            ;                 :name               "MouseMine"
            ;                 :common             "Mouse"
            ;                 :output?            true
            ;                 :icon               "icon-mouse"
            ;                 :abbrev             "M. musculus"
            ;                 :default-organism   "M. musculus"
            ;            :default-object-types   ["Gene" "Protein" "Ontology Term" "Publication" "Sequence Feature"]
            ;                 :status             {:status :na}
            ;                 :regionsearch-example ["2:10000000..15000000"
            ;                                       "chr6:10000000..20000000"
            ;                                       "X:53000000-54000000"]
            ;                 :idresolver-example "MGI:88388 MGI:96677 Fgf2 Bmp4"
            ;                 :mine
            ;                                     {:name    "MouseMine"
            ;                                      :url     "www.mousemine.org/mousemine"
            ;                                      :service {:root "www.mousemine.org/mousemine"}}}
            ; ; :url "beta.mousemine.org/mousemine"
            ; ; :service {:root "beta.mousemine.org/mousemine"}}}
            ; :ratmine       {:id                 :ratmine
            ;                 :service            {:root "stearman.hmgc.mcw.edu/ratmine"}
            ;                 :name               "RatMine"
            ;                 :common             "Rat"
            ;                 :output?            true
            ;                 :icon               "icon-rat"
            ;                 :abbrev             "R. norvegicus"
            ;                 :default-organism    "R. norvegicus"
            ;            :default-object-types   ["Gene" "Protein"]
            ;                 :status             {:status :na}
            ;                 :idresolver-example "Exo1, LEPR, PW:0000564, 2004, RGD:3001, Hypertension"
            ;                 :mine
            ;                                     {:name    "RatMine"
            ;                                      ; :url "dev.ratmine.mcw.edu/ratmine"
            ;                                      ; :service {:root "dev.ratmine.mcw.edu/ratmine"}}}
            ;                                      :url     "ratmine.mcw.edu/ratmine"
            ;                                      :service {:root "stearman.hmgc.mcw.edu/ratmine"}}}
            ; :zebrafishmine {:id                 :zebrafishmine
            ;                 :service            {:root "www.zebrafishmine.org"}
            ;                 :name               "ZebrafishMine"
            ;                 :common             "Zebrafish"
            ;                 :icon               "icon-zebrafish"
            ;                 :output?            true
            ;            :default-object-types   ["Gene" "Protein"]
            ;                 :status             {:status :na}
            ;                 :abbrev             "D. rerio"
            ;                 :default-organism   "D. rerio"
            ;                 :idresolver-example "esr1, pparg, esr2a, esr2b, sdr42e1, star, ENSDARG00000063438, apoa1b, apoa1a, npc2, dhcr7, ZDB-GENE-061013-742, cyp11a2, s2p"
            ;                 :mine
            ;                                     {:name    "ZebraFishMine"
            ;                                      :url     "www.zebrafishmine.org"
            ;                                      :service {:root "www.zebrafishmine.org"}}}
            ; :wormmine      {:id                 :wormmine
            ;                 :service            {:root "intermine.wormbase.org/tools/wormmine"}
            ;                 :name               "WormMine"
            ;                 :common             "Worm"
            ;                 :output?            true
            ;                 :icon               "icon-worm"
            ;                 :abbrev             "C. elegans"
            ;                 :default-organism   "C. elegans"
            ;            :default-object-types   ["Gene" "Protein"]
            ;                 :status             {:status :na}
            ;                 :idresolver-example "acr-10, unc-26, hlh-2, WBGene00002299, WBGene00004323, WBGene00002992"
            ;                 :mine
            ;                                     {:name    "WormMine"
            ;                                      ;    :url "im-253.wormbase.org/tools/wormmine"
            ;                                      ;    :service {:root "im-253.wormbase.org/tools/wormmine"}}}
            ;                                      :url     "intermine.wormbase.org/tools/wormmine"
            ;                                      :service {:root "intermine.wormbase.org/tools/wormmine"}}}
            ; :yeastmine     {:id                 :yeastmine
            ;                 :service            {:root "yeastmine.yeastgenome.org/yeastmine"}
            ;                 :name               "YeastMine"
            ;                 :output?            true
            ;                 :common             "Yeast"
            ;                 :icon               "icon-yeast"
            ;                 :abbrev             "S. cerevisiae"
            ;            :default-object-types   ["Gene"]
            ;                 :default-organism   "S. cerevisiae"
            ;                 :status             {:status :na}
            ;                 :idresolver-example "rad51; rad52; rad53; ddc1; rad55; rad57; spo11; dmc1; rad17; rad9; rad24; msh1; msh5; mre11; xrs2; ndt80; tid1; ssb1; pre3; acr1; doa3; rad54; ssf1"
            ;                 :regionsearch-example ["chrIII:1356..20455"
            ;                                       "chrIV:11331..18001"
            ;                                       "chrVI:9856..100010"]
            ;                 :mine
            ;                                     {:name    "YeastMine"
            ;                                      :url     "yeastmine.yeastgenome.org/yeastmine"
            ;                                      :service {:root "yeastmine.yeastgenome.org/yeastmine"}}}
            ;
            ; :mitominer     {:id                 :mitominer
            ;                 :service            {:root "mitominer.mrc-mbu.cam.ac.uk/release-4.0"}
            ;                 :name               "MitoMiner"
            ;                 :output?            true
            ;                 :common             "MitoMiner"
            ;                 :icon               "icon-human"
            ;                 :abbrev             "H. sapiens" ;;HAVING A DEFAULT ORGANISM MAY BE A BAD IDEA!! ;; i know
            ;                 :default-organism   "H. sapiens"
            ;            :default-object-types   ["Gene" "Protein"]
            ;                 :status             {:status :na}
            ;                 :idresolver-example "Atp5a1, Atp5b, Atp5d, Atp5c1"
            ;                 :mine
            ;                                     {:name    "MitoMiner"
            ;                                      :url     "mitominer.mrc-mbu.cam.ac.uk/release-4.0"
            ;                                      :service {:root "mitominer.mrc-mbu.cam.ac.uk/release-4.0"}}}
            })
