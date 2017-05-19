
(ns redgenes.mines)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  SAMPLE MINE CONFIG WITH COMMENTS:  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sample-mine
  {:kittenmine
   {;;This is just a keyword to identify your mine. It could
    ;;be almost anything, but must be consistent with the map key
    :id                           :kittenmine
    ;;The web servce URL for your mine
    :service                      {:root "www.kittenmine.org/query" :token nil}
    ;; The name of your Mine. This will show in the navbar
    :name                         "KittenMine"
    ;; DEPRECATED - common name for your mine's organism. This may be
    ;; removed in the future b/c mines are multi-organism . (Homology)
    :common                       "Fly"
    ;; Probably deprecated as above
    :abbrev                       "D. melanogaster"
    ;; The name for an icon to represent your InterMine. Should be
    ;; defined as an SVG in src/cljs/redgenes/components/icons.cljs
    ;; It will appear in the top-left corner of the navbar.
    :icon                         "icon-kitten"
    ;; Mines have default "popular" object types that aren't available via the API
    ;; This is the workaround. When in doubt,[:Gene] is a good bet.
    ;; Also update selected object-type when you change the default types.
    :default-object-types         [:Gene :Protein]
    ;; This must be one of the types in default-object type, even if there's more than one. Initial / default value for the query
    :default-selected-object-type :Gene
    ;; Must be set to initialise organism dropdowns correctly
    :default-organism             "D. melanogaster"
    ;; What to populates the list upload / id resolver with when user clicks
    ;; [Show me an example]. Make sure IDs are consistent with the
    ;; selected-object-type and default organism.
    :idresolver-example           {:Gene    "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt"
                                   :Protein "Q8T3M3,FBpp0081318,FTZ_DROME"}
    ;;What to populate in regionsearch when user clicks [Show me an example].
    ;;This is optional if your mine doesn't have region search. If you don't include
    ;;it, the regionsearch tab won't show.
    :regionsearch-example         ["2L:14615455..14619002"
                                   "2R:5866646..5868384"
                                   "3R:2578486..2580016"]
   ;;this is for the "example query" button in the query builder. Create an example that will produce interesting
   ;;results but ideally not hundreds of rows - include a constraint in the example too.
    :default-query-example        {:from   "Gene"
                                   :select ["Gene.primaryIdentifier" "Gene.homologues.homologue.primaryIdentifier" "Gene.homologues.homologue.symbol" "Gene.symbol" "Gene.homologues.homologue.organism.name"]
                                   :where  [{:path  "Gene.symbol"
                                             :op    "="
                                             :code  "A"
                                             :value "GATA1"}]}}})

(def mines {:humanmine     {:id                 :humanmine
                            :service            {:root "www.humanmine.org/humanmine" :token nil}
                            :name               "HumanMine"
                            :common             "Human"
                            :icon               "icon-human"
                            :output?            true
                            :abbrev             "H. sapiens"
                            :default-organism   "H. sapiens"
                            :default-object-types   ["Gene" "Protein"]
                            :default-selected-object-type :Gene
                            :status             {:status :na}
                            :idresolver-example {:Gene "PPARG, FTO, 3949, LEP, 946, MC3R, 9607, LPL, LDLR, P55916, 335, GLUT4, Notch1, SLC27A1"
                                                 :Protein "P37231, PPARG_HUMAN"}
                            :regionsearch-example ["2:14615455..14619002"
                                                   "4:5866646..5868384"
                                                   "3:2578486..2580016"]
                            :default-query-example        {:from            "Gene"
                                                           :constraintLogic "A and B"
                                                           :select          ["primaryIdentifier"
                                                                             "symbol"
                                                                             "name"
                                                                             "goAnnotation.ontologyTerm.identifier"
                                                                             "goAnnotation.ontologyTerm.name"
                                                                             "organism.shortName"]
                                                           :where           [{:path  "goAnnotation.ontologyTerm.name"
                                                                              :op    "="
                                                                              :code  "A"
                                                                              :value "DNA binding"}
                                                                             {:path  "organism.shortName"
                                                                              :op    "="
                                                                              :code  "B"
                                                                              :value "H. sapiens"}]}
                            :mine
                            {:name    "HumanMine"
                             :url     "www.humanmine.org/humanmine"
                             :service {:root "www.humanmine.org/humanmine"}}}

            :flymine       {:id                 :flymine
                            :service            {:root "www.flymine.org/query" :token nil}
                            :name               "FlyMine"
                            :common             "Fly"
                            :icon               "icon-fly"
                            :status             {:status :na}
                            :output?            true
                            :abbrev             "D. melanogaster"
                            :default-object-types   ["Gene" "Protein"]
                            :default-selected-object-type :Gene
                            :idresolver-example {:Gene "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt"
                                                 :Protein "Q8T3M3,FBpp0081318,FTZ_DROME"}
                            :regionsearch-example ["2L:14615455..14619002"
                                                   "2R:5866646..5868384"
                                                   "3R:2578486..2580016"]
                            :default-query-example        {:from            "Gene"
                                                           :constraintLogic "A or B"
                                                           :select          ["symbol"
                                                                             "organism.name"
                                                                             "alleles.symbol"
                                                                             "alleles.phenotypeAnnotations.annotationType"
                                                                             "alleles.phenotypeAnnotations.description"]
                                                           :where           [{:path  "Gene.symbol"
                                                                              :op    "="
                                                                              :code  "A"
                                                                              :value "zen"}
                                                                             {:path  "Gene.symbol"
                                                                              :op    "="
                                                                              :code  "B"
                                                                              :value "mad"}]}
                            :mine
                            {:name    "FlyMine"
                             :url     "www.flymine.org/query"
                             :service {:root "www.flymine.org/query"}}}

            :humanmine-beta {:id                           :humanmine-beta
                             :service                      {:root "beta.humanmine.org/beta" :token nil}
                             :name                         "HumanMine Beta"
                             :common                       "Human"
                             :icon                         "icon-human"
                             :abbrev                       "H. sapiens"
                             :default-organism             "H. sapiens"
                             :default-object-types         [:Gene :Protein]
                             :default-selected-object-type :Gene
                             :idresolver-example           {:Gene    "PPARG, FTO, 3949, LEP, 946, MC3R, 9607, LPL, LDLR, P55916, 335, GLUT4, Notch1, SLC27A1"
                                                            :Protein "P37231, PPARG_HUMAN"}
                             :regionsearch-example         ["2:14615455..14619002"
                                                            "4:5866646..5868384"
                                                            "3:2578486..2580016"]
                             :default-query-example        {:from            "Gene"
                                                            :constraintLogic "A and B"
                                                            :select          ["primaryIdentifier"
                                                                              "symbol"
                                                                              "name"
                                                                              "goAnnotation.ontologyTerm.identifier"
                                                                              "goAnnotation.ontologyTerm.name"
                                                                              "organism.shortName"]
                                                            :where           [{:path  "goAnnotation.ontologyTerm.name"
                                                                               :op    "="
                                                                               :code  "A"
                                                                               :value "DNA binding"}
                                                                              {:path  "organism.shortName"
                                                                               :op    "="
                                                                               :code  "B"
                                                                               :value "H. sapiens"}]}
                             :mine
                             {:name    "HumanMine"
                              :url     "beta.humanmine.org/beta"
                              :service {:root "beta.humanmine.org/beta"}}}

            :flymine-beta   {:id                           :flymine-beta
                             :service                      {:root "beta.flymine.org/beta" :token nil}
                             :name                         "Flymine Beta"
                             :common                       "Fly"
                             :icon                         "icon-fly"
                             :status                       {:status :na}
                             :output?                      true
                             :abbrev                       "D. melanogaster"
                             :default-object-types         [:Gene :Protein]
                             :default-organism             "D. melanogaster"
                             :default-selected-object-type :Gene
                             :regionsearch-example         ["2L:14615455..14619002"
                                                            "2R:5866646..5868384"
                                                            "3R:2578486..2580016"]
                             :idresolver-example           {:Gene    "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt"
                                                            :Protein "Q8T3M3,FBpp0081318,FTZ_DROME"}
                             :default-query-example        {:from            "Gene"
                                                            :constraintLogic "A or B"
                                                            :select          ["symbol"
                                                                              "organism.name"
                                                                              "alleles.symbol"
                                                                              "alleles.phenotypeAnnotations.annotationType"
                                                                              "alleles.phenotypeAnnotations.description"]
                                                            :where           [{:path  "Gene.symbol"
                                                                               :op    "="
                                                                               :code  "A"
                                                                               :value "zen"}
                                                                              {:path  "Gene.symbol"
                                                                               :op    "="
                                                                               :code  "B"
                                                                               :value "mad"}]}
                             :mine
                             {:name    "FlyMine"
                                                            ;:url "beta.flymine.org/beta"
                              :url     "beta.flymine.org/beta"
                              :service {:root "beta.flymine.org/beta"}}}
            :beanmine       {:id                           :beanmine
                             :service                      {:root "https://mines.legumeinfo.org/beanmine" :token nil}
                             :name                         "BeanMine"
                             :common                       "Bean"
                             :icon                         "icon-intermine"
                             :status                       {:status :na}
                             :output?                      true
                             :abbrev                       "P. vulgaris"
                             :default-object-types         [:Gene] ;;there are other 'default' identifers but they have no examples so I'm not adding them right now.
                             :default-organism             "P. vulgaris"
                             :default-selected-object-type :Gene
                             :regionsearch-example         ["phavu.Chr01:29733..37349"
                                                            "phavu.Chr01:393758..394189"
                                                            "phavu.Chr07:1495567..1503324"]
                             :idresolver-example           {:Gene "Phvul.001G011500"}
                             :default-query-example        {:from   "Gene"
                                                            :select ["primaryIdentifier"
                                                                     "length"
                                                                     "goAnnotation.ontologyTerm.identifier"
                                                                     "goAnnotation.ontologyTerm.name"
                                                                     "goAnnotation.ontologyTerm.description"]
                                                            :where  [{:path  "Gene.length"
                                                                      :op    "<"
                                                                      :code  "A"
                                                                      :value "222"}]}
                             :mine
                             {:name    "BeanMine"
                              :url     "https://mines.legumeinfo.org/beanmine"
                              :service {:root "https://mines.legumeinfo.org/beanmine"}}}

            :legumemine     {:id                           :legumemine
                             :service                      {:root "https://intermine.legumefederation.org/legumemine" :token nil}
                             :name                         "LegumeMine"
                             :common                       "Legume"
                             :icon                         "icon-intermine"
                             :status                       {:status :na}
                             :output?                      true
                             :abbrev                       "G. max"
                             :default-object-types         [:Gene] ;;there are other 'default' identifers but they have no examples so I'm not adding them right now.
                             :default-organism             "G. max"
                             :default-selected-object-type :Gene
                             :regionsearch-example         ["phavu.Chr01:1000000..2000000"]
                             :idresolver-example           {:Gene "Glyma.16G153700"}
                             :default-query-example        {:from   "Gene"
                                                            :select ["primaryIdentifier"
                                                                     "length"
                                                                     "goAnnotation.ontologyTerm.identifier"
                                                                     "goAnnotation.ontologyTerm.name"
                                                                     "goAnnotation.ontologyTerm.description"]
                                                            :where  [{:path  "Gene.length"
                                                                      :op    "<"
                                                                      :code  "A"
                                                                      :value "222"}]}
                             :mine                         {:name    "LegumeMine"
                                                            :url     "https://intermine.legumefederation.org/legumemine"
                                                            :service {:root "https://intermine.legumefederation.org/legumemine"}}}

            :mousemine     {:id                 :mousemine
                            :service            {:root "www.mousemine.org/mousemine"}
                            :name               "MouseMine"
                            :common             "Mouse"
                            :output?            true
                            :icon               "icon-mouse"
                            :abbrev             "M. musculus"
                            :default-organism   "M. musculus"
                            :default-object-types   [:Gene :Protein :OntologyTerm :Publication :SequenceFeature]
                            :default-selected-object-type :Gene
                            :regionsearch-example ["2:10000000..15000000"
                                                   "chr6:10000000..20000000"
                                                   "X:53000000-54000000"]
                            :idresolver-example {:Gene "MGI:88388 MGI:96677 Fgf2 Bmp4"}
                            :default-query-example        {:from   "Gene"
                                                           :select ["Gene.primaryIdentifier" "Gene.homologues.homologue.primaryIdentifier" "Gene.homologues.homologue.symbol" "Gene.symbol" "Gene.homologues.homologue.organism.name"]
                                                           :where  [{:path  "Gene.symbol"
                                                                     :op    "="
                                                                     :code  "A"
                                                                     :value "GATA1"}]}

                            :mine
                            {:name    "MouseMine"
                             :url     "www.mousemine.org/mousemine"
                             :service {:root "www.mousemine.org/mousemine"}}}
            :bovinemine {:id             :bovinemine
                         :service        {:root "bovinegenome.org/bovinemine-dev/service" :token nil}
                         :name           "BovineMine"
                         :common         "Bovine"
                         :icon           "icon-intermine"
                         :status         {:status :na}
                         :output?        true
                         :abbrev         "B. taurus"
                         :default-object-types [:Gene]
                         :default-organism "B. taurus"
                         :default-selected-object-type :Gene
                         :regionsearch-example ["GK000001.2:7901376..7901377", "GK000003.2:80105316..80105317", "GK000003.2:88904960..88904961", "GK000004.2:7139260..7139261", "GK000004.2:75484332..75484333", "GK000005.2:47594268..47594269"]
                         :idresolver-example {:Gene "ABCG2, ACLY, ACTB, ATP2B2, B4GALT1, BoLA-DRB3, BTN1A1, CCL2, CSN1S2, CSN2, DGAT1, EGF, ETS2, FEZF2, ID2, KCNK1, MFGE8, NME1, LGB, PRL, PTGS1, PTHLH, RORA, STAT5A, TLR4, XDH, LALBA, LEP, TP53, CSN3, CSN1S1, LTF"}
                         :mine
                         {:name "BovineMine"
                          :url  "bovinegenome.org/bovinemine-dev"
                          :service {:root "bovinegenome.org/bovinemine-dev/service"}}
                         :default-query-example        {:from   "Gene"
                                                        :select ["Gene.primaryIdentifier" "Gene.homologues.homologue.primaryIdentifier" "Gene.homologues.homologue.symbol" "Gene.symbol" "Gene.homologues.homologue.organism.name"]
                                                        :where  [{:path  "Gene.symbol"
                                                                  :op    "="
                                                                  :code  "A"
                                                                  :value "GATA1"}]}}
            ; :url "beta.mousemine.org/mousemine"
            ; :service {:root "beta.mousemine.org/mousemine"}}}
            ; :ratmine       {:id                 :ratmine
            ;                 :service            {:root "stearman.hmgc.mcw.edu/ratmine"}
            ;                 :name               "RatMine"
            ;                 :common             "Rat"
            ;                 :output?            true
            ;                 :icon               "icon-rat"
            ;                 :abbrev             "R. norvegicus"
            ;                 :default-organism    "R. norvegicus"
            ;                 :default-object-types   [:Gene :Protein]
            ;                 :default-selected-object-type :Gene
            ;                 :status             {:status :na}
            ;                 :idresolver-example {:Gene "Exo1, LEPR, PW:0000564, 2004, RGD:3001, Hypertension"}
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
            ;                 :default-selected-object-type :Gene
            ;                 :default-object-types   [Gene ]
            ;                 :abbrev             "D. rerio"
            ;                 :default-organism   "D. rerio"
            ;                 :idresolver-example {:Gene "esr1, pparg, esr2a, esr2b, sdr42e1, star, ENSDARG00000063438, apoa1b, apoa1a, npc2, dhcr7, ZDB-GENE-061013-742, cyp11a2, s2p"}
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
            ;                 :default-object-types   [:Gene :Protein]
            ;                 :default-selected-object-type :Gene
            ;                 :status             {:status :na}
            ;                 :idresolver-example {:Gene "acr-10, unc-26, hlh-2, WBGene00002299, WBGene00004323, WBGene00002992"}
            ;                 :mine
            ;                                     {:name    "WormMine"
            ;                                      ;    :url "im-253.wormbase.org/tools/wormmine"
            ;                                      ;    :service {:root "im-253.wormbase.org/tools/wormmine"}}}
            ;                                      :url     "intermine.wormbase.org/tools/wormmine"
            ;                                      :service {:root "intermine.wormbase.org/tools/wormmine"}}}
            :yeastmine      {:id                           :yeastmine
                             :service                      {:root "yeastmine-test-aws.yeastgenome.org/yeastmine-dev/"}
                             :name                         "YeastMine"
                             :output?                      true
                             :common                       "Yeast"
                             :icon                         "icon-yeast"
                             :abbrev                       "S. cerevisiae"
                             :default-object-types         [:Gene]
                             :default-selected-object-type :Gene
                             :default-organism             "S. cerevisiae"
                             :status                       {:status :na}
                             :idresolver-example           {:Gene "rad51; rad52; rad53; ddc1; rad55; rad57; spo11; dmc1; rad17; rad9; rad24; msh1; msh5; mre11; xrs2; ndt80; tid1; ssb1; pre3; acr1; doa3; rad54; ssf1"}
                             :regionsearch-example         ["chrIII:1356..20455"
                                                            "chrIV:11331..18001"
                                                            "chrVI:9856..100010"]
                             :default-query-example        {:from   "Phenotype"
                                                            :select ["genes.primaryIdentifier"
                                                                     "genes.secondaryIdentifier"
                                                                     "genes.symbol"
                                                                     "genes.qualifier"
                                                                     "genes.sgdAlias"
                                                                     "experimentType"
                                                                     "mutantType"
                                                                     "observable"]
                                                            :where  [{:path  "observable"
                                                                      :op    "="
                                                                      :value "Protein secretion"
                                                                      :code "A"}]}
                             :mine                         {:name    "YeastMine"
                                                            :url     "yeastmine.yeastgenome.org/yeastmine"
                                                            :service {:root "yeastmine.yeastgenome.org/yeastmine"}}}

            ; :mitominer     {:id                 :mitominer
            ;                 :service            {:root "mitominer.mrc-mbu.cam.ac.uk/release-4.0"}
            ;                 :name               "MitoMiner"
            ;                 :output?            true
            ;                 :common             "MitoMiner"
            ;                 :icon               "icon-human"
            ;                 :abbrev             "H. sapiens" ;;HAVING A DEFAULT ORGANISM MAY BE A BAD IDEA!! ;; i know
            ;                 :default-organism   "H. sapiens"
            ;                 :default-selected-object-type :Gene
            ;                 :default-object-types   [:Gene :Protein]
            ;                 :status             {:status :na}
            ;                 :idresolver-example {:Gene "Atp5a1, Atp5b, Atp5d, Atp5c1"
            ;                                      :Protein "Q03265, P56480, Q9D3D9, Q91VR2"}
            ;                 :mine
            ;                                     {:name    "MitoMiner"
            ;                                      :url     "mitominer.mrc-mbu.cam.ac.uk/release-4.0"
            ;                                      :service {:root "mitominer.mrc-mbu.cam.ac.uk/release-4.0"}}}
})
