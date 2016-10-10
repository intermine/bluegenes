(ns redgenes.sections.objects.components.homologues
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require      [imcljs.search :refer [raw-query-rows]]
                 [re-frame.core :refer [subscribe]]
                 [cljs.core.async :as a :refer [put! chan <! >! timeout close!]]))

(defn homologue-query [id organism]
  {
   :constraintLogic "A and B"
   :from            "Gene"
   :select          [
                     "homologues.homologue.id"
                     "homologues.homologue.primaryIdentifier"
                     "homologues.homologue.symbol"
                     "homologues.homologue.organism.shortName"
                     ]
   :where           [
                     {
                      :path  "primaryIdentifier"
                      :op    "="
                      :value id
                      :code  "A"
                      }
                     {
                      :path  "homologues.homologue.organism.shortName"
                      :op    "="
                      :value organism
                      :code  "B"
                      }
                     ]
   })

(defn local-homologue-query [ids type organism]
  {
   :from    type
   :select  [
             "primaryIdentifier"
             "symbol"
             "organism.shortName"
             ]
   :orderBy [
             {
              :path      "primaryIdentifier"
              :direction "ASC"
              }
             ]
   :where   [
             {
              :path       type
              :op         "LOOKUP"
              :value      ids
              :extraValue organism
              }
             ]
   })

(defn map-local-homologue-response [data]
  "formats the get-local-homologues response to match the default homologue response shape, so they can be output using the same logic."
  {:homologues (map (fn [homie] {:homologue homie}) data)})

(defn get-local-homologues [original-service remote-service q type organism]
  "If the remote mine says it has no homologues for a given identifier, query the local mine instead. It may be that there *are* homologues, but the remote mine doesn't know about them. If the local mine returns identifiers, verify them on the remote server and return them to the user."
  (let [c (chan)]
    ;(.log js/console "%c getting local homologues for %s" "border-bottom:wheat solid 3px" (:root (:service remote-service)))
    (go (let [
              ;;get the list of homologues from the local mine
              local-homologue-results (:homologues (first (<! (raw-query-rows {:root @(subscribe [:mine-url])}
              q
              {:format "json"}))))]
          (if (some? local-homologue-results)
            (do (let
                  ;;convert the results to just the list of homologues
                  [local-homologue-list     (map #(-> % :homologue :primaryIdentifier) local-homologue-results)
                   ;;build the query to send to the remote service
                   remote-homologue-query   (local-homologue-query local-homologue-list type organism)
                   ;;look up the list of identifers we just made on the remote mine to
                   ;;get the correct objectid to link to
                   remote-homologue-results (<! (raw-query-rows remote-service remote-homologue-query    {:root @(subscribe [:mine-url])}
))]
                  ;;put the results in the channel
                  (>! c (map-local-homologue-response remote-homologue-results))))
            (>! c {:homologues []})))) c))

            (defn get-primary-identifier
              "Returns the primary identifier associated with a given object id. Useful for cross-mine queries, as object ids aren't consistent between different mine instances."
              [type id service]
              (let [c (chan) q {
                                :from   type
                                :select "primaryIdentifier"
                                :where  [{
                                          :path  (str type ".id")
                                          :op    "="
                                          :value id}]}]
                (go (let [response (<! (raw-query-rows service q {:format "json"}))]
                      (.log js/console "%cresponse" "color:cornflowerblue;font-weight:bold;" (clj->js response))
                      (>! c (first (first (:results response))))))
                c))

(defn homologues
  "returns homologues of a given gene id from a remote mine."
  [original-service remote-service type id organism]
  (let [c (chan)]
    (.log js/console "%cremote-service" "color:hotpink;font-weight:bold;" (clj->js remote-service))
    (go (let [
              ;;get the primary identifier from the current mine
              primary-id (<! (get-primary-identifier type id original-service))
              ;build the query
              q          (homologue-query primary-id organism)
              ;;query the remote mine for homologues
              response   (<! (raw-query-rows remote-service q {:format "json"}))]
          (.log js/console "%c getting homologues for %s" "border-bottom:mediumorchid dotted 3px" (clj->js remote-service) (clj->js response))
          (if (> (count (:results response)) 0)
            (>! c response)
            (>! c (<! (get-local-homologues original-service remote-service q type organism)))
            ))) c))
